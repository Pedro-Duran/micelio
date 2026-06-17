package com.puredo.blog.Service.Like;

import com.puredo.blog.Entity.Like;
import com.puredo.blog.Entity.NotificationType;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Like.LikeRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.Notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public LikeServiceImpl(LikeRepository likeRepository, PostRepository postRepository,
                           UserRepository userRepository, NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public boolean like(Long postId, String username) {
        if (likeRepository.existsByPostIdAndUserUsername(postId, username)) return false;

        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findByUsername(username).orElseThrow();

        Like like = new Like();
        like.setPost(post);
        like.setUser(user);
        likeRepository.save(like);

        notificationService.notify(
                post.getAuthor().getUsername(),
                NotificationType.LIKE,
                username,
                user.getAvatarUrl(),
                postId,
                post.getTitle()
        );
        return true;
    }

    @Override
    public void unlike(Long postId, String username) {
        likeRepository.deleteByPostIdAndUserUsername(postId, username);
    }

    @Override
    public long countByPost(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    @Override
    public boolean isLikedByUser(Long postId, String username) {
        return likeRepository.existsByPostIdAndUserUsername(postId, username);
    }

    @Override
    public Map<Long, Long> countByPosts(List<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return likeRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    @Override
    public Set<Long> likedPostIds(List<Long> postIds, String username) {
        if (postIds.isEmpty() || username == null) return Set.of();
        return new HashSet<>(likeRepository.findLikedPostIds(postIds, username));
    }
}