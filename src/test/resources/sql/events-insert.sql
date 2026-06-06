INSERT INTO users (id, username, email, password, superuser, avatar_url, google_id)
VALUES (1, 'alice', 'alice@test.com', 'test-password', false, null, null);

INSERT INTO posts (id, title, content, author_id, subject, is_stub, cover_image_url, created_at)
VALUES (1, 'Post 1', 'Content 1', 1, 'Tech', false, null, '2024-01-01 10:00:00'),
       (2, 'Post 2', 'Content 2', 1, 'Tech', false, null, '2024-01-02 10:00:00');

INSERT INTO events (id, post_id, event_type, session_id, timestamp, duration, utm_source, referred_by)
VALUES (1, 1, 'VIEW',       'session-1', '2024-01-01 10:00:00', 120,  null,      null),
       (2, 1, 'VIEW',       'session-2', '2024-01-01 11:00:00', 90,   null,      null),
       (3, 1, 'CLICK_NODE', 'session-1', '2024-01-01 10:30:00', null, null,      null),
       (4, 2, 'VIEW',       'session-3', '2024-01-02 10:00:00', 60,   'twitter', 'referrer.com');
