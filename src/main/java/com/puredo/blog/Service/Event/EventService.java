package com.puredo.blog.Service.Event;

import com.puredo.blog.DTO.EventDTO;
import com.puredo.blog.Entity.EventType;

import java.util.List;

public interface EventService {
    EventDTO.Response.EventSaved registerEvent(EventDTO.Request.Register request);
    void registerInternalEvent(Long postId, EventType type, String username);
    List<EventDTO.Response.EventSaved> getEventsByPost(Long postId);
    List<EventDTO.Response.Summary> getSummary(String username, boolean isSuperuser);
    List<EventDTO.Response.ReferrerSummary> getReferrerSummary();
    EventDTO.Response.SubscriptionAnalytics getSubscriptionAnalytics();
    List<EventDTO.Response.CoverConversionEntry> getCoverConversionAnalytics();
    List<EventDTO.Response.ShareLeaderboardEntry> getShareLeaderboard(String subject);
}
