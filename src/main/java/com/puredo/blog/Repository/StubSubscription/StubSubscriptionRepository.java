package com.puredo.blog.Repository.StubSubscription;

import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.StubSubscription;
import com.puredo.blog.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StubSubscriptionRepository extends JpaRepository<StubSubscription, Long> {
    @Query("SELECT s FROM StubSubscription s JOIN FETCH s.user WHERE s.post = :post")
    List<StubSubscription> findByPostWithUser(@Param("post") Post post);
    boolean existsByPostAndUser(Post post, User user);
    boolean existsByPostIdAndUserUsername(Long postId, String username);
    void deleteByPost(Post post);
    @Transactional
    void deleteByPostId(Long postId);
    @Transactional
    void deleteByPostIdAndUserUsername(Long postId, String username);
    long countByPostId(Long postId);
    @Query("SELECT s.post.id, COUNT(s) FROM StubSubscription s WHERE s.post.id IN :postIds GROUP BY s.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
    @Query("SELECT s.post.id FROM StubSubscription s WHERE s.post.id IN :postIds AND s.user.username = :username")
    List<Long> findSubscribedPostIds(@Param("postIds") List<Long> postIds, @Param("username") String username);
    @Query("SELECT s.user FROM StubSubscription s WHERE s.post.id = :postId")
    List<User> findSubscribersByPostId(@Param("postId") Long postId);
}
