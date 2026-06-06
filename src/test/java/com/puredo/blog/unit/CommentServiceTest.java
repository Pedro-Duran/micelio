package com.puredo.blog.unit;

import com.puredo.blog.DTO.CommentDTO;
import com.puredo.blog.Entity.Comment;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Comment.CommentRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.Comment.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CommentServiceImpl commentService;

    private User alice;
    private User bob;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");

        post = new Post();
        post.setId(1L);
        post.setTitle("Test Post");

        comment = new Comment();
        comment.setId(1L);
        comment.setContent("Hello");
        comment.setAuthor(alice);
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setReplies(new ArrayList<>());
    }

    // ---- createComment ----

    @Test
    void createComment_validRequest_returnsDTO() {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(1L, "Hello");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(commentRepository.save(any())).thenReturn(comment);

        Optional<CommentDTO.Response.Comment> result = commentService.createComment(request, "alice");

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Hello");
        assertThat(result.get().getAuthorUsername()).isEqualTo("alice");
    }

    @Test
    void createComment_postNotFound_returnsEmpty() {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(99L, "Hello");
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<CommentDTO.Response.Comment> result = commentService.createComment(request, "alice");

        assertThat(result).isEmpty();
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_userNotFound_returnsEmpty() {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(1L, "Hello");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        Optional<CommentDTO.Response.Comment> result = commentService.createComment(request, "nobody");

        assertThat(result).isEmpty();
        verify(commentRepository, never()).save(any());
    }

    // ---- replyToComment ----

    @Test
    void replyToComment_validRequest_setsParentAndPost() {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(1L, "Reply");
        Comment reply = new Comment();
        reply.setId(2L);
        reply.setContent("Reply");
        reply.setAuthor(bob);
        reply.setPost(post);
        reply.setParentComment(comment);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setReplies(new ArrayList<>());

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(commentRepository.save(any())).thenReturn(reply);

        Optional<CommentDTO.Response.Comment> result = commentService.replyToComment(1L, request, "bob");

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Reply");
    }

    @Test
    void replyToComment_parentNotFound_returnsEmpty() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<CommentDTO.Response.Comment> result = commentService.replyToComment(
                99L, new CommentDTO.Request.Create(1L, "Reply"), "bob");

        assertThat(result).isEmpty();
    }

    @Test
    void replyToComment_userNotFound_returnsEmpty() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<CommentDTO.Response.Comment> result = commentService.replyToComment(
                1L, new CommentDTO.Request.Create(1L, "Reply"), "ghost");

        assertThat(result).isEmpty();
    }

    // ---- getCommentsByPost ----

    @Test
    void getCommentsByPost_returnsOnlyTopLevelComments() {
        when(commentRepository.findByPostIdAndParentCommentIsNull(1L)).thenReturn(List.of(comment));

        List<CommentDTO.Response.Comment> result = commentService.getCommentsByPost(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Hello");
    }

    @Test
    void getCommentsByPost_noComments_returnsEmptyList() {
        when(commentRepository.findByPostIdAndParentCommentIsNull(1L)).thenReturn(List.of());

        assertThat(commentService.getCommentsByPost(1L)).isEmpty();
    }

    // ---- updateComment ----

    @Test
    void updateComment_authorUpdatesOwnComment_returnsUpdated() {
        CommentDTO.Request.Update request = new CommentDTO.Request.Update("Updated content");
        Comment updated = new Comment();
        updated.setId(1L);
        updated.setContent("Updated content");
        updated.setAuthor(alice);
        updated.setPost(post);
        updated.setCreatedAt(LocalDateTime.now());
        updated.setReplies(new ArrayList<>());

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenReturn(updated);

        CommentDTO.Response.Comment result = commentService.updateComment(1L, request, "alice");

        assertThat(result.getContent()).isEqualTo("Updated content");
    }

    @Test
    void updateComment_differentUser_throwsIllegalState() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(1L, new CommentDTO.Request.Update("x"), "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    void updateComment_commentNotFound_throwsNoSuchElement() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(99L, new CommentDTO.Request.Update("x"), "alice"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---- deleteComment ----

    @Test
    void deleteComment_authorDeletesOwnComment_succeeds() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L, "alice", false);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_superuserDeletesAnyComment_succeeds() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L, "bob", true);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_unauthorizedUser_throwsIllegalState() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(1L, "bob", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    void deleteComment_commentNotFound_throwsNoSuchElement() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(99L, "alice", false))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---- nested replies are mapped recursively ----

    @Test
    void getCommentsByPost_withReplies_mapsRepliesRecursively() {
        Comment reply = new Comment();
        reply.setId(2L);
        reply.setContent("Reply");
        reply.setAuthor(bob);
        reply.setPost(post);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setReplies(new ArrayList<>());
        comment.setReplies(List.of(reply));

        when(commentRepository.findByPostIdAndParentCommentIsNull(1L)).thenReturn(List.of(comment));

        List<CommentDTO.Response.Comment> result = commentService.getCommentsByPost(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReplies()).hasSize(1);
        assertThat(result.get(0).getReplies().get(0).getContent()).isEqualTo("Reply");
    }
}