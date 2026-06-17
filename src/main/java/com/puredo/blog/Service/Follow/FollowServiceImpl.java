package com.puredo.blog.Service.Follow;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.Follow;
import com.puredo.blog.Entity.NotificationType;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Follow.FollowRepository;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.Notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository,
                             NotificationService notificationService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void follow(String followerUsername, String followedUsername) {
        if (followerUsername.equals(followedUsername)) {
            throw new IllegalArgumentException("Usuário não pode seguir a si mesmo");
        }
        if (followRepository.existsByFollowerUsernameAndFollowedUsername(followerUsername, followedUsername)) {
            return; // idempotente
        }
        User follower = userRepository.findByUsername(followerUsername)
            .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado: " + followerUsername));
        User followed = userRepository.findByUsername(followedUsername)
            .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado: " + followedUsername));

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowed(followed);
        followRepository.save(follow);

        notificationService.notify(followedUsername, NotificationType.FOLLOW, followerUsername, follower.getAvatarUrl(), null, null);
    }

    @Override
    public void unfollow(String followerUsername, String followedUsername) {
        followRepository.findByFollowerUsernameAndFollowedUsername(followerUsername, followedUsername)
            .ifPresent(followRepository::delete); // idempotente — sem erro se não existir
    }

    @Override
    public List<UserDTO.Response.UsuarioPublico> getFollowing(String username) {
        return followRepository.findByFollowerUsername(username).stream()
            .map(f -> new UserDTO.Response.UsuarioPublico(f.getFollowed().getId(), f.getFollowed().getUsername(), null, f.getFollowed().getAvatarUrl()))
            .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO.Response.UsuarioPublico> getFollowers(String username) {
        return followRepository.findByFollowedUsername(username).stream()
            .map(f -> new UserDTO.Response.UsuarioPublico(f.getFollower().getId(), f.getFollower().getUsername(), null, f.getFollower().getAvatarUrl()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isFollowing(String followerUsername, String followedUsername) {
        return followRepository.existsByFollowerUsernameAndFollowedUsername(followerUsername, followedUsername);
    }
}
