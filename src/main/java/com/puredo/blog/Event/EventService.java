package com.puredo.blog.Event;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.Event;

import java.util.List;

public interface EventService {
    Event registerEvent(Event event);
    List<Event> getEventsByPost(Long postId);
    List<EventDTO.Response.Summary> getSummary(String username, boolean isSuperuser);
    List<EventDTO.Response.ReferrerSummary> getReferrerSummary();
}
