package com.puredo.blog.Controller;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Service.Post.PostService;
import com.puredo.blog.Service.Storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    private final PostService postService;
    private final StorageService storageService;

    @Autowired
    public PostController(PostService postService, StorageService storageService) {
        this.postService = postService;
        this.storageService = storageService;
    }

    @PostMapping("/createPost")
    public ResponseEntity<PostDTO.Response.Post> createPost(@RequestBody PostDTO.Request.Create request) {
        return postService.createPost(request)
            .map(p -> ResponseEntity.ok(toResponse(p)))
            .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/verPosts")
    public ResponseEntity<Page<PostDTO.Response.Post>> getAllPosts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(postService.getAllPosts(PageRequest.of(page, size)).map(this::toResponse));
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostDTO.Response.Post>> getFeed(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            postService.getFeed(authentication.getName(), PageRequest.of(page, size)).map(this::toResponse)
        );
    }

    @GetMapping("/explore")
    public ResponseEntity<Page<PostDTO.Response.Post>> getExplore(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            postService.getExplore(authentication.getName(), PageRequest.of(page, size)).map(this::toResponse)
        );
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<PostDTO.Response.Post>> getMyPosts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            postService.getPostsByUser(authentication.getName(), PageRequest.of(page, size)).map(this::toResponse)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PostDTO.Response.Post>> searchByUser(
        @RequestParam String username,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            postService.getPostsByUser(username, PageRequest.of(page, size)).map(this::toResponse)
        );
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

    @PostMapping("/{postId}/cover")
    public ResponseEntity<?> uploadCover(@PathVariable Long postId, @RequestParam("file") MultipartFile file) {
        try {
            String url = storageService.uploadCover(file);
            return postService.updateCover(postId, url)
                .map(coverUrl -> ResponseEntity.ok((Object) new PostDTO.Response.CoverResponse(coverUrl)))
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{postId}/cover")
    public ResponseEntity<?> deleteCover(@PathVariable Long postId) {
        return postService.removeCover(postId).map(existingUrl -> {
            if (existingUrl != null) {
                try {
                    storageService.deleteFile(existingUrl);
                } catch (IllegalArgumentException ignored) {}
            }
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{postId}/notify")
    public ResponseEntity<Void> subscribeToStub(@PathVariable Long postId, Authentication authentication) {
        boolean ok = postService.subscribeToStub(postId, authentication.getName());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @PostMapping("/images")
    public ResponseEntity<?> uploadPostImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = storageService.uploadPostImage(file);
            return ResponseEntity.ok(new PostDTO.Response.ImageUploadResponse(url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/images")
    public ResponseEntity<?> deletePostImage(@RequestParam String url) {
        try {
            storageService.deleteFile(url);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
            post.getAuthor().getId(), post.getAuthor().getUsername(), null, post.getAuthor().getAvatarUrl()
        );
        return new PostDTO.Response.Post(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            author,
            post.getCreatedAt().toString(),
            post.getLinks(),
            post.getSubject(),
            post.isStub(),
            post.getCoverImageUrl()
        );
    }
}
