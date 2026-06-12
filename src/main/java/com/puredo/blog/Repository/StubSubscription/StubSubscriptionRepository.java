package com.puredo.blog.Repository.StubSubscription;

import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.StubSubscription;
import com.puredo.blog.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StubSubscriptionRepository extends JpaRepository<StubSubscription, Long> {
    @Query("SELECT s FROM StubSubscription s JOIN FETCH s.user WHERE s.post = :post")
    List<StubSubscription> findByPostWithUser(@Param("post") Post post);
    boolean existsByPostAndUser(Post post, User user);
    void deleteByPost(Post post);
}
