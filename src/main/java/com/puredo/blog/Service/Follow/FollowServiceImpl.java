package com.puredo.blog.Service.Follow;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.Follow;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Follow.FollowRepository;
import com.puredo.blog.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Autowired
    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
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
    }

    @Override
    public void unfollow(String followerUsername, String followedUsername) {
        followRepository.findByFollowerUsernameAndFollowedUsername(followerUsername, followedUsername)
            .ifPresent(followRepository::delete); // idempotente — sem erro se não existir
    }

    @Override
    public List<UserDTO.Response.UsuarioPublico> getFollowing(String username) {
        return followRepository.findByFollowerUsername(username).stream()
            .map(f -> new UserDTO.Response.UsuarioPublico(f.getFollowed().getId(), f.getFollowed().getUsername(), null))
            .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO.Response.UsuarioPublico> getFollowers(String username) {
        return followRepository.findByFollowedUsername(username).stream()
            .map(f -> new UserDTO.Response.UsuarioPublico(f.getFollower().getId(), f.getFollower().getUsername(), null))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isFollowing(String followerUsername, String followedUsername) {
        return followRepository.existsByFollowerUsernameAndFollowedUsername(followerUsername, followedUsername);
    }
}
