package com.puredo.blog.Service.Comment;

import com.puredo.blog.DTO.CommentDTO;

import java.util.List;
import java.util.Optional;

public interface CommentService {
    Optional<CommentDTO.Response.Comment> createComment(CommentDTO.Request.Create request, String username);
    Optional<CommentDTO.Response.Comment> replyToComment(Long parentId, CommentDTO.Request.Create request, String username);
    List<CommentDTO.Response.Comment> getCommentsByPost(Long postId);
    CommentDTO.Response.Comment updateComment(Long id, CommentDTO.Request.Update request, String username);
    void deleteComment(Long id, String username, boolean isSuperuser);
}
