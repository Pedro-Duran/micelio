package com.puredo.blog.Service.Post;

import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface PostService {
    Optional<Post> createPost(PostDTO.Request.Create request);
    Optional<Post> updatePost(PostDTO.Request.Update request);
    List<Post> getAllPosts();
    void deletePostById(Long id);
    Optional<Post> findPostByTitle(String title);
    Optional<Post> getPostByID(Long id);
    List<String> getDistinctSubjects();
    HashMap<Long, String> findPostsBySubject(String subject);
}
