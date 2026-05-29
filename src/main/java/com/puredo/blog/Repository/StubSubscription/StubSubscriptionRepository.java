package com.puredo.blog.Repository.StubSubscription;

import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.StubSubscription;
import com.puredo.blog.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StubSubscriptionRepository extends JpaRepository<StubSubscription, Long> {
    List<StubSubscription> findByPost(Post post);
    boolean existsByPostAndUser(Post post, User user);
    void deleteByPost(Post post);
}
