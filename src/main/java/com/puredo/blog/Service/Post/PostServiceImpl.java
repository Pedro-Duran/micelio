package com.puredo.blog.Service.Post;

import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Event.EventRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, EventRepository eventRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<Post> createPost(PostDTO.Request.Create request) {
        Optional<User> author = userRepository.findByUsername(request.getAuthorUsername());
        if (author.isEmpty()) return Optional.empty();

        List<Long> links = new ArrayList<>(request.getLinks() != null ? request.getLinks() : List.of());
        String subject = request.getSubject() != null ? request.getSubject() : "Sem Assunto";
        resolveWikilinksInto(links, request.getWikilinks(), author.get(), subject);

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(author.get());
        post.setLinks(links);
        post.setSubject(request.getSubject());

        return Optional.of(postRepository.save(post));
    }

    @Override
    public Optional<Post> updatePost(PostDTO.Request.Update request) {
        Optional<Post> existing = postRepository.findById(request.getId());
        if (existing.isEmpty()) return Optional.empty();

        Post post = existing.get();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setSubject(request.getSubject());
        if (request.getContent() != null && !request.getContent().isBlank()) {
            post.setStub(false);
        }

        List<Long> mergedLinks = new ArrayList<>(post.getLinks() != null ? post.getLinks() : List.of());
        if (request.getLinks() != null) {
            for (Long id : request.getLinks()) {
                if (!mergedLinks.contains(id)) mergedLinks.add(id);
            }
        }
        resolveWikilinksInto(mergedLinks, request.getWikilinks(), post.getAuthor(), post.getSubject());
        post.setLinks(mergedLinks);

        return Optional.of(postRepository.save(post));
    }

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @Override
    @Transactional
    public void deletePostById(Long id) {
        eventRepository.deleteByPostId(id);

        List<Post> postsWithLink = postRepository.findPostsByLinkId(id);
        for (Post post : postsWithLink) {
            post.getLinks().remove(id);
        }
        postRepository.saveAll(postsWithLink);

        postRepository.deleteById(id);
    }

    @Override
    public Optional<Post> findPostByTitle(String title) {
        return postRepository.findPostByTitle(title);
    }

    @Override
    public Optional<Post> getPostByID(Long id) {
        return postRepository.findById(id);
    }

    @Override
    public List<String> getDistinctSubjects() {
        return postRepository.findDistinctSubjects();
    }

    @Override
    public HashMap<Long, String> findPostsBySubject(String subject) {
        List<Object[]> results = postRepository.findPostIdsAndTitlesBySubject(subject);
        HashMap<Long, String> postMap = new HashMap<>();
        for (Object[] result : results) {
            postMap.put((Long) result[0], (String) result[1]);
        }
        return postMap;
    }

    private void resolveWikilinksInto(List<Long> links, List<String> wikilinks, User author, String subject) {
        if (wikilinks == null) return;
        for (String title : wikilinks) {
            Long resolvedId = postRepository.findPostByTitle(title)
                .map(Post::getId)
                .orElseGet(() -> {
                    Post stub = new Post();
                    stub.setTitle(title);
                    stub.setContent("");
                    stub.setAuthor(author);
                    stub.setSubject(subject);
                    stub.setLinks(new ArrayList<>());
                    stub.setStub(true);
                    return postRepository.save(stub).getId();
                });
            if (!links.contains(resolvedId)) links.add(resolvedId);
        }
    }
}
