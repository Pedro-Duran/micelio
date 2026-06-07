package com.puredo.blog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puredo.blog.DTO.CommentDTO;
import com.puredo.blog.Entity.EventType;
import com.puredo.blog.Repository.Event.EventRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired EventRepository eventRepository;

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
    @Sql("/sql/comments-insert.sql")
    void createComment_registersCommentEvent() throws Exception {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(1L, "Triggers event");

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertThat(eventRepository.findByPostId(1L))
                .anyMatch(e -> e.getEventType() == EventType.COMMENT && "alice".equals(e.getUsername()));
    }

    @Test
    void createComment_postNotFound_returns400AndNoEvent() throws Exception {
        CommentDTO.Request.Create request = new CommentDTO.Request.Create(999L, "Comment");

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(eventRepository.findByPostId(999L)
                .stream().anyMatch(e -> e.getEventType() == EventType.COMMENT)).isFalse();
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
        mockMvc.perform(post("/api/comments/999/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDTO.Request.Create(1L, "Reply"))))
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
        // comentário 2 é de alice (author_id=1)
        mockMvc.perform(put("/api/comments/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDTO.Request.Update("Updated content"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"));
    }

    @Test
    @Sql("/sql/comments-insert.sql")
    void updateComment_otherUserComment_returns403() throws Exception {
        // comentário 1 é de bob (author_id=2)
        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDTO.Request.Update("Updated"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateComment_commentNotFound_returns404() throws Exception {
        mockMvc.perform(put("/api/comments/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDTO.Request.Update("Updated"))))
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
        // comentário 1 é de bob
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComment_commentNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/comments/999"))
                .andExpect(status().isNotFound());
    }
}