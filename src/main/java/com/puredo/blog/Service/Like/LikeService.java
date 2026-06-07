package com.puredo.blog.Service.Like;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LikeService {
    boolean like(Long postId, String username);
    void unlike(Long postId, String username);
    long countByPost(Long postId);
    boolean isLikedByUser(Long postId, String username);
    Map<Long, Long> countByPosts(List<Long> postIds);
    Set<Long> likedPostIds(List<Long> postIds, String username);
}