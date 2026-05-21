package com.puredo.blog.Controller;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.Event;
import com.puredo.blog.Event.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/register")
    public ResponseEntity<EventDTO.Response.EventSaved> registerEvent(
        @RequestBody EventDTO.Request.Register request
    ) {
        Event event = new Event();
        event.setPostId(request.getPostId());
        event.setEventType(request.getEventType());
        event.setSessionId(request.getSessionId());
        event.setDuration(request.getDuration());
        event.setUtmSource(request.getUtmSource());
        event.setReferredBy(request.getReferredBy());

        Event saved = eventService.registerEvent(event);

        return ResponseEntity.ok(new EventDTO.Response.EventSaved(
            saved.getId(),
            saved.getPostId(),
            saved.getEventType(),
            saved.getSessionId(),
            saved.getTimestamp().toString(),
            saved.getDuration(),
            saved.getUtmSource(),
            saved.getReferredBy()
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<EventDTO.Response.Summary>> getSummary() {
        return ResponseEntity.ok(eventService.getSummary());
    }

    @GetMapping("/byPost")
    public ResponseEntity<List<EventDTO.Response.EventSaved>> getEventsByPost(@RequestParam Long postId) {
        List<EventDTO.Response.EventSaved> response = eventService.getEventsByPost(postId).stream()
            .map(e -> new EventDTO.Response.EventSaved(
                e.getId(),
                e.getPostId(),
                e.getEventType(),
                e.getSessionId(),
                e.getTimestamp().toString(),
                e.getDuration(),
                e.getUtmSource(),
                e.getReferredBy()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/referrers")
    public ResponseEntity<List<EventDTO.Response.ReferrerSummary>> getReferrerSummary() {
        return ResponseEntity.ok(eventService.getReferrerSummary());
    }
}
