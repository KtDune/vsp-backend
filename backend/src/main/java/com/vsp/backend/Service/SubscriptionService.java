package com.vsp.backend.Service;

import com.vsp.backend.Dto.SubscriptionResponse;
import com.vsp.backend.Model.Subscription;
import com.vsp.backend.Model.User;
import com.vsp.backend.Repository.SubscriptionRepository;
import com.vsp.backend.Repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;

	public SubscriptionService(UserRepository userRepository, SubscriptionRepository subscriptionRepository) {
		this.userRepository = userRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Transactional
	public Map<String, Object> subscribe(Long subscriberId, Long channelId) {
		if (subscriberId == null || channelId == null) {
			return Map.of("success", false, "message", "Subscriber and channel are required");
		}
		if (subscriberId.equals(channelId)) {
			return Map.of("success", false, "message", "You cannot subscribe to yourself");
		}
		if (userRepository.findByIdAndIsActiveTrue(subscriberId).isEmpty()) {
			return Map.of("success", false, "message", "Subscriber not found or deactivated");
		}
		Optional<User> channelResult = userRepository.findByIdAndIsActiveTrue(channelId);
		if (channelResult.isEmpty()) {
			return Map.of("success", false, "message", "Channel not found or deactivated");
		}

		Subscription.Status initialStatus = channelResult.get().getVisibility() == User.Visibility.PRIVATE
				? Subscription.Status.PENDING
				: Subscription.Status.APPROVED;

		Optional<Subscription> existingResult = subscriptionRepository
				.findBySubscriberIdAndChannelIdAndIsActiveTrue(subscriberId, channelId);

		if (existingResult.isPresent()) {
			Subscription existing = existingResult.get();
			if (existing.getStatus() == Subscription.Status.REJECTED) {
				existing.setStatus(Subscription.Status.PENDING);
				existing.setDecidedAt(LocalDateTime.now());
				subscriptionRepository.save(existing);
				return Map.of("success", true, "message", "Subscription requested", "id", existing.getId());
			}
			return Map.of("success", true, "message",
					existing.getStatus() == Subscription.Status.APPROVED ? "Already subscribed" : "Request already pending",
					"id", existing.getId());
		}

		Subscription subscription = new Subscription();
		subscription.setSubscriberId(subscriberId);
		subscription.setChannelId(channelId);
		subscription.setStatus(initialStatus);
		subscription.setDecidedAt(LocalDateTime.now());
		subscription.setIsActive(true);

		Subscription saved = subscriptionRepository.save(subscription);
		return Map.of("success", true, "message",
				initialStatus == Subscription.Status.APPROVED ? "Subscribed" : "Subscription requested",
				"id", saved.getId());
	}

	@Transactional
	public boolean unsubscribe(Long subscriberId, Long channelId) {
		if (subscriberId == null || channelId == null) {
			return false;
		}

		Optional<Subscription> result = subscriptionRepository
				.findBySubscriberIdAndChannelIdAndIsActiveTrue(subscriberId, channelId);

		if (result.isEmpty()) {
			return false;
		}

		subscriptionRepository.delete(result.get());
		return true;
	}

	@Transactional
	public Map<String, Object> approve(Long subscriptionId, Long requesterId) {
		Optional<Subscription> result = subscriptionRepository.findByIdAndIsActiveTrue(subscriptionId);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Subscription not found");
		}

		Subscription subscription = result.get();

		if (requesterId == null || !requesterId.equals(subscription.getChannelId())) {
			return Map.of("success", false, "message", "Owner only");
		}
		if (subscription.getStatus() != Subscription.Status.PENDING) {
			return Map.of("success", false, "message", "Request is not pending");
		}

		subscription.setStatus(Subscription.Status.APPROVED);
		subscription.setDecidedAt(LocalDateTime.now());
		subscriptionRepository.save(subscription);
		return Map.of("success", true, "message", "Subscription approved", "id", subscription.getId());
	}

	@Transactional
	public Map<String, Object> reject(Long subscriptionId, Long requesterId) {
		Optional<Subscription> result = subscriptionRepository.findByIdAndIsActiveTrue(subscriptionId);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Subscription not found");
		}

		Subscription subscription = result.get();

		if (requesterId == null || !requesterId.equals(subscription.getChannelId())) {
			return Map.of("success", false, "message", "Owner only");
		}
		if (subscription.getStatus() != Subscription.Status.PENDING) {
			return Map.of("success", false, "message", "Request is not pending");
		}

		subscription.setStatus(Subscription.Status.REJECTED);
		subscription.setDecidedAt(LocalDateTime.now());
		subscriptionRepository.save(subscription);
		return Map.of("success", true, "message", "Subscription rejected", "id", subscription.getId());
	}

	@Transactional
	public Map<String, Object> removeSubscriber(Long subscriptionId, Long requesterId) {
		Optional<Subscription> result = subscriptionRepository.findByIdAndIsActiveTrue(subscriptionId);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Subscription not found");
		}

		Subscription subscription = result.get();

		if (requesterId == null || !requesterId.equals(subscription.getChannelId())) {
			return Map.of("success", false, "message", "Owner only");
		}
		if (subscription.getStatus() != Subscription.Status.APPROVED) {
			return Map.of("success", false, "message", "Subscription is not approved");
		}

		subscriptionRepository.delete(subscription);
		return Map.of("success", true, "message", "Subscriber removed");
	}

	@Transactional
	public int approveAll(Long channelId) {
		return subscriptionRepository.approveAllPendingForChannel(channelId, LocalDateTime.now());
	}

	public List<SubscriptionResponse> getChannelSubscriptionRequests(Long channelId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Slice<Subscription> requests = subscriptionRepository.findByChannelIdAndStatusAndIsActiveTrue(channelId,
				Subscription.Status.PENDING, pageable);

		Set<Long> subscriberIds = new HashSet<>();
		for (Subscription request : requests) {
			subscriberIds.add(request.getSubscriberId());
		}

		Map<Long, User> subscribersById = new HashMap<>();
		for (User subscriber : userRepository.findAllById(subscriberIds)) {
			if (Boolean.TRUE.equals(subscriber.getIsActive())) {
				subscribersById.put(subscriber.getId(), subscriber);
			}
		}

		List<SubscriptionResponse> views = new ArrayList<>();
		for (Subscription request : requests) {
			views.add(SubscriptionResponse.subscriberView(request, subscribersById.get(request.getSubscriberId())));
		}
		return views;
	}

	public Slice<Subscription> getChannelSubscribers(Long channelId, Long viewerId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "decidedAt"));

		Optional<User> channel = userRepository.findByIdAndIsActiveTrue(channelId);
		if (channel.isEmpty()) {
			return null;
		}

		boolean isOwner = viewerId != null && viewerId.equals(channelId);
		if (!isOwner && channel.get().getVisibility() == User.Visibility.PRIVATE) {
			return new SliceImpl<>(List.of(), pageable, false);
		}

		return subscriptionRepository.findByChannelIdAndStatusAndIsActiveTrue(channelId, Subscription.Status.APPROVED, pageable);
	}

	public Map<String, Object> getMySubscriptions(Long userId, int page, int size) {
		if (userRepository.findByIdAndIsActiveTrue(userId).isEmpty()) {
			return Map.of("success", false, "message", "User not found or deactivated");
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Slice<Subscription> subscriptions = subscriptionRepository
				.findBySubscriberIdAndStatusAndIsActiveTrue(userId, Subscription.Status.APPROVED, pageable);
		Slice<Subscription> pendingRequests = subscriptionRepository
				.findBySubscriberIdAndStatusAndIsActiveTrue(userId, Subscription.Status.PENDING, pageable);

		Set<Long> channelIds = new HashSet<>();
		for (Subscription subscription : subscriptions) {
			channelIds.add(subscription.getChannelId());
		}
		for (Subscription subscription : pendingRequests) {
			channelIds.add(subscription.getChannelId());
		}

		Map<Long, User> channelsById = new HashMap<>();
		for (User channel : userRepository.findAllById(channelIds)) {
			if (Boolean.TRUE.equals(channel.getIsActive())) {
				channelsById.put(channel.getId(), channel);
			}
		}

		List<SubscriptionResponse> subscriptionViews = new ArrayList<>();
		for (Subscription subscription : subscriptions) {
			subscriptionViews.add(
					SubscriptionResponse.channelView(subscription, channelsById.get(subscription.getChannelId())));
		}
		List<SubscriptionResponse> pendingViews = new ArrayList<>();
		for (Subscription subscription : pendingRequests) {
			pendingViews.add(
					SubscriptionResponse.channelView(subscription, channelsById.get(subscription.getChannelId())));
		}

		return Map.of("success", true, "subscriptions", subscriptionViews, "pendingRequests", pendingViews);
	}

	public Map<String, Object> getMySubscriptionStatus(Long channelId, Long viewerId) {
		if (userRepository.findByIdAndIsActiveTrue(channelId).isEmpty()) {
			return Map.of("success", false, "message", "User not found or deactivated");
		}

		String status = "NONE";
		if (viewerId != null && !viewerId.equals(channelId)) {
			status = subscriptionRepository
					.findBySubscriberIdAndChannelIdAndIsActiveTrue(viewerId, channelId)
					.map(s -> s.getStatus() != null ? s.getStatus().name() : "NONE")
					.orElse("NONE");
		}

		return Map.of("success", true, "status", status);
	}

	public Map<String, Object> getSubscriberCount(Long channelId, Long viewerId) {
		Optional<User> channel = userRepository.findByIdAndIsActiveTrue(channelId);
		if (channel.isEmpty()) {
			return Map.of("success", false, "message", "User not found or deactivated");
		}

		boolean isOwner = viewerId != null && viewerId.equals(channelId);
		if (!isOwner && channel.get().getVisibility() == User.Visibility.PRIVATE) {
			return Map.of("success", false, "message", "Private account");
		}

		long count = subscriptionRepository.countByChannelIdAndStatusAndIsActiveTrue(channelId,
				Subscription.Status.APPROVED);
		return Map.of("success", true, "subscriberCount", count);
	}
}
