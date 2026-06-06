INSERT INTO users (id, username, email, password, superuser, avatar_url, google_id)
VALUES (1, 'alice', 'alice@test.com', 'test-password', false, null, null),
       (2, 'bob',   'bob@test.com',   'test-password', false, null, null);

INSERT INTO posts (id, title, content, author_id, subject, is_stub, cover_image_url, created_at)
VALUES (1, 'Bob Post', 'Bob content', 2, 'Tech', false, null, '2024-01-01 10:00:00');

INSERT INTO follows (id, follower_id, followed_id, created_at)
VALUES (1, 1, 2, '2024-01-01 09:00:00');
