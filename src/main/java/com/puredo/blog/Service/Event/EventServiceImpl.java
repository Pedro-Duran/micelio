package com.puredo.blog.Service.Event;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.Event;
import com.puredo.blog.Entity.EventType;
import com.puredo.blog.Repository.Event.EventRepository;
import com.puredo.blog.Repository.Like.LikeRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;

    @Autowired
    public EventServiceImpl(EventRepository eventRepository, PostRepository postRepository,
                            LikeRepository likeRepository) {
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
    }

    @Override
    public EventDTO.Response.EventSaved registerEvent(EventDTO.Request.Register request) {
        Event event = new Event();
        event.setPostId(request.getPostId());
        event.setEventType(request.getEventType());
        event.setSessionId(request.getSessionId());
        event.setDuration(request.getDuration());
        event.setUtmSource(request.getUtmSource());
        event.setReferredBy(request.getReferredBy());
        event.setSubject(request.getSubject());
        event.setCoverImageUrl(request.getCoverImageUrl());
        event.setUsername(request.getUsername());

        Event saved = eventRepository.save(event);
        return toEventSaved(saved);
    }

    @Override
    public void registerInternalEvent(Long postId, EventType type, String username) {
        Event event = new Event();
        event.setPostId(postId);
        event.setEventType(type);
        event.setSessionId("system");
        event.setUsername(username);
        eventRepository.save(event);
    }

    @Override
    public List<EventDTO.Response.EventSaved> getEventsByPost(Long postId) {
        return eventRepository.findByPostId(postId).stream()
                .map(this::toEventSaved)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDTO.Response.Summary> getSummary(String username, boolean isSuperuser) {
        Map<Long, Long> viewCounts = toMap(eventRepository.countByPostAndType(EventType.VIEW));
        Map<Long, Long> clickCounts = toMap(eventRepository.countByPostAndType(EventType.CLICK_NODE));
        Map<Long, Long> commentCounts = toMap(eventRepository.countByPostAndType(EventType.COMMENT));
        Map<Long, Long> shareCounts = toMap(eventRepository.countByPostAndType(EventType.SHARE));
        Map<Long, Double> avgDurations = toDoubleMap(eventRepository.avgDurationByPostAndType(EventType.VIEW));

        Set<Long> postIds = new HashSet<>();
        postIds.addAll(viewCounts.keySet());
        postIds.addAll(clickCounts.keySet());
        postIds.addAll(commentCounts.keySet());

        if (!isSuperuser) {
            Set<Long> authorPostIds = new HashSet<>(postRepository.findPostIdsByAuthorUsername(username));
            postIds.retainAll(authorPostIds);
        }

        Map<Long, Long> likeCounts = postIds.isEmpty() ? Map.of()
                : likeRepository.countByPostIds(new ArrayList<>(postIds)).stream()
                        .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        List<EventDTO.Response.Summary> summaries = new ArrayList<>();
        for (Long postId : postIds) {
            String title = postRepository.findById(postId)
                    .map(p -> p.getTitle())
                    .orElse("Post removido");

            summaries.add(new EventDTO.Response.Summary(
                    postId,
                    title,
                    viewCounts.getOrDefault(postId, 0L),
                    avgDurations.getOrDefault(postId, 0.0),
                    clickCounts.getOrDefault(postId, 0L),
                    commentCounts.getOrDefault(postId, 0L),
                    likeCounts.getOrDefault(postId, 0L),
                    shareCounts.getOrDefault(postId, 0L)
            ));
        }

        return summaries;
    }

    @Override
    public List<EventDTO.Response.ReferrerSummary> getReferrerSummary() {
        Map<String, Long> totals = new HashMap<>();
        for (Object[] row : eventRepository.countByReferrer()) {
            totals.put((String) row[0], (Long) row[1]);
        }

        Map<String, Map<String, Long>> byPlatform = new HashMap<>();
        for (Object[] row : eventRepository.countByReferrerAndPlatform()) {
            String referrer = (String) row[0];
            String platform = (String) row[1];
            long count = (Long) row[2];
            byPlatform.computeIfAbsent(referrer, k -> new HashMap<>()).put(platform, count);
        }

        List<EventDTO.Response.ReferrerSummary> result = new ArrayList<>();
        for (String referrer : totals.keySet()) {
            result.add(new EventDTO.Response.ReferrerSummary(
                    referrer,
                    totals.get(referrer),
                    byPlatform.getOrDefault(referrer, Collections.emptyMap())
            ));
        }

        return result;
    }

    @Override
    public EventDTO.Response.SubscriptionAnalytics getSubscriptionAnalytics() {
        List<EventDTO.Response.TopSubscriber> subscribers = eventRepository.topStubSubscribers().stream()
                .map(r -> new EventDTO.Response.TopSubscriber(
                        (String) r[0],
                        ((Number) r[1]).longValue()))
                .toList();

        List<EventDTO.Response.TopAuthor> authors = eventRepository.topStubSubscribeAuthors().stream()
                .map(r -> new EventDTO.Response.TopAuthor(
                        (String) r[0],
                        ((Number) r[1]).longValue()))
                .toList();

        return new EventDTO.Response.SubscriptionAnalytics(subscribers, authors);
    }

    @Override
    public List<EventDTO.Response.CoverConversionEntry> getCoverConversionAnalytics() {
        Map<Long, Long> viewCounts = toMap(eventRepository.countByPostAndType(EventType.VIEW));
        return eventRepository.coverConversionStats().stream()
                .map(r -> new EventDTO.Response.CoverConversionEntry(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        (String) r[3],
                        (String) r[2],
                        ((Number) r[4]).longValue(),
                        viewCounts.getOrDefault(((Number) r[0]).longValue(), 0L)))
                .toList();
    }

    @Override
    public List<EventDTO.Response.ShareLeaderboardEntry> getShareLeaderboard(String subject) {
        List<Object[]> rows = subject != null
                ? eventRepository.countSharesByUserAndSubject(subject)
                : eventRepository.countSharesByUser();
        return rows.stream()
                .map(r -> new EventDTO.Response.ShareLeaderboardEntry(
                        (String) r[0],
                        ((Number) r[1]).longValue()))
                .toList();
    }

    private EventDTO.Response.EventSaved toEventSaved(Event e) {
        return new EventDTO.Response.EventSaved(
                e.getId(),
                e.getPostId(),
                e.getEventType(),
                e.getSessionId(),
                e.getTimestamp().toString(),
                e.getDuration(),
                e.getUtmSource(),
                e.getReferredBy(),
                e.getUsername(),
                e.getSubject(),
                e.getCoverImageUrl()
        );
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) map.put((Long) row[0], (Long) row[1]);
        return map;
    }

    private Map<Long, Double> toDoubleMap(List<Object[]> rows) {
        Map<Long, Double> map = new HashMap<>();
        for (Object[] row : rows)
            map.put((Long) row[0], row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        return map;
    }
}
