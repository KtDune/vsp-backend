package com.vsp.backend.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vsp.backend.Config.MinioConfig;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@Service
public class StorageService {

	public enum MediaKind {
		VIDEO("video"),
		IMAGE("image"),
		AVATAR("avatar");

		private final String dir;

		MediaKind(String dir) {
			this.dir = dir;
		}

		private String dir() {
			return dir;
		}
	}

	private static final long MAX_VIDEO_BYTES = 2L * 1024 * 1024 * 1024;
	private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
	private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

	private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "webm");
	private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

	private static final Set<String> VIDEO_MIME_TYPES = Set.of("video/mp4", "video/quicktime", "video/webm");
	private static final Set<String> IMAGE_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

	private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
			"video/mp4", "mp4",
			"video/quicktime", "mov",
			"video/webm", "webm",
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp",
			"image/gif", "gif");

	private static final Pattern OBJECT_KEY_PATTERN = Pattern
			.compile("[A-Za-z0-9-]+\\.(?:mp4|mov|webm|jpg|jpeg|png|webp|gif)");
	private static final Pattern TRAILING_EXTENSION_PATTERN = Pattern.compile("\\.([A-Za-z0-9]+)$");

	private static final int SAMPLE_BYTES = 4096;

	private final MinioClient minioClient;
	private final MinioConfig.MinioProperties properties;
	private final Tika tika = new Tika();

	public StorageService(MinioClient minioClient, MinioConfig.MinioProperties properties) {
		this.minioClient = minioClient;
		this.properties = properties;
	}

	public Map<String, Object> upload(MultipartFile file, MediaKind kind, Long userId) {
		if (userId == null) {
			return failure("Authentication required");
		}
		if (file == null || file.isEmpty()) {
			return failure("File is empty");
		}
		if (file.getSize() > maxSize(kind)) {
			return failure(sizeMessage(kind));
		}

		if (!hasAllowedExtension(file.getOriginalFilename(), extensions(kind))) {
			return failure(fileTypeMessage(kind));
		}

		String contentType = cleanContentType(file.getContentType());
		if (contentType != null && !mimetypes(kind).contains(contentType)
				&& !"application/octet-stream".equals(contentType)) {
			return failure(fileTypeMessage(kind));
		}

		String detected = detectMagicBytes(file);
		if (detected == null || !mimetypes(kind).contains(detected)) {
			return failure(fileTypeMessage(kind));
		}

		String key = kind.dir() + "/" + userId + "/" + UUID.randomUUID() + "."
				+ MIME_TO_EXTENSION.get(detected);
		try (InputStream in = file.getInputStream()) {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(properties.bucket())
					.object(key)
					.stream(in, file.getSize(), -1)
					.contentType(detected)
					.build());
		} catch (Exception e) {
			return failure("Upload failed, please try again");
		}
		return Map.of("success", true, "url", publicUrl() + "/" + key);
	}

	public void deleteByUrl(String url) {
		String key = extractObjectKey(url);
		if (key == null) {
			return;
		}
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(properties.bucket())
					.object(key)
					.build());
		} catch (Exception ignored) {
		}
	}

	public boolean isOwnedMediaUrl(String url, MediaKind kind, Long userId) {
		if (userId == null || url == null || url.length() > 255) {
			return false;
		}
		String prefix = publicUrl() + "/" + kind.dir() + "/" + userId + "/";
		if (!url.startsWith(prefix)) {
			return false;
		}
		String remainder = url.substring(prefix.length());
		return OBJECT_KEY_PATTERN.matcher(remainder).matches();
	}

	private String detectMagicBytes(MultipartFile file) {
		byte[] sample = new byte[SAMPLE_BYTES];
		int read = 0;
		try (InputStream in = file.getInputStream()) {
			while (read < sample.length) {
				int chunk = in.read(sample, read, sample.length - read);
				if (chunk < 0) {
					break;
				}
				read += chunk;
			}
		} catch (IOException e) {
			return null;
		}
		if (read < 8) {
			return null;
		}
		return tika.detect(read == sample.length ? sample : Arrays.copyOf(sample, read));
	}

	private boolean hasAllowedExtension(String filename, Set<String> allowed) {
		if (filename == null || filename.isBlank() || filename.length() > 255) {
			return false;
		}
		if (filename.contains("/") || filename.contains("\\") || filename.contains("\0")) {
			return false;
		}
		var matcher = TRAILING_EXTENSION_PATTERN.matcher(filename);
		if (!matcher.find()) {
			return false;
		}
		String extension = matcher.group(1).toLowerCase(Locale.ROOT);
		return allowed.contains(extension);
	}

	private String cleanContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return null;
		}
		int semicolon = contentType.indexOf(';');
		String type = semicolon < 0 ? contentType : contentType.substring(0, semicolon);
		return type.trim().toLowerCase(Locale.ROOT);
	}

	private String publicUrl() {
		String url = properties.publicUrl();
		if (url == null || url.isBlank()) {
			url = properties.endpoint() + "/" + properties.bucket();
		}
		while (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	private String extractObjectKey(String url) {
		if (url == null) {
			return null;
		}
		String prefix = publicUrl() + "/";
		if (!url.startsWith(prefix)) {
			return null;
		}
		String key = url.substring(prefix.length());
		if (key.isEmpty() || key.contains("..") || key.contains("%")) {
			return null;
		}
		return key;
	}

	private static Set<String> extensions(MediaKind kind) {
		return kind == MediaKind.VIDEO ? VIDEO_EXTENSIONS : IMAGE_EXTENSIONS;
	}

	private static Set<String> mimetypes(MediaKind kind) {
		return kind == MediaKind.VIDEO ? VIDEO_MIME_TYPES : IMAGE_MIME_TYPES;
	}

	private static long maxSize(MediaKind kind) {
		return switch (kind) {
			case VIDEO -> MAX_VIDEO_BYTES;
			case AVATAR -> MAX_AVATAR_BYTES;
			default -> MAX_IMAGE_BYTES;
		};
	}

	private static String sizeMessage(MediaKind kind) {
		return switch (kind) {
			case VIDEO -> "Video must be 2 GB or smaller";
			case AVATAR -> "Avatar must be 2 MB or smaller";
			default -> "Image must be 5 MB or smaller";
		};
	}

	private static String fileTypeMessage(MediaKind kind) {
		return switch (kind) {
			case VIDEO -> "Video must be a real MP4, MOV or WebM file";
			default -> "Image must be a real JPG, PNG, WebP or GIF file";
		};
	}

	private static Map<String, Object> failure(String message) {
		return Map.of("success", false, "message", Objects.requireNonNull(message));
	}
}
