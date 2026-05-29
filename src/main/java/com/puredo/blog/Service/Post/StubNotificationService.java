package com.puredo.blog.Service.Post;

import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.StubSubscription;
import com.puredo.blog.Entity.User;
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
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    public StubNotificationService(StubSubscriptionRepository subscriptionRepository, EmailService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.emailService = emailService;
    }

    @Async
    @Transactional
    public void notifyAndCleanup(Post post) {
        List<StubSubscription> subscriptions = subscriptionRepository.findByPost(post);
        String postLink = frontendUrl + "/post/" + post.getId();
        String authorUsername = post.getAuthor().getUsername();

        for (StubSubscription sub : subscriptions) {
            User subscriber = sub.getUser();
            if (subscriber.getEmail() == null) continue;
            emailService.sendStubPublished(
                subscriber.getEmail(),
                subscriber.getUsername(),
                post.getTitle(),
                authorUsername,
                postLink
            );
        }
        subscriptionRepository.deleteByPost(post);
    }
}
