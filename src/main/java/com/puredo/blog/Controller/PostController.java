package com.puredo.blog.Controller;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.User.UserService;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Post.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @Autowired
    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @PostMapping("/createPost")
    public ResponseEntity<PostDTO.Response.Post> createPost(@RequestBody PostDTO.Request.Create request) {
        Optional<User> author = userService.findByUserName(request.getAuthorUsername());
        if (author.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        List<Long> resolvedLinks = new ArrayList<>(
            request.getLinks() != null ? request.getLinks() : List.of()
        );

        if (request.getWikilinks() != null) {
            for (String wikilinkTitle : request.getWikilinks()) {
                Optional<Post> existing = postService.findPostByTitle(wikilinkTitle);
                if (existing.isPresent()) {
                    resolvedLinks.add(existing.get().getId());
                } else {
                    Post stub = new Post();
                    stub.setTitle(wikilinkTitle);
                    stub.setContent("");
                    stub.setAuthor(author.get());
                    stub.setSubject(request.getSubject() != null ? request.getSubject() : "Sem Assunto");
                    stub.setLinks(new ArrayList<>());
                    stub.setStub(true);
                    Post savedStub = postService.createPost(stub);
                    resolvedLinks.add(savedStub.getId());
                }
            }
        }

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(author.get());
        post.setLinks(resolvedLinks);
        post.setSubject(request.getSubject());

        Post createdPost = postService.createPost(post);

        UserDTO.Response.UsuarioPublico authorDTO = convertToUsuarioPublico(createdPost.getAuthor());

        PostDTO.Response.Post response = new PostDTO.Response.Post(
            createdPost.getId(),
            createdPost.getTitle(),
            createdPost.getContent(),
            authorDTO,
            createdPost.getCreatedAt().toString(),
            createdPost.getLinks(),
            createdPost.getSubject(),
            createdPost.isStub()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verPosts")
    public ResponseEntity<List<PostDTO.Response.Post>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();

        List<PostDTO.Response.Post> responses = posts.stream()
            .map(post -> new PostDTO.Response.Post(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                convertToUsuarioPublico(post.getAuthor()),
                post.getCreatedAt().toString(),
                post.getLinks(),
                post.getSubject(),
                post.isStub()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/updatePost")
    public ResponseEntity<PostDTO.Response.Post> updatePost(@RequestBody PostDTO.Request.Update request) {
        Optional<Post> existingPost = postService.getPostByID(request.getId());
        if (existingPost.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Post post = existingPost.get();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setSubject(request.getSubject());

        if (request.getContent() != null && !request.getContent().isBlank()) {
            post.setStub(false);
        }

        // Union: existentes + links explícitos + wikilinks resolvidos (sem duplicatas)
        List<Long> mergedLinks = new ArrayList<>(post.getLinks() != null ? post.getLinks() : List.of());

        if (request.getLinks() != null) {
            for (Long linkId : request.getLinks()) {
                if (!mergedLinks.contains(linkId)) mergedLinks.add(linkId);
            }
        }

        if (request.getWikilinks() != null) {
            for (String wikilinkTitle : request.getWikilinks()) {
                Optional<Post> linked = postService.findPostByTitle(wikilinkTitle);
                Long resolvedId;
                if (linked.isPresent()) {
                    resolvedId = linked.get().getId();
                } else {
                    Post stub = new Post();
                    stub.setTitle(wikilinkTitle);
                    stub.setContent("");
                    stub.setAuthor(post.getAuthor());
                    stub.setSubject(post.getSubject());
                    stub.setLinks(new ArrayList<>());
                    stub.setStub(true);
                    resolvedId = postService.createPost(stub).getId();
                }
                if (!mergedLinks.contains(resolvedId)) mergedLinks.add(resolvedId);
            }
        }

        post.setLinks(mergedLinks);

        Post updatedPost = postService.updatePost(post);
        UserDTO.Response.UsuarioPublico usuarioPublico = convertToUsuarioPublico(updatedPost.getAuthor());

        PostDTO.Response.Post response = new PostDTO.Response.Post(
            updatedPost.getId(),
            updatedPost.getTitle(),
            updatedPost.getContent(),
            usuarioPublico,
            updatedPost.getCreatedAt().toString(),
            updatedPost.getLinks(),
            updatedPost.getSubject(),
            updatedPost.isStub()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deletePost")
    public ResponseEntity<Void> deletePost(@RequestParam Long id) {
        Optional<Post> existingPost = postService.getPostByID(id);
        if (existingPost.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        postService.deletePostById(existingPost.get().getId());
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

    private UserDTO.Response.UsuarioPublico convertToUsuarioPublico(User user) {
        return new UserDTO.Response.UsuarioPublico(user.getId(), user.getUsername(), null);
    }
}
