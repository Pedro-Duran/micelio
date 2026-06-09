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

    @Query(value = """
            SELECT e.username, COUNT(*) as total
            FROM events e
            WHERE e.event_type = 'STUB_SUBSCRIBE' AND e.username IS NOT NULL
            GROUP BY e.username
            ORDER BY total DESC
            LIMIT 20
            """, nativeQuery = true)
    List<Object[]> topStubSubscribers();

    @Query(value = """
            SELECT u.username, COUNT(e.id) AS sub_count
            FROM events e
            JOIN posts p ON e.post_id = p.id
            JOIN users u ON p.author_id = u.id
            WHERE e.event_type = 'STUB_SUBSCRIBE'
            GROUP BY u.username
            ORDER BY sub_count DESC
            LIMIT 20
            """, nativeQuery = true)
    List<Object[]> topStubSubscribeAuthors();

    @Query(value = """
            SELECT e.post_id, p.title, MAX(e.cover_image_url) AS cover_image_url,
                   MAX(e.subject) AS subject, COUNT(e.id) AS click_count
            FROM events e
            JOIN posts p ON e.post_id = p.id
            WHERE e.event_type = 'COVER_CLICK'
            GROUP BY e.post_id, p.title
            ORDER BY click_count DESC
            """, nativeQuery = true)
    List<Object[]> coverConversionStats();

    @Query(value = """
            SELECT e.username, COUNT(*) AS share_count
            FROM events e
            WHERE e.event_type = 'SHARE' AND e.username IS NOT NULL
            GROUP BY e.username
            ORDER BY share_count DESC
            """, nativeQuery = true)
    List<Object[]> countSharesByUser();

    @Query(value = """
            SELECT e.username, COUNT(*) AS share_count
            FROM events e
            WHERE e.event_type = 'SHARE' AND e.username IS NOT NULL AND e.subject = :subject
            GROUP BY e.username
            ORDER BY share_count DESC
            """, nativeQuery = true)
    List<Object[]> countSharesByUserAndSubject(@Param("subject") String subject);
}
