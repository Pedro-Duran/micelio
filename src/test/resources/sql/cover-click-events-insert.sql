INSERT INTO users (id, username, email, password, superuser, avatar_url, google_id)
VALUES (1, 'alice', 'alice@test.com', 'test-password', false, null, null),
       (2, 'bob',   'bob@test.com',   'test-password', false, null, null);

INSERT INTO posts (id, title, content, author_id, is_stub, cover_image_url, created_at)
VALUES
(1, 'Post Alice 1', 'Content of first post', 1, false, null, '2024-01-01 10:00:00'),
(2, 'Post Alice 2', 'Content of second post', 1, false, null, '2024-01-02 10:00:00'),
(3, 'Post Bob 1', 'Bob content', 2, false, null, '2024-01-03 10:00:00');

-- 2 COVER_CLICK para post 1, 1 para post 2
-- 3 VIEW para post 1, 1 VIEW para post 2
INSERT INTO events (id, post_id, event_type, session_id, timestamp, subject, cover_image_url)
VALUES
(1, 1, 'COVER_CLICK', 'session-1', '2024-01-01 10:00:00', 'Tech', 'https://s3/cover1.jpg'),
(2, 1, 'COVER_CLICK', 'session-2', '2024-01-01 11:00:00', 'Tech', 'https://s3/cover1.jpg'),
(3, 2, 'COVER_CLICK', 'session-3', '2024-01-01 12:00:00', 'Life', 'https://s3/cover2.jpg'),
(4, 1, 'VIEW',        'session-1', '2024-01-01 10:05:00', null,   null),
(5, 1, 'VIEW',        'session-2', '2024-01-01 11:05:00', null,   null),
(6, 1, 'VIEW',        'session-3', '2024-01-01 12:05:00', null,   null),
(7, 2, 'VIEW',        'session-4', '2024-01-01 13:00:00', null,   null);
