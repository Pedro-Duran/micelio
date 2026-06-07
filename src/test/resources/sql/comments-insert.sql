INSERT INTO users (id, username, email, password, superuser, avatar_url, google_id)
VALUES (1, 'alice', 'alice@test.com', 'test-password', false, null, null),
       (2, 'bob',   'bob@test.com',   'test-password', false, null, null);

INSERT INTO posts (
    id,
    title,
    content,
    author_id,
    is_stub,
    cover_image_url,
    created_at
)
VALUES
(1, 'Post Alice 1', 'Content of first post', 1, false, null, '2024-01-01 10:00:00'),
(2, 'Post Alice 2', 'Content of second post', 1, false, null, '2024-01-02 10:00:00'),
(3, 'Post Bob 1', 'Bob content', 2, false, null, '2024-01-03 10:00:00'),
(4, 'Stub Post', '', 1, true, null, '2024-01-04 10:00:00');

INSERT INTO comments (id, content, author_id, post_id, parent_comment_id, created_at)
VALUES (1, 'First comment by bob',      2, 1, null, '2024-01-01 11:00:00'),
       (2, 'Second comment by alice',   1, 1, null, '2024-01-01 12:00:00'),
       (3, 'Reply by alice to comment', 1, 1, 1,    '2024-01-01 13:00:00');