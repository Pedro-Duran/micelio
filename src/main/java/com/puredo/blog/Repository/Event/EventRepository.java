package com.puredo.blog.Repository.Event;

import com.puredo.blog.Entity.Event;
import com.puredo.blog.Entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("SELECT e.postId, COUNT(e) FROM Event e WHERE e.eventType = :type GROUP BY e.postId")
    List<Object[]> countByPostAndType(@Param("type") EventType type);

    @Query("SELECT e.postId, AVG(e.duration) FROM Event e WHERE e.eventType = :type GROUP BY e.postId")
    List<Object[]> avgDurationByPostAndType(@Param("type") EventType type);

    @Query("SELECT e.referredBy, COUNT(e) FROM Event e WHERE e.referredBy IS NOT NULL GROUP BY e.referredBy")
    List<Object[]> countByReferrer();

    @Query("SELECT e.referredBy, e.utmSource, COUNT(e) FROM Event e WHERE e.referredBy IS NOT NULL AND e.utmSource IS NOT NULL GROUP BY e.referredBy, e.utmSource")
    List<Object[]> countByReferrerAndPlatform();
}
