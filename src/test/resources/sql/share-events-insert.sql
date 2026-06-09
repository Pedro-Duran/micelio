INSERT INTO users (id, username, email, password, superuser, avatar_url, google_id)
VALUES (1, 'alice', 'alice@test.com', 'test-password', false, null, null),
       (2, 'pedro', 'pedro@test.com', 'test-password', false, null, null);

INSERT INTO posts (id, title, content, author_id, is_stub, cover_image_url, created_at)
VALUES
(1, 'Post 1', 'Content 1', 1, false, null, '2024-01-01 10:00:00'),
(2, 'Post 2', 'Content 2', 1, false, null, '2024-01-02 10:00:00');

-- pedro: 2 shares em Tech + 1 share em Life = 3 total
-- alice: 1 share em Tech = 1 total
INSERT INTO events (id, post_id, event_type, session_id, timestamp, subject, username)
VALUES
(1, 1, 'SHARE', 'session-1', '2024-01-01 10:00:00', 'Tech', 'pedro'),
(2, 2, 'SHARE', 'session-2', '2024-01-01 11:00:00', 'Tech', 'pedro'),
(3, 1, 'SHARE', 'session-3', '2024-01-01 12:00:00', 'Life', 'pedro'),
(4, 1, 'SHARE', 'session-4', '2024-01-01 13:00:00', 'Tech', 'alice');
