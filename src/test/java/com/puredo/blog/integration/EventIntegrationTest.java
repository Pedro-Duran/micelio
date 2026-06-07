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

    // ---- POST /api/events/register ----

    @ParameterizedTest
    @EnumSource(EventType.class)
    @Sql("/sql/posts-insert.sql")
    void registerEvent_allEventTypes_returns200WithSavedEvent(EventType eventType) throws Exception {
        EventDTO.Request.Register request = new EventDTO.Request.Register(
                1L, eventType, "session-test", null, null, null, null, null);

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
                1L, EventType.VIEW, "session-abc", 90L, "twitter", "referrer.com", null, null);

        mockMvc.perform(post("/api/events/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value(90))
                .andExpect(jsonPath("$.utmSource").value("twitter"))
                .andExpect(jsonPath("$.referredBy").value("referrer.com"));
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void registerEvent_subjectClick_persistsSubjectAndCoverImageUrl() throws Exception {
        EventDTO.Request.Register request = new EventDTO.Request.Register(
                1L, EventType.SUBJECT_CLICK, "session-x", null, null, null, "Tech", "https://s3/cover.jpg");

        mockMvc.perform(post("/api/events/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("SUBJECT_CLICK"))
                .andExpect(jsonPath("$.subject").value("Tech"))
                .andExpect(jsonPath("$.coverImageUrl").value("https://s3/cover.jpg"));
    }

    // ---- GET /api/events/byPost ----

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

    // ---- GET /api/events/summary ----

    @Test
    @Sql("/sql/events-insert.sql")
    @WithMockUser(username = "admin", roles = "SUPERUSER")
    void getSummary_asSuperuser_returnsAllPostsWithCommentAndLikeCount() throws Exception {
        mockMvc.perform(get("/api/events/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].postId", hasItems(1, 2)))
                .andExpect(jsonPath("$[0].commentCount").isNumber())
                .andExpect(jsonPath("$[0].likeCount").isNumber());
    }

    @Test
    @Sql("/sql/events-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void getSummary_asRegularUser_returnsOwnPostsOnly() throws Exception {
        mockMvc.perform(get("/api/events/summary"))
                .andExpect(status().isOk())
                // alice é autora dos posts 1 e 2 em events-insert.sql
                .andExpect(jsonPath("$[*].postId", everyItem(is(oneOf(1, 2)))));
    }

    @Test
    @Sql("/sql/events-insert.sql")
    @WithMockUser(username = "alice", roles = "USER")
    void getSummary_viewCountIsCorrect() throws Exception {
        mockMvc.perform(get("/api/events/summary"))
                .andExpect(status().isOk())
                // post 1 tem 2 VIEW events
                .andExpect(jsonPath("$[?(@.postId == 1)].viewCount", contains(2)));
    }

    // ---- GET /api/events/referrers ----

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

    // ---- GET /api/events/subscriptions ----

    @Test
    @Sql("/sql/stub-events-insert.sql")
    void getSubscriptionAnalytics_returnsTopSubscribersAndAuthors() throws Exception {
        mockMvc.perform(get("/api/events/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topSubscribers").isArray())
                .andExpect(jsonPath("$.topSubscribers", not(empty())))
                // bob subscreveu 2x (post 1 e 2)
                .andExpect(jsonPath("$.topSubscribers[0].username").value("bob"))
                .andExpect(jsonPath("$.topSubscribers[0].totalSubscriptions").value(2))
                .andExpect(jsonPath("$.topAuthors").isArray())
                .andExpect(jsonPath("$.topAuthors", not(empty())))
                // alice é autora dos 2 posts com 3 inscrições total
                .andExpect(jsonPath("$.topAuthors[0].username").value("alice"))
                .andExpect(jsonPath("$.topAuthors[0].subscriptionCount").value(3))
                .andExpect(jsonPath("$.topAuthors[0].postCount").value(2));
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void getSubscriptionAnalytics_noStubSubscribeEvents_returnsEmptyLists() throws Exception {
        mockMvc.perform(get("/api/events/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topSubscribers", empty()))
                .andExpect(jsonPath("$.topAuthors", empty()));
    }

    // ---- GET /api/events/coverConversion ----

    @Test
    @Sql("/sql/subject-click-events-insert.sql")
    void getCoverConversionAnalytics_returnsAggregatedDataOrderedByClicks() throws Exception {
        mockMvc.perform(get("/api/events/coverConversion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // cover1.jpg tem 2 cliques, cover2.jpg tem 1 — ordenado desc
                .andExpect(jsonPath("$[0].coverImageUrl").value("https://s3/cover1.jpg"))
                .andExpect(jsonPath("$[0].clicks").value(2))
                .andExpect(jsonPath("$[0].subject").value("Tech"))
                .andExpect(jsonPath("$[1].coverImageUrl").value("https://s3/cover2.jpg"))
                .andExpect(jsonPath("$[1].clicks").value(1));
    }

    @Test
    @Sql("/sql/posts-insert.sql")
    void getCoverConversionAnalytics_noSubjectClickEvents_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/events/coverConversion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }
}