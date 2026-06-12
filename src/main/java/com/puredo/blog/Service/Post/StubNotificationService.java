package com.puredo.blog.Service.Post;

import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.StubSubscription;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.StubSubscription.StubSubscriptionRepository;
import com.puredo.blog.Service.Email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StubNotificationService {

    private final StubSubscriptionRepository subscriptionRepository;
    private final PostRepository postRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    public StubNotificationService(StubSubscriptionRepository subscriptionRepository,
                                   PostRepository postRepository,
                                   EmailService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.postRepository = postRepository;
        this.emailService = emailService;
    }

    @Async
    @Transactional
    public void notifyAndCleanup(Post post) {
        Post managedPost = postRepository.findByIdWithAuthor(post.getId()).orElseThrow();
        List<StubSubscription> subscriptions = subscriptionRepository.findByPostWithUser(managedPost);
        String postLink = frontendUrl + "/post/" + managedPost.getId();
        String authorUsername = managedPost.getAuthor().getUsername();

        for (StubSubscription sub : subscriptions) {
            User subscriber = sub.getUser();
            if (subscriber.getEmail() == null) continue;
            emailService.sendStubPublished(
                subscriber.getEmail(),
                subscriber.getUsername(),
                managedPost.getTitle(),
                authorUsername,
                postLink
            );
        }
        subscriptionRepository.deleteByPost(managedPost);
    }
}
