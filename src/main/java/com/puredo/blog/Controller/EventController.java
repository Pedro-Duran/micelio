package com.puredo.blog.Controller;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Service.Event.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<EventDTO.Response.EventSaved> registerEvent(@RequestBody EventDTO.Request.Register request) {
        return ResponseEntity.ok(eventService.registerEvent(request));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<EventDTO.Response.Summary>> getSummary(Authentication authentication) {
        boolean isSuperuser = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERUSER"));
        return ResponseEntity.ok(eventService.getSummary(authentication.getName(), isSuperuser));
    }

    @GetMapping("/byPost")
    public ResponseEntity<List<EventDTO.Response.EventSaved>> getEventsByPost(@RequestParam Long postId) {
        return ResponseEntity.ok(eventService.getEventsByPost(postId));
    }

    @GetMapping("/referrers")
    public ResponseEntity<List<EventDTO.Response.ReferrerSummary>> getReferrerSummary() {
        return ResponseEntity.ok(eventService.getReferrerSummary());
    }
}
