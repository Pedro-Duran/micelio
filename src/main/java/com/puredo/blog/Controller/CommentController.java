package com.puredo.blog.Controller;

import com.puredo.blog.DTO.CommentDTO;
import com.puredo.blog.Service.Comment.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "http://localhost:3000")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentDTO.Response.Comment> createComment(
        @RequestBody CommentDTO.Request.Create request,
        Authentication authentication
    ) {
        return commentService.createComment(request, authentication.getName())
            .map(c -> ResponseEntity.status(HttpStatus.CREATED).body(c))
            .orElse(ResponseEntity.badRequest().build());
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<CommentDTO.Response.Comment> replyToComment(
        @PathVariable Long id,
        @RequestBody CommentDTO.Request.Create request,
        Authentication authentication
    ) {
        return commentService.replyToComment(id, request, authentication.getName())
            .map(c -> ResponseEntity.status(HttpStatus.CREATED).body(c))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/byPost")
    public ResponseEntity<List<CommentDTO.Response.Comment>> getCommentsByPost(@RequestParam Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, Authentication authentication) {
        boolean isSuperuser = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERUSER"));
        try {
            commentService.deleteComment(id, authentication.getName(), isSuperuser);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
