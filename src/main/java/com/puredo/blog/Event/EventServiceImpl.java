package com.puredo.blog.Event;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.Event;
import com.puredo.blog.Entity.EventType;
import com.puredo.blog.Post.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
    public Event registerEvent(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public List<Event> getEventsByPost(Long postId) {
        return eventRepository.findByPostId(postId);
    }

    @Override
    public List<EventDTO.Response.Summary> getSummary() {
        Map<Long, Long> viewCounts = toMap(eventRepository.countByPostAndType(EventType.VIEW));
        Map<Long, Long> clickCounts = toMap(eventRepository.countByPostAndType(EventType.CLICK_NODE));
        Map<Long, Double> avgDurations = toDoubleMap(
            eventRepository.avgDurationByPostAndType(EventType.VIEW)
        );

        Set<Long> postIds = new HashSet<>();
        postIds.addAll(viewCounts.keySet());
        postIds.addAll(clickCounts.keySet());

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

    private Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<Long, Double> toDoubleMap(List<Object[]> rows) {
        Map<Long, Double> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        }
        return map;
    }
}
