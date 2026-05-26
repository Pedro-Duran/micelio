package com.puredo.blog.Service.Comment;

import com.puredo.blog.DTO.CommentDTO;
import com.puredo.blog.Entity.Comment;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Comment.CommentRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<CommentDTO.Response.Comment> createComment(CommentDTO.Request.Create request, String username) {
        Optional<Post> post = postRepository.findById(request.getPostId());
        Optional<User> author = userRepository.findByUsername(username);
        if (post.isEmpty() || author.isEmpty()) return Optional.empty();

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAuthor(author.get());
        comment.setPost(post.get());

        return Optional.of(toDTO(commentRepository.save(comment)));
    }

    @Override
    public Optional<CommentDTO.Response.Comment> replyToComment(Long parentId, CommentDTO.Request.Create request, String username) {
        Optional<Comment> parent = commentRepository.findById(parentId);
        Optional<User> author = userRepository.findByUsername(username);
        if (parent.isEmpty() || author.isEmpty()) return Optional.empty();

        Comment reply = new Comment();
        reply.setContent(request.getContent());
        reply.setAuthor(author.get());
        reply.setPost(parent.get().getPost());
        reply.setParentComment(parent.get());

        return Optional.of(toDTO(commentRepository.save(reply)));
    }

    @Override
    public List<CommentDTO.Response.Comment> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdAndParentCommentIsNull(postId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long id, String username, boolean isSuperuser) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Comentário não encontrado"));

        if (!isSuperuser && !comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("Sem permissão para deletar este comentário");
        }

        commentRepository.delete(comment);
    }

    private CommentDTO.Response.Comment toDTO(Comment comment) {
        List<CommentDTO.Response.Comment> replies = comment.getReplies().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());

        return new CommentDTO.Response.Comment(
            comment.getId(),
            comment.getContent(),
            comment.getAuthor().getUsername(),
            comment.getCreatedAt().toString(),
            replies
        );
    }
}
