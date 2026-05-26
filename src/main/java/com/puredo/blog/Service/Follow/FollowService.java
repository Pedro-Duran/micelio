package com.puredo.blog.Service.Follow;

import com.puredo.blog.DTO.UserDTO;

import java.util.List;

public interface FollowService {
    void follow(String followerUsername, String followedUsername);
    void unfollow(String followerUsername, String followedUsername);
    List<UserDTO.Response.UsuarioPublico> getFollowing(String username);
    List<UserDTO.Response.UsuarioPublico> getFollowers(String username);
    boolean isFollowing(String followerUsername, String followedUsername);
}
