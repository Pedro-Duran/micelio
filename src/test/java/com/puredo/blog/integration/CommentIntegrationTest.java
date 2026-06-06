package com.puredo.blog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puredo.blog.DTO.CommentDTO;
import com.puredo.blog.Service.Email.EmailService;
import com.puredo.blog.Service.Post.StubNotificationService;
import com.puredo.blog.Service.Storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "alice", roles = "USER")
class CommentIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean StorageService storageService;
    @MockBean EmailService emailService;
    @MockBean StubNotificationService stubNotificationService;

    // ---- POST /api/comments ----

    @Test
    @Sql("/sql/comments-insert.sql")
    void createComment_validRequest_returns201WithComment() throws Exception {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(1L, "New comment text");

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("New comment text"))
                .andExpect(jsonPath("$.authorUsername").value("alice"));
    }

    @Test
    void createComment_postNotFound_returns400() throws Exception {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(999L, "Comment");

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/comments/{id}/reply ----

    @Test
    @Sql("/sql/comments-insert.sql")
    void replyToComment_validRequest_returns201WithReply() throws Exception {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(1L, "A reply");

        mockMvc.perform(post("/api/comments/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("A reply"))
                .andExpect(jsonPath("$.authorUsername").value("alice"));
    }

    @Test
    void replyToComment_parentNotFound_returns404() throws Exception {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(1L, "Reply");

        mockMvc.perform(post("/api/comments/999/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/comments/byPost ----

    @Test
    @Sql("/sql/comments-insert.sql")
    void getCommentsByPost_returnsTopLevelComments() throws Exception {
        mockMvc.perform(get("/api/comments/byPost").param("postId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].replies").exists());
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void getCommentsByPost_noComments_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/comments/byPost").param("postId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    // ---- PUT /api/comments/{id} ----

    @Test
    @Sql("/sql/comments-insert.sql")
    void updateComment_ownComment_returns200WithUpdatedContent() throws Exception {
        CommentDTO.Request.Update request = new CommentDTO.Request.Update("Updated content");

        mockMvc.perform(put("/api/comments/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"));
    }

    @Test
    @Sql("/sql/comments-insert.sql")
    void updateComment_otherUserComment_returns403() throws Exception {
        CommentDTO.Request.Update request = new CommentDTO.Request.Update("Updated");

        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateComment_commentNotFound_returns404() throws Exception {
        CommentDTO.Request.Update request = new CommentDTO.Request.Update("Updated");

        mockMvc.perform(put("/api/comments/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/comments/{id} ----

    @Test
    @Sql("/sql/comments-insert.sql")
    void deleteComment_ownComment_returns204() throws Exception {
        mockMvc.perform(delete("/api/comments/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql("/sql/comments-insert.sql")
    @WithMockUser(username = "admin", roles = "SUPERUSER")
    void deleteComment_asSuperuser_canDeleteAnyComment_returns204() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql("/sql/comments-insert.sql")
    void deleteComment_otherUserComment_returns403() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComment_commentNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/comments/999"))
                .andExpect(status().isNotFound());
    }
}