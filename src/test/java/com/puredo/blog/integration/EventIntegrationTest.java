package com.puredo.blog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.EventType;
import com.puredo.blog.Service.Email.EmailService;
import com.puredo.blog.Service.Post.StubNotificationService;
import com.puredo.blog.Service.Storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class EventIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean StorageService storageService;
    @MockBean EmailService emailService;
    @MockBean StubNotificationService stubNotificationService;

    // ---- POST /api/events/register (permitAll) ----

    @ParameterizedTest
    @EnumSource(EventType.class)
    @Sql("/sql/posts-insert.sql")
    void registerEvent_allEventTypes_returns200WithSavedEvent(EventType eventType) throws Exception {
        EventDTO.Request.Register request = new EventDTO.Request.Register(
                1L, eventType, "session-test", null, null, null);

        mockMvc.perform(post("/api/events/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.eventType").value(eventType.name()))
                .andExpect(jsonPath("$.sessionId").value("session-test"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void registerEvent_withOptionalFields_persistsAllData() throws Exception {
        EventDTO.Request.Register request = new EventDTO.Request.Register(
                1L, EventType.VIEW, "session-abc", 90L, "twitter", "referrer.com");

        mockMvc.perform(post("/api/events/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value(90))
                .andExpect(jsonPath("$.utmSource").value("twitter"))
                .andExpect(jsonPath("$.referredBy").value("referrer.com"));
    }

    // ---- GET /api/events/byPost (permitAll) ----

    @Test
    @Sql("/sql/events-insert.sql")
    void getEventsByPost_returnsEventsForPost() throws Exception {
        mockMvc.perform(get("/api/events/byPost").param("postId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].postId", everyItem(is(1))));
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void getEventsByPost_noEvents_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/events/byPost").param("postId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    // ---- GET /api/events/summary (requires auth) ----

    @Test
    @Sql("/sql/events-insert.sql")
    @WithMockUser(username = "admin", roles = "SUPERUSER")
    void getSummary_asSuperuser_returnsAllPosts() throws Exception {
        mockMvc.perform(get("/api/events/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].postId", hasItems(1, 2)));
    }

    @Test
    @Sql("/sql/events-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void getSummary_asRegularUser_returnsOnlyOwnPosts() throws Exception {
        mockMvc.perform(get("/api/events/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].postId", everyItem(is(oneOf(1, 2)))));
    }

    @Test
    @Sql("/sql/events-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void getSummary_viewCountIsCorrect() throws Exception {
        mockMvc.perform(get("/api/events/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.postId == 1)].viewCount", contains(2)));
    }

    // ---- GET /api/events/referrers (permitAll) ----

    @Test
    @Sql("/sql/events-insert.sql")
    void getReferrerSummary_returnsReferrerData() throws Exception {
        mockMvc.perform(get("/api/events/referrers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("referrer.com"))
                .andExpect(jsonPath("$[0].totalReferrals").value(1))
                .andExpect(jsonPath("$[0].byPlatform.twitter").value(1));
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void getReferrerSummary_noReferrers_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/events/referrers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }
}