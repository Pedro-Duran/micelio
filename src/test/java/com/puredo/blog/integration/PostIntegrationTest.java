package com.puredo.blog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "alice", roles = "USER")
class PostIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean StorageService storageService;
    @MockBean EmailService emailService;
    @MockBean StubNotificationService stubNotificationService;

    // ---- POST /api/posts/createPost ----

    @Test
    @Sql("/sql/users-insert.sql")
    void createPost_existingAuthor_returns200WithPost() throws Exception {
        PostDTO.Request.Create request = new PostDTO.Request.Create(
                "My Title", "My content", "alice", null, null, "Tech");

        mockMvc.perform(post("/api/posts/createPost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Title"))
                .andExpect(jsonPath("$.content").value("My content"))
                .andExpect(jsonPath("$.author.username").value("alice"));
    }

    @Test
    void createPost_unknownAuthor_returns400() throws Exception {
        PostDTO.Request.Create request = new PostDTO.Request.Create(
                "Title", "Content", "nobody", null, null, "Tech");

        mockMvc.perform(post("/api/posts/createPost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql("/sql/users-insert.sql")
    void createPost_withWikilink_createsStubForUnknownTitle() throws Exception {
        PostDTO.Request.Create request = new PostDTO.Request.Create(
                "Main Post", "Content", "alice", null, List.of("Unknown Topic"), "Tech");

        mockMvc.perform(post("/api/posts/createPost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links", hasSize(1)));
    }

    // ---- GET /api/posts/verPosts ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void getAllPosts_returnsPaginatedResult() throws Exception {
        mockMvc.perform(get("/api/posts/verPosts").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));
    }

    // ---- GET /api/posts/feed ----

    @Test
    @Sql("/sql/users-insert.sql")
    void getFeed_noFollows_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/posts/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()));
    }

    @Test
    @Sql("/sql/follows-insert.sql")
    void getFeed_withFollowedUser_returnsHisPost() throws Exception {
        mockMvc.perform(get("/api/posts/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].author.username").value("bob"));
    }

    // ---- GET /api/posts/explore ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void getExplore_returnsOnlyOtherUsersPosts() throws Exception {
        mockMvc.perform(get("/api/posts/explore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].author.username", everyItem(not("alice"))));
    }

    // ---- GET /api/posts/mine ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void getMyPosts_returnsOnlyAlicesPosts() throws Exception {
        mockMvc.perform(get("/api/posts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].author.username", everyItem(is("alice"))));
    }

    // ---- PUT /api/posts/updatePost ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void updatePost_existingPost_returns200WithUpdatedData() throws Exception {
        PostDTO.Request.Update request = new PostDTO.Request.Update(
                1L, "Updated Title", "Updated content", null, null, "Science");

        mockMvc.perform(put("/api/posts/updatePost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.subject").value("Science"));
    }

    @Test
    void updatePost_postNotFound_returns404() throws Exception {
        PostDTO.Request.Update request = new PostDTO.Request.Update(
                999L, "Title", "Content", null, null, "Tech");

        mockMvc.perform(put("/api/posts/updatePost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void updatePost_stubBecomesPublished_triggersNotification() throws Exception {
        PostDTO.Request.Update request = new PostDTO.Request.Update(
                4L, "Stub Post", "Now has real content", null, null, "Tech");

        mockMvc.perform(put("/api/posts/updatePost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(stubNotificationService).notifyAndCleanup(any(Post.class));
    }

    // ---- DELETE /api/posts/deletePost ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void deletePost_existingPost_returns204() throws Exception {
        mockMvc.perform(delete("/api/posts/deletePost").param("id", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePost_postNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/posts/deletePost").param("id", "999"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/posts/{postId}/notify ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void subscribeToStub_stubPost_returns200() throws Exception {
        mockMvc.perform(post("/api/posts/4/notify"))
                .andExpect(status().isOk());
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void subscribeToStub_notStubPost_returns400() throws Exception {
        mockMvc.perform(post("/api/posts/1/notify"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/posts/subjects ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void getSubjects_returnsDistinctSubjects() throws Exception {
        mockMvc.perform(get("/api/posts/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItems("Tech", "Life")));
    }

    // ---- GET /api/posts/postsIdForThisSubject ----

    @Test
    @Sql("/sql/posts-insert.sql")
    void getPostsBySubject_returnsMapOfIdToTitle() throws Exception {
        mockMvc.perform(get("/api/posts/postsIdForThisSubject").param("subject", "Tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$", not(anEmptyMap())));
    }
}