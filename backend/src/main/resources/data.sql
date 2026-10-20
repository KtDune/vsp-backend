DROP DATABASE IF EXISTS vsp;
CREATE DATABASE IF NOT EXISTS vsp;
USE vsp;

CREATE TABLE users (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	username VARCHAR(50) UNIQUE NOT NULL,
	email VARCHAR(255) UNIQUE NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	profile_url VARCHAR(255),
	visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PUBLIC',
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE videos (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	title VARCHAR(100) NOT NULL,
	description TEXT,
	visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PUBLIC',
	thumbnail_url VARCHAR(255),
	video_url VARCHAR(255) NOT NULL,
	duration INT UNSIGNED,
	view_count INT NOT NULL DEFAULT 0,
	comments_disabled BOOLEAN NOT NULL DEFAULT FALSE,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,

	CONSTRAINT fk_video_user
	FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE posts (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	content VARCHAR(1000),
	visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PUBLIC',
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	is_edited BOOLEAN NOT NULL DEFAULT FALSE,

	CONSTRAINT fk_post_user
	FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TRIGGER posts_set_edited
BEFORE UPDATE ON posts
FOR EACH ROW
	SET NEW.is_edited = NEW.is_edited OR NOT (NEW.content <=> OLD.content);

CREATE TABLE post_images (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	post_id BIGINT NOT NULL,
	user_id BIGINT NOT NULL,
	image_url VARCHAR(255) NOT NULL,
	sort_order INT UNSIGNED NOT NULL DEFAULT 0,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,

	CONSTRAINT fk_image_post
	FOREIGN KEY (post_id) REFERENCES posts(id)
		ON DELETE CASCADE,
	CONSTRAINT fk_image_user
	FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE comments (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	video_id BIGINT,
	post_id BIGINT,
	parent_id BIGINT,
	content VARCHAR(500) NOT NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	is_edited BOOLEAN NOT NULL DEFAULT FALSE,

	CHECK ((video_id IS NOT NULL) <> (post_id IS NOT NULL)),
	CONSTRAINT fk_comment_user
	FOREIGN KEY (user_id) REFERENCES users(id),
	CONSTRAINT fk_comment_video
	FOREIGN KEY (video_id) REFERENCES videos(id)
		ON DELETE CASCADE,
	CONSTRAINT fk_comment_post
	FOREIGN KEY (post_id) REFERENCES posts(id)
		ON DELETE CASCADE,
	CONSTRAINT fk_comment_parent
	FOREIGN KEY (parent_id) REFERENCES comments(id)
		ON DELETE CASCADE
);

CREATE TRIGGER comments_set_edited
BEFORE UPDATE ON comments
FOR EACH ROW
	SET NEW.is_edited = NEW.is_edited OR NOT (NEW.content <=> OLD.content);

CREATE TABLE likes (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	target_type ENUM('VIDEO', 'POST', 'COMMENT') NOT NULL,
	target_id BIGINT NOT NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,

	UNIQUE KEY uk_like (user_id, target_type, target_id),
	INDEX idx_likes_target (target_type, target_id),
	CONSTRAINT fk_like_user
	FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE subscriptions (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	subscriber_id BIGINT NOT NULL,
	channel_id BIGINT NOT NULL,
	status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,

	CHECK (subscriber_id <> channel_id),
	UNIQUE KEY uk_subscription (subscriber_id, channel_id),
	CONSTRAINT fk_sub_subscriber
	FOREIGN KEY (subscriber_id) REFERENCES users(id),
	CONSTRAINT fk_sub_channel
	FOREIGN KEY (channel_id) REFERENCES users(id),
	INDEX idx_sub_incoming (channel_id, status),
	INDEX idx_sub_mine (subscriber_id, status)
);

INSERT INTO users (username, email, password_hash, profile_url, visibility, created_at) VALUES
('alice', 'alice@vsp.example', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'https://picsum.photos/seed/vsp-avatar-alice/200/200', 'PRIVATE', '2026-05-04 10:15:00'),
('bob', 'bob@vsp.example', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'https://picsum.photos/seed/vsp-avatar-bob/200/200', 'PUBLIC', '2026-05-10 16:40:00'),
('carol', 'carol@vsp.example', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'https://picsum.photos/seed/vsp-avatar-carol/200/200', 'PUBLIC', '2026-06-01 09:05:00');

INSERT INTO videos (user_id, title, description, visibility, thumbnail_url, video_url, duration, view_count, comments_disabled, created_at, is_active) VALUES
(1, 'Getting Started with Spring Boot 3', 'A practical walkthrough of setting up your first Spring Boot project.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-1/640/360', 'https://cdn.vsp.example/videos/1.mp4', 754, 128400, FALSE, '2026-10-15 14:20:00', TRUE),
(1, 'My Desk Setup Tour 2026', 'Everything on my desk and why it is there.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-2/640/360', 'https://cdn.vsp.example/videos/2.mp4', 312, 54300, FALSE, '2026-10-13 09:45:00', TRUE),
(1, '10 Minute Morning Stretch Routine', 'A quick routine to wake up your body.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-3/640/360', 'https://cdn.vsp.example/videos/3.mp4', 605, 21000, FALSE, '2026-10-11 06:30:00', TRUE),
(1, 'Building a REST API from Scratch', 'Designing and implementing a clean REST API step by step.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-4/640/360', 'https://cdn.vsp.example/videos/4.mp4', 1840, 98700, FALSE, '2026-10-09 18:05:00', TRUE),
(1, 'Street Food Tour - Night Markets', 'Tasting my way through the night market.', 'PRIVATE', 'https://picsum.photos/seed/vsp-video-5/640/360', 'https://cdn.vsp.example/videos/5.mp4', 921, 33200, FALSE, '2026-10-07 20:15:00', TRUE),
(1, 'Lo-fi Beats to Code To', 'One hour of chill beats for focus sessions.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-6/640/360', 'https://cdn.vsp.example/videos/6.mp4', 2400, 402100, FALSE, '2026-10-05 12:00:00', TRUE),
(1, 'Understanding Database Indexes', 'How indexes work and when they hurt. Questions go in the community tab.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-7/640/360', 'https://cdn.vsp.example/videos/7.mp4', 1500, 76200, TRUE, '2026-10-03 11:40:00', TRUE),
(1, 'Week in the Life of a Software Engineer', 'Standups, sprints, and coffee.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-8/640/360', 'https://cdn.vsp.example/videos/8.mp4', 1080, 15400, FALSE, '2026-10-01 17:55:00', TRUE),
(1, 'Homemade Pizza Dough Tutorial', 'Two-day cold ferment, restaurant quality crust.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-9/640/360', 'https://cdn.vsp.example/videos/9.mp4', 842, 88900, FALSE, '2026-09-29 15:10:00', TRUE),
(1, 'Hiking the Dolomites - 4K Timelapse', 'Five days on the Alta Via 1.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-10/640/360', 'https://cdn.vsp.example/videos/10.mp4', 3600, 245000, FALSE, '2026-09-27 08:25:00', TRUE),
(1, 'Java Records Explained in 5 Minutes', 'Boilerplate-free data classes.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-11/640/360', 'https://cdn.vsp.example/videos/11.mp4', 302, 45200, FALSE, '2026-09-25 13:15:00', TRUE),
(1, 'Apartment Renovation Before and After', 'Six months of work in ten minutes.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-12/640/360', 'https://cdn.vsp.example/videos/12.mp4', 1450, 18700, FALSE, '2026-09-23 19:35:00', TRUE),
(1, 'Git Rebase vs Merge: When to Use Which', 'Untangling the most common Git debate.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-13/640/360', 'https://cdn.vsp.example/videos/13.mp4', 780, 65300, FALSE, '2026-09-21 10:50:00', TRUE),
(1, 'I Tried Waking Up at 5AM for 30 Days', 'Results were mixed. Here is what happened.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-14/640/360', 'https://cdn.vsp.example/videos/14.mp4', 1620, 310500, FALSE, '2026-09-19 07:00:00', TRUE),
(1, 'Intro to Docker for Beginners', 'Containers, images, and volumes demystified.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-15/640/360', 'https://cdn.vsp.example/videos/15.mp4', 2405, 120400, FALSE, '2026-09-17 16:45:00', TRUE),
(1, 'Watercolor Painting Basics', 'Supplies, washes, and your first painting.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-16/640/360', 'https://cdn.vsp.example/videos/16.mp4', 950, 9200, FALSE, '2026-09-15 14:30:00', TRUE),
(1, 'SQL Window Functions Deep Dive', 'ROW_NUMBER, LAG, and friends with real examples.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-17/640/360', 'https://cdn.vsp.example/videos/17.mp4', 2760, 51000, FALSE, '2026-09-13 11:20:00', TRUE),
(2, 'Camera Review - One Year Later', 'Long-term thoughts after 200 shoots.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-18/640/360', 'https://cdn.vsp.example/videos/18.mp4', 415, 23800, FALSE, '2026-09-11 12:40:00', TRUE),
(2, 'Meal Prep Sunday: 5 Easy Lunches', 'Five lunches, one hour, minimal dishes.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-19/640/360', 'https://cdn.vsp.example/videos/19.mp4', 1250, 44700, FALSE, '2026-09-09 10:05:00', TRUE),
(2, 'Tokyo Travel Vlog - Day 1', 'Landing, ramen, and getting lost on purpose.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-20/640/360', 'https://cdn.vsp.example/videos/20.mp4', 1120, 61200, FALSE, '2026-09-07 21:15:00', TRUE),
(2, 'Refactoring Legacy Code Without Fear', 'Strangler fig pattern in practice.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-21/640/360', 'https://cdn.vsp.example/videos/21.mp4', 3420, 39800, FALSE, '2026-09-05 15:30:00', TRUE),
(2, 'Winter Bike Commute Setup', 'Gear and lights that survive the dark months.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-22/640/360', 'https://cdn.vsp.example/videos/22.mp4', 860, 7600, FALSE, '2026-09-03 08:15:00', TRUE),
(2, 'Office Hours Q&A', 'Monthly Q&A for channel members.', 'PRIVATE', 'https://picsum.photos/seed/vsp-video-23/640/360', 'https://cdn.vsp.example/videos/23.mp4', 4500, 1100, FALSE, '2026-09-01 18:50:00', TRUE),
(2, 'Composition Rules I Break on Purpose', 'Rule of thirds is a suggestion.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-24/640/360', 'https://cdn.vsp.example/videos/24.mp4', 720, 27300, FALSE, '2026-08-30 13:25:00', TRUE),
(2, 'Kubernetes Basics in 20 Minutes', 'Pods, services, and deployments explained.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-25/640/360', 'https://cdn.vsp.example/videos/25.mp4', 1980, 158200, FALSE, '2026-08-28 09:35:00', TRUE),
(2, 'Sourdough Starter from Scratch', 'Day by day until it doubles.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-26/640/360', 'https://cdn.vsp.example/videos/26.mp4', 1105, 35900, FALSE, '2026-08-26 07:45:00', TRUE),
(2, 'Isle of Skye Road Trip', 'Wind, rain, and the best light of my life.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-27/640/360', 'https://cdn.vsp.example/videos/27.mp4', 2890, 48600, FALSE, '2026-08-24 20:00:00', TRUE),
(2, 'Unit Testing Best Practices', 'What to test, what to skip, and why.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-28/640/360', 'https://cdn.vsp.example/videos/28.mp4', 1540, 72100, FALSE, '2026-08-22 14:55:00', TRUE),
(2, 'Minimalist Wardrobe Tour', 'Thirty pieces, zero decision fatigue.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-29/640/360', 'https://cdn.vsp.example/videos/29.mp4', 630, 13400, FALSE, '2026-08-20 11:10:00', TRUE),
(2, 'Live Coding: Building a Chat App', 'Recorded live, unedited.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-30/640/360', 'https://cdn.vsp.example/videos/30.mp4', 5400, 9900, TRUE, '2026-08-18 19:40:00', TRUE),
(2, 'Coffee Brewing Methods Compared', 'V60 vs Aeropress vs French press, blind tasted.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-31/640/360', 'https://cdn.vsp.example/videos/31.mp4', 745, 52800, FALSE, '2026-08-16 09:20:00', TRUE),
(2, 'System Design: URL Shortener', 'Classic interview question, production answers.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-32/640/360', 'https://cdn.vsp.example/videos/32.mp4', 2230, 187400, FALSE, '2026-08-14 16:05:00', TRUE),
(2, 'Learning Japanese - 6 Month Update', 'What worked, what did not.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-33/640/360', 'https://cdn.vsp.example/videos/33.mp4', 1305, 21600, FALSE, '2026-08-12 08:55:00', TRUE),
(2, 'First Visit to the Climbing Gym', 'Fell off the wall, signed up anyway.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-34/640/360', 'https://cdn.vsp.example/videos/34.mp4', 620, 8700, FALSE, '2026-08-10 17:30:00', TRUE),
(3, 'Clean Architecture - Does It Scale?', 'Honest lessons from a three-year-old codebase.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-35/640/360', 'https://cdn.vsp.example/videos/35.mp4', 890, 64500, FALSE, '2026-08-08 10:40:00', TRUE),
(3, 'Vegetable Garden Tour - Summer', 'Tomatoes are winning this year.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-36/640/360', 'https://cdn.vsp.example/videos/36.mp4', 1670, 15900, FALSE, '2026-08-06 07:50:00', TRUE),
(3, 'Vintage Synth Sound Test', 'No talking, just patches.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-37/640/360', 'https://cdn.vsp.example/videos/37.mp4', 1122, 23700, FALSE, '2026-08-04 15:25:00', TRUE),
(3, 'Message Queues Explained Simply', 'Why your orders should not ride the same bus as your emails.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-38/640/360', 'https://cdn.vsp.example/videos/38.mp4', 1355, 91300, FALSE, '2026-08-02 12:35:00', TRUE),
(3, 'Pottery Wheel for Beginners', 'Centering is harder than it looks.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-39/640/360', 'https://cdn.vsp.example/videos/39.mp4', 760, 4200, FALSE, '2026-07-31 18:20:00', TRUE),
(3, 'Northern Lights Timelapse', 'Seven nights of waiting for the sky.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-40/640/360', 'https://cdn.vsp.example/videos/40.mp4', 4200, 312000, FALSE, '2026-07-29 22:45:00', TRUE),
(3, 'Members Only AMA', 'Answers to member questions.', 'PRIVATE', 'https://picsum.photos/seed/vsp-video-41/640/360', 'https://cdn.vsp.example/videos/41.mp4', 1880, 850, FALSE, '2026-07-27 11:30:00', TRUE),
(3, 'API Rate Limiting Strategies', 'Token bucket, sliding window, and when to use each.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-42/640/360', 'https://cdn.vsp.example/videos/42.mp4', 2450, 54300, FALSE, '2026-07-25 09:15:00', TRUE),
(3, 'Salsa Dancing Beginner Class', 'Two left feet welcome.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-43/640/360', 'https://cdn.vsp.example/videos/43.mp4', 940, 12800, FALSE, '2026-07-23 19:05:00', TRUE),
(3, 'Old Streaming Setup Tour', 'This setup is long gone - kept for the archives.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-44/640/360', 'https://cdn.vsp.example/videos/44.mp4', 1720, 76200, FALSE, '2026-07-21 13:50:00', FALSE),
(3, 'Bread Scoring Techniques', 'Five patterns, one lame.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-45/640/360', 'https://cdn.vsp.example/videos/45.mp4', 805, 19400, FALSE, '2026-07-19 08:35:00', TRUE),
(3, 'Postgres Performance Tuning Tips', 'EXPLAIN ANALYZE is your friend.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-46/640/360', 'https://cdn.vsp.example/videos/46.mp4', 2130, 102600, FALSE, '2026-07-17 15:00:00', TRUE),
(3, 'Kayaking the Coast - GoPro Edit', 'Two days on the water.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-47/640/360', 'https://cdn.vsp.example/videos/47.mp4', 1090, 26800, FALSE, '2026-07-15 20:30:00', TRUE),
(3, 'Weekly Standup Highlights', 'Best moments from this week.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-48/640/360', 'https://cdn.vsp.example/videos/48.mp4', 560, 3100, TRUE, '2026-07-13 12:15:00', TRUE),
(3, 'My Favorite VS Code Extensions', 'Ten extensions that earn their keep.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-49/640/360', 'https://cdn.vsp.example/videos/49.mp4', 1015, 47500, FALSE, '2026-07-11 10:25:00', TRUE),
(3, 'Year End Reflections and Channel Update', 'Where we have been and where we are going.', 'PUBLIC', 'https://picsum.photos/seed/vsp-video-50/640/360', 'https://cdn.vsp.example/videos/50.mp4', 1440, 12300, FALSE, '2026-07-09 17:40:00', TRUE);

INSERT INTO posts (user_id, content, visibility, created_at, is_edited) VALUES
(1, 'Finally shipped the new portfolio site! Feedback welcome.', 'PUBLIC', '2026-10-10 11:45:00', TRUE),
(2, 'TIL you can use a RETURNING clause in Postgres to get generated IDs back in one round trip. Game changer for batch inserts.', 'PUBLIC', '2026-10-12 08:20:00', FALSE),
(3, 'Studio is closed this week for renovations. Back on Monday!', 'PRIVATE', '2026-10-14 17:05:00', FALSE),
(2, NULL, 'PUBLIC', '2026-10-16 13:22:00', FALSE),
(2, 'Race day tomorrow. Carbs loaded, gear checked, alarm set for 5AM. Course photos from today shakeout run attached.', 'PUBLIC', '2026-10-17 19:30:00', FALSE);

INSERT INTO post_images (post_id, user_id, image_url, sort_order, created_at) VALUES
(1, 1, 'https://picsum.photos/seed/vsp-post-1-img-1/800/600', 0, '2026-10-10 11:46:00'),
(1, 1, 'https://picsum.photos/seed/vsp-post-1-img-2/800/600', 1, '2026-10-10 11:46:30'),
(1, 1, 'https://picsum.photos/seed/vsp-post-1-img-3/800/600', 2, '2026-10-10 11:47:00'),
(4, 2, 'https://picsum.photos/seed/vsp-post-4-img-1/800/600', 0, '2026-10-16 13:22:30'),
(5, 2, 'https://picsum.photos/seed/vsp-post-5-img-1/800/600', 0, '2026-10-17 19:31:00'),
(5, 2, 'https://picsum.photos/seed/vsp-post-5-img-2/800/600', 1, '2026-10-17 19:31:10'),
(5, 2, 'https://picsum.photos/seed/vsp-post-5-img-3/800/600', 2, '2026-10-17 19:31:20'),
(5, 2, 'https://picsum.photos/seed/vsp-post-5-img-4/800/600', 3, '2026-10-17 19:31:30');

INSERT INTO comments (user_id, video_id, post_id, parent_id, content, created_at, is_edited) VALUES
(2, 1, NULL, NULL, 'Great intro! The section on actuator endpoints was really useful.', '2026-10-16 10:00:00', FALSE),
(1, NULL, 2, NULL, 'Wait, that is huge. Trying this today.', '2026-10-12 09:10:00', FALSE),
(3, 1, NULL, 1, 'Same here, rewatched that part twice before it clicked.', '2026-10-16 12:30:00', TRUE);

INSERT INTO likes (user_id, target_type, target_id, created_at) VALUES
(2, 'VIDEO', 1, '2026-10-16 10:02:00'),
(1, 'POST', 2, '2026-10-12 09:11:00'),
(3, 'COMMENT', 1, '2026-10-16 12:31:00');

INSERT INTO subscriptions (subscriber_id, channel_id, status, created_at, decided_at) VALUES
(1, 2, 'APPROVED', '2026-10-01 08:30:00', '2026-10-01 09:00:00'),
(2, 3, 'PENDING', '2026-10-11 14:10:00', '2026-10-11 14:10:00'),
(3, 1, 'REJECTED', '2026-10-13 19:45:00', '2026-10-14 07:20:00');
