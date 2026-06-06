package com.puredo.blog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puredo.blog.DTO.UserDTO;
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
class UserIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean StorageService storageService;
    @MockBean EmailService emailService;
    @MockBean StubNotificationService stubNotificationService;

    // ---- POST /api/users/createUser (permitAll) ----

    @Test
    void createUser_validRequest_returns200WithUser() throws Exception {
        UserDTO.Request.Create request = new UserDTO.Request.Create("newuser", "password123", "new@test.com");

        mockMvc.perform(post("/api/users/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @Sql("/sql/users-insert.sql")
    void createUser_duplicateUsername_returns400() throws Exception {
        UserDTO.Request.Create request = new UserDTO.Request.Create("alice", "password", "other@test.com");

        mockMvc.perform(post("/api/users/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql("/sql/users-insert.sql")
    void createUser_duplicateEmail_returns400() throws Exception {
        UserDTO.Request.Create request = new UserDTO.Request.Create("newuser", "password", "alice@test.com");

        mockMvc.perform(post("/api/users/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/users/listUsers (permitAll) ----

    @Test
    @Sql("/sql/users-insert.sql")
    void getAllUsers_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/users/listUsers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].username", hasItems("alice", "bob", "admin")));
    }

    // ---- GET /api/users/getUserByUsername (permitAll) ----

    @Test
    @Sql("/sql/users-insert.sql")
    void getUserByUsername_existingUser_returns200() throws Exception {
        mockMvc.perform(get("/api/users/getUserByUsername").param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void getUserByUsername_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users/getUserByUsername").param("username", "ghost"))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/users/search (permitAll) ----

    @Test
    @Sql("/sql/users-insert.sql")
    void searchUsers_withQuery_returnsMatches() throws Exception {
        mockMvc.perform(get("/api/users/search").param("username", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", everyItem(containsStringIgnoringCase("ali"))));
    }

    @Test
    @Sql("/sql/users-insert.sql")
    void searchUsers_withoutQuery_returnsAll() throws Exception {
        mockMvc.perform(get("/api/users/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    // ---- PUT /api/users/updateUser (requires auth) ----

    @Test
    @Sql("/sql/users-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void updateUser_existingUser_returns200() throws Exception {
        UserDTO.Request.Update request = new UserDTO.Request.Update(1L, "alice-updated", "newpass");

        mockMvc.perform(put("/api/users/updateUser")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice-updated"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void updateUser_notFound_returns404() throws Exception {
        UserDTO.Request.Update request = new UserDTO.Request.Update(999L, "x", "y");

        mockMvc.perform(put("/api/users/updateUser")
                        .param("id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/users/deleteUser (requires auth) ----

    @Test
    @Sql("/sql/users-insert.sql")
    @WithMockUser(username = "admin", roles = "SUPERUSER")
    void deleteUser_existingUser_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/deleteUser").param("id", "2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPERUSER")
    void deleteUser_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/users/deleteUser").param("id", "999"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/users/{username}/follow ----

    @Test
    @Sql("/sql/users-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void follow_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/users/bob/follow"))
                .andExpect(status().isCreated());
    }

    @Test
    @Sql("/sql/users-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void follow_self_returns400() throws Exception {
        mockMvc.perform(post("/api/users/alice/follow"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql("/sql/follows-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void follow_alreadyFollowing_isIdempotentAndReturns201() throws Exception {
        mockMvc.perform(post("/api/users/bob/follow"))
                .andExpect(status().isCreated());
    }

    // ---- DELETE /api/users/{username}/follow ----

    @Test
    @Sql("/sql/follows-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void unfollow_existingFollow_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/bob/follow"))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql("/sql/users-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void unfollow_notFollowing_isIdempotentAndReturns204() throws Exception {
        mockMvc.perform(delete("/api/users/bob/follow"))
                .andExpect(status().isNoContent());
    }

    // ---- GET /api/users/{username}/following ----

    @Test
    @Sql("/sql/follows-insert.sql")
    void getFollowing_returnsFollowedUsers() throws Exception {
        mockMvc.perform(get("/api/users/alice/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("bob"));
    }

    // ---- GET /api/users/{username}/followers ----

    @Test
    @Sql("/sql/follows-insert.sql")
    void getFollowers_returnsFollowerUsers() throws Exception {
        mockMvc.perform(get("/api/users/bob/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    // ---- GET /api/users/{username}/isFollowing ----

    @Test
    @Sql("/sql/follows-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void isFollowing_aliceFollowsBob_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/users/bob/isFollowing"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @Sql("/sql/users-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void isFollowing_notFollowing_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/users/bob/isFollowing"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
