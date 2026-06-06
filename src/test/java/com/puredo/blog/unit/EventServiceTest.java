package com.puredo.blog.unit;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.Event;
import com.puredo.blog.Entity.EventType;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Repository.Event.EventRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Service.Event.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock PostRepository postRepository;

    @InjectMocks EventServiceImpl eventService;

    private Event event;
    private Post post;

    @BeforeEach
    void setUp() {
        post = new Post();
        post.setId(1L);
        post.setTitle("Test Post");

        event = new Event();
        event.setId(1L);
        event.setPostId(1L);
        event.setEventType(EventType.VIEW);
        event.setSessionId("session-abc");
        event.setTimestamp(LocalDateTime.now());
    }

    // ---- registerEvent ----

    @ParameterizedTest
    @EnumSource(EventType.class)
    void registerEvent_allEventTypes_savesAndReturnsDTO(EventType eventType) {
        EventDTO.Request.Register request = new EventDTO.Request.Register(
                1L, eventType, "session-abc", null, null, null);
        event.setEventType(eventType);
        when(eventRepository.save(any())).thenReturn(event);

        EventDTO.Response.EventSaved result = eventService.registerEvent(request);

        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(1L);
        assertThat(result.getSessionId()).isEqualTo("session-abc");
    }

    @Test
    void registerEvent_withOptionalFields_mapsAllFieldsCorrectly() {
        EventDTO.Request.Register request = new EventDTO.Request.Register(
                1L, EventType.VIEW, "session-1", 120L, "twitter", "referrer.com");
        event.setDuration(120L);
        event.setUtmSource("twitter");
        event.setReferredBy("referrer.com");
        when(eventRepository.save(any())).thenReturn(event);

        EventDTO.Response.EventSaved result = eventService.registerEvent(request);

        assertThat(result.getDuration()).isEqualTo(120L);
        assertThat(result.getUtmSource()).isEqualTo("twitter");
        assertThat(result.getReferredBy()).isEqualTo("referrer.com");
    }

    // ---- getEventsByPost ----

    @Test
    void getEventsByPost_returnsAllEventsForPost() {
        Event event2 = new Event();
        event2.setId(2L);
        event2.setPostId(1L);
        event2.setEventType(EventType.CLICK_NODE);
        event2.setSessionId("session-xyz");
        event2.setTimestamp(LocalDateTime.now());

        when(eventRepository.findByPostId(1L)).thenReturn(List.of(event, event2));

        List<EventDTO.Response.EventSaved> result = eventService.getEventsByPost(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventDTO.Response.EventSaved::getPostId).containsOnly(1L);
    }

    @Test
    void getEventsByPost_noEvents_returnsEmptyList() {
        when(eventRepository.findByPostId(99L)).thenReturn(List.of());

        assertThat(eventService.getEventsByPost(99L)).isEmpty();
    }

    // ---- getSummary ----

    @Test
    void getSummary_asSuperuser_includesAllPosts() {
        Post post2 = new Post();
        post2.setId(2L);
        post2.setTitle("Post 2");

        // countByPostAndType retorna List<Object[]> onde row[1] é Long (COUNT)
        // avgDurationByPostAndType retorna List<Object[]> onde row[1] é Double/Number (AVG)
        // Listas propositalmente separadas: toMap() faz cast para Long, toDoubleMap() para Number
        when(eventRepository.countByPostAndType(EventType.VIEW))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}, new Object[]{2L, 1L}));
        when(eventRepository.countByPostAndType(EventType.CLICK_NODE))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}));
        when(eventRepository.avgDurationByPostAndType(EventType.VIEW))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 150.0}));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.findById(2L)).thenReturn(Optional.of(post2));

        List<EventDTO.Response.Summary> result = eventService.getSummary("alice", true);

        assertThat(result).hasSize(2);
        EventDTO.Response.Summary s1 = result.stream()
                .filter(s -> s.getPostId() == 1L).findFirst().orElseThrow();
        assertThat(s1.getViewCount()).isEqualTo(3L);
        assertThat(s1.getNodeClickCount()).isEqualTo(2L);
        assertThat(s1.getAvgDuration()).isEqualTo(150.0);
    }

    @Test
    void getSummary_asRegularUser_filtersToOwnPosts() {
        when(eventRepository.countByPostAndType(EventType.VIEW))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 5L}, new Object[]{2L, 3L}));
        when(eventRepository.countByPostAndType(EventType.CLICK_NODE))
                .thenReturn(List.<Object[]>of());
        when(eventRepository.avgDurationByPostAndType(EventType.VIEW))
                .thenReturn(List.<Object[]>of());
        when(postRepository.findPostIdsByAuthorUsername("alice")).thenReturn(List.of(1L));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        List<EventDTO.Response.Summary> result = eventService.getSummary("alice", false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPostId()).isEqualTo(1L);
    }

    @Test
    void getSummary_removedPost_usesDefaultTitle() {
        when(eventRepository.countByPostAndType(EventType.VIEW))
                .thenReturn(List.<Object[]>of(new Object[]{999L, 1L}));
        when(eventRepository.countByPostAndType(EventType.CLICK_NODE))
                .thenReturn(List.<Object[]>of());
        when(eventRepository.avgDurationByPostAndType(EventType.VIEW))
                .thenReturn(List.<Object[]>of());
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        List<EventDTO.Response.Summary> result = eventService.getSummary("alice", true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Post removido");
    }

    // ---- getReferrerSummary ----

    @Test
    void getReferrerSummary_aggregatesReferrersAndPlatforms() {
        when(eventRepository.countByReferrer())
                .thenReturn(List.<Object[]>of(
                        new Object[]{"site.com", 10L},
                        new Object[]{"blog.org", 5L}));
        when(eventRepository.countByReferrerAndPlatform())
                .thenReturn(List.<Object[]>of(
                        new Object[]{"site.com", "twitter", 7L},
                        new Object[]{"site.com", "facebook", 3L}));

        List<EventDTO.Response.ReferrerSummary> result = eventService.getReferrerSummary();

        assertThat(result).hasSize(2);
        EventDTO.Response.ReferrerSummary siteCom = result.stream()
                .filter(r -> r.getUsername().equals("site.com")).findFirst().orElseThrow();
        assertThat(siteCom.getTotalReferrals()).isEqualTo(10L);
        assertThat(siteCom.getByPlatform()).containsEntry("twitter", 7L).containsEntry("facebook", 3L);
    }

    @Test
    void getReferrerSummary_noData_returnsEmptyList() {
        when(eventRepository.countByReferrer()).thenReturn(List.<Object[]>of());
        when(eventRepository.countByReferrerAndPlatform()).thenReturn(List.<Object[]>of());

        assertThat(eventService.getReferrerSummary()).isEmpty();
    }

    @Test
    void getReferrerSummary_referrerWithNoPlatformBreakdown_hasEmptyMap() {
        when(eventRepository.countByReferrer())
                .thenReturn(List.<Object[]>of(new Object[]{"referrer.net", 4L}));
        when(eventRepository.countByReferrerAndPlatform())
                .thenReturn(List.<Object[]>of());

        List<EventDTO.Response.ReferrerSummary> result = eventService.getReferrerSummary();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getByPlatform()).isEmpty();
    }
}
