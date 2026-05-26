package com.puredo.blog.Service.Event;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.Event;
import com.puredo.blog.Entity.EventType;
import com.puredo.blog.Repository.Event.EventRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final PostRepository postRepository;

    @Autowired
    public EventServiceImpl(EventRepository eventRepository, PostRepository postRepository) {
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
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

        Event saved = eventRepository.save(event);
        return toEventSaved(saved);
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
        Map<Long, Double> avgDurations = toDoubleMap(eventRepository.avgDurationByPostAndType(EventType.VIEW));

        Set<Long> postIds = new HashSet<>();
        postIds.addAll(viewCounts.keySet());
        postIds.addAll(clickCounts.keySet());

        if (!isSuperuser) {
            Set<Long> authorPostIds = new HashSet<>(postRepository.findPostIdsByAuthorUsername(username));
            postIds.retainAll(authorPostIds);
        }

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
                clickCounts.getOrDefault(postId, 0L)
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

    private EventDTO.Response.EventSaved toEventSaved(Event e) {
        return new EventDTO.Response.EventSaved(
            e.getId(),
            e.getPostId(),
            e.getEventType(),
            e.getSessionId(),
            e.getTimestamp().toString(),
            e.getDuration(),
            e.getUtmSource(),
            e.getReferredBy()
        );
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) map.put((Long) row[0], (Long) row[1]);
        return map;
    }

    private Map<Long, Double> toDoubleMap(List<Object[]> rows) {
        Map<Long, Double> map = new HashMap<>();
        for (Object[] row : rows) map.put((Long) row[0], row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        return map;
    }
}
