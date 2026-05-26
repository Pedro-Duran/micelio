package com.puredo.blog.Service.Event;

import com.puredo.blog.DTO.EventDTO;

import java.util.List;

public interface EventService {
    EventDTO.Response.EventSaved registerEvent(EventDTO.Request.Register request);
    List<EventDTO.Response.EventSaved> getEventsByPost(Long postId);
    List<EventDTO.Response.Summary> getSummary(String username, boolean isSuperuser);
    List<EventDTO.Response.ReferrerSummary> getReferrerSummary();
}
