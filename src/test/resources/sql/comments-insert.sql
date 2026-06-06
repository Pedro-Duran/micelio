INSERT INTO users (id, username, email, password, superuser, avatar_url, google_id)
VALUES (1, 'alice', 'alice@test.com', 'test-password', false, null, null),
       (2, 'bob',   'bob@test.com',   'test-password', false, null, null);

INSERT INTO posts (id, title, content, author_id, subject, is_stub, cover_image_url, created_at)
VALUES (1, 'Test Post', 'Content', 1, 'Tech', false, null, '2024-01-01 10:00:00');

INSERT INTO comments (id, content, author_id, post_id, parent_comment_id, created_at)
VALUES (1, 'First comment by bob',      2, 1, null, '2024-01-01 11:00:00'),
       (2, 'Second comment by alice',   1, 1, null, '2024-01-01 12:00:00'),
       (3, 'Reply by alice to comment', 1, 1, 1,    '2024-01-01 13:00:00');