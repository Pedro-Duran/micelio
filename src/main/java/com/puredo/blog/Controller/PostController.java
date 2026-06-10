package com.puredo.blog.Controller;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Service.Like.LikeService;
import com.puredo.blog.Service.Post.PostService;
import com.puredo.blog.Service.Storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    private final PostService postService;
    private final StorageService storageService;
    private final LikeService likeService;

    @Autowired
    public PostController(PostService postService, StorageService storageService, LikeService likeService) {
        this.postService = postService;
        this.storageService = storageService;
        this.likeService = likeService;
    }

    @PostMapping("/createPost")
    public ResponseEntity<PostDTO.Response.Post> createPost(
            @RequestBody PostDTO.Request.Create request,
            Authentication authentication) {
        String username = extractUsername(authentication);
        return postService.createPost(request)
                .map(p -> ResponseEntity.ok(toResponse(p,
                        likeService.countByPost(p.getId()),
                        username != null && likeService.isLikedByUser(p.getId(), username))))
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/verPosts")
    public ResponseEntity<Page<PostDTO.Response.Post>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return pageResponse(postService.getAllPosts(PageRequest.of(page, size)), authentication);
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostDTO.Response.Post>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return pageResponse(
                postService.getFeed(authentication.getName(), PageRequest.of(page, size)),
                authentication);
    }

    @GetMapping("/explore")
    public ResponseEntity<Page<PostDTO.Response.Post>> getExplore(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return pageResponse(
                postService.getExplore(authentication.getName(), PageRequest.of(page, size)),
                authentication);
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<PostDTO.Response.Post>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return pageResponse(
                postService.getPostsByUser(authentication.getName(), PageRequest.of(page, size)),
                authentication);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PostDTO.Response.Post>> searchByUser(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return pageResponse(
                postService.getPostsByUser(username, PageRequest.of(page, size)),
                authentication);
    }

    @GetMapping("/searchByTitle")
    public ResponseEntity<Page<PostDTO.Response.Post>> searchByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return pageResponse(
                postService.searchByTitle(title, PageRequest.of(page, size)),
                authentication);
    }

    @PutMapping("/updatePost")
    public ResponseEntity<PostDTO.Response.Post> updatePost(
            @RequestBody PostDTO.Request.Update request,
            Authentication authentication) {
        String username = extractUsername(authentication);
        return postService.updatePost(request)
                .map(p -> ResponseEntity.ok(toResponse(p,
                        likeService.countByPost(p.getId()),
                        username != null && likeService.isLikedByUser(p.getId(), username))))
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
                try { storageService.deleteFile(existingUrl); } catch (IllegalArgumentException ignored) {}
            }
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{postId}/notify")
    public ResponseEntity<Void> subscribeToStub(@PathVariable Long postId, Authentication authentication) {
        boolean ok = postService.subscribeToStub(postId, authentication.getName());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId, Authentication authentication) {
        if (postService.getPostByID(postId).isEmpty()) return ResponseEntity.notFound().build();
        boolean isNew = likeService.like(postId, authentication.getName());
        return isNew ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId, Authentication authentication) {
        likeService.unlike(postId, authentication.getName());
        return ResponseEntity.noContent().build();
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

    @PutMapping("/subjects/{subject}")
    public ResponseEntity<Void> renameSubject(
            @PathVariable String subject,
            @RequestBody PostDTO.Request.RenameSubject request,
            Authentication authentication) {
        PostService.SubjectResult result = postService.renameSubject(subject, request.getNewName(), authentication.getName());
        return result == PostService.SubjectResult.OK
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @DeleteMapping("/subjects/{subject}")
    public ResponseEntity<Void> deleteSubject(
            @PathVariable String subject,
            Authentication authentication) {
        PostService.SubjectResult result = postService.deleteSubject(subject, authentication.getName());
        return result == PostService.SubjectResult.OK
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // ---- helpers ----

    private ResponseEntity<Page<PostDTO.Response.Post>> pageResponse(Page<Post> posts, Authentication auth) {
        List<Long> ids = posts.stream().map(Post::getId).toList();
        String username = extractUsername(auth);
        Map<Long, Long> counts = likeService.countByPosts(ids);
        Set<Long> liked = username != null ? likeService.likedPostIds(ids, username) : Set.of();
        return ResponseEntity.ok(posts.map(p ->
                toResponse(p, counts.getOrDefault(p.getId(), 0L), liked.contains(p.getId()))));
    }

    private PostDTO.Response.Post toResponse(Post post, long likeCount, boolean likedByMe) {
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
                post.getSubjects(),
                post.isStub(),
                post.getCoverImageUrl(),
                likeCount,
                likedByMe
        );
    }

    private String extractUsername(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) return null;
        return auth.getName();
    }
}