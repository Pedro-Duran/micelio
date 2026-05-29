package com.puredo.blog.Service.Post;

import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface PostService {
    Optional<Post> createPost(PostDTO.Request.Create request);
    Optional<Post> updatePost(PostDTO.Request.Update request);
    Optional<String> updateCover(Long postId, String coverImageUrl);
    Optional<String> removeCover(Long postId);
    List<Post> getAllPosts();
    Page<Post> getAllPosts(Pageable pageable);
    Page<Post> getFeed(String username, Pageable pageable);
    Page<Post> getExplore(String username, Pageable pageable);
    Page<Post> getPostsByUser(String username, Pageable pageable);
    void deletePostById(Long id);
    Optional<Post> findPostByTitle(String title);
    Optional<Post> getPostByID(Long id);
    List<String> getDistinctSubjects();
    HashMap<Long, String> findPostsBySubject(String subject);
    boolean subscribeToStub(Long postId, String subscriberUsername);
}
