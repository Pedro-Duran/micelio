package com.puredo.blog.Controller;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Service.Post.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/createPost")
    public ResponseEntity<PostDTO.Response.Post> createPost(@RequestBody PostDTO.Request.Create request) {
        return postService.createPost(request)
            .map(p -> ResponseEntity.ok(toResponse(p)))
            .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/verPosts")
    public ResponseEntity<List<PostDTO.Response.Post>> getAllPosts() {
        List<PostDTO.Response.Post> responses = postService.getAllPosts().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/updatePost")
    public ResponseEntity<PostDTO.Response.Post> updatePost(@RequestBody PostDTO.Request.Update request) {
        return postService.updatePost(request)
            .map(p -> ResponseEntity.ok(toResponse(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/deletePost")
    public ResponseEntity<Void> deletePost(@RequestParam Long id) {
        if (postService.getPostByID(id).isEmpty()) return ResponseEntity.notFound().build();
        postService.deletePostById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subjects")
    public List<String> getSubjects() {
        return postService.getDistinctSubjects();
    }

    @GetMapping("/postsIdForThisSubject")
    public HashMap<Long, String> getPostsIdsForThisSubject(@RequestParam String subject) {
        return postService.findPostsBySubject(subject);
    }

    private PostDTO.Response.Post toResponse(Post post) {
        UserDTO.Response.UsuarioPublico author = new UserDTO.Response.UsuarioPublico(
            post.getAuthor().getId(), post.getAuthor().getUsername(), null
        );
        return new PostDTO.Response.Post(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            author,
            post.getCreatedAt().toString(),
            post.getLinks(),
            post.getSubject(),
            post.isStub()
        );
    }
}
