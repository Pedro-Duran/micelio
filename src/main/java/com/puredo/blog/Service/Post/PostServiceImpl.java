package com.puredo.blog.Service.Post;

import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Event;
import com.puredo.blog.Entity.EventType;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.StubSubscription;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Event.EventRepository;
import com.puredo.blog.Repository.Follow.FollowRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.StubSubscription.StubSubscriptionRepository;
import com.puredo.blog.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final StubSubscriptionRepository subscriptionRepository;
    private final StubNotificationService stubNotificationService;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, EventRepository eventRepository,
                           UserRepository userRepository, FollowRepository followRepository,
                           StubSubscriptionRepository subscriptionRepository,
                           StubNotificationService stubNotificationService) {
        this.postRepository = postRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stubNotificationService = stubNotificationService;
    }

    @Override
    public Optional<Post> createPost(PostDTO.Request.Create request) {
        Optional<User> author = userRepository.findByUsername(request.getAuthorUsername());
        if (author.isEmpty()) return Optional.empty();

        List<Long> links = new ArrayList<>(request.getLinks() != null ? request.getLinks() : List.of());
        List<String> subjects = request.getSubjects() != null ? new ArrayList<>(request.getSubjects()) : new ArrayList<>();
        resolveWikilinksInto(links, request.getWikilinks(), author.get(), subjects);

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(author.get());
        post.setLinks(links);
        post.setSubjects(subjects);

        return Optional.of(postRepository.save(post));
    }

    @Override
    public Optional<String> updateCover(Long postId, String coverImageUrl) {
        return postRepository.findById(postId).map(post -> {
            post.setCoverImageUrl(coverImageUrl);
            postRepository.save(post);
            return coverImageUrl;
        });
    }

    @Override
    public Optional<String> removeCover(Long postId) {
        return postRepository.findById(postId).map(post -> {
            String existing = post.getCoverImageUrl();
            post.setCoverImageUrl(null);
            postRepository.save(post);
            return existing;
        });
    }

    @Override
    public Optional<Post> updatePost(PostDTO.Request.Update request) {
        Optional<Post> existing = postRepository.findById(request.getId());
        if (existing.isEmpty()) return Optional.empty();

        Post post = existing.get();
        boolean wasStub = post.isStub();

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getSubjects() != null) {
            post.setSubjects(new ArrayList<>(request.getSubjects()));
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            post.setStub(false);
        }

        List<Long> mergedLinks = new ArrayList<>(post.getLinks() != null ? post.getLinks() : List.of());
        if (request.getLinks() != null) {
            for (Long id : request.getLinks()) {
                if (!mergedLinks.contains(id)) mergedLinks.add(id);
            }
        }
        resolveWikilinksInto(mergedLinks, request.getWikilinks(), post.getAuthor(), post.getSubjects());
        post.setLinks(mergedLinks);

        Post saved = postRepository.save(post);

        if (wasStub && !saved.isStub()) {
            stubNotificationService.notifyAndCleanup(saved);
        }

        return Optional.of(saved);
    }

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @Override
    public Page<Post> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    @Override
    public Page<Post> getFeed(String username, Pageable pageable) {
        List<Long> followedIds = followRepository.findFollowedIdsByFollowerUsername(username);
        if (followedIds.isEmpty()) return Page.empty(pageable);

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );
        return postRepository.findFeedPosts(followedIds, sorted);
    }

    @Override
    public Page<Post> getExplore(String username, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );
        return postRepository.findByAuthorUsernameNotAndStubFalse(username, sorted);
    }

    @Override
    public Page<Post> getPostsByUser(String username, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );
        return postRepository.findByAuthorUsernameAndStubFalse(username, sorted);
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
    public Page<Post> searchByTitle(String title, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );
        return postRepository.findByTitleContainingIgnoreCaseAndStubFalse(title, sorted);
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

    @Override
    public boolean subscribeToStub(Long postId, String subscriberUsername) {
        Optional<Post> postOpt = postRepository.findById(postId);
        Optional<User> userOpt = userRepository.findByUsername(subscriberUsername);
        if (postOpt.isEmpty() || userOpt.isEmpty() || !postOpt.get().isStub()) return false;

        Post post = postOpt.get();
        User user = userOpt.get();
        if (subscriptionRepository.existsByPostAndUser(post, user)) return true;

        subscriptionRepository.save(new StubSubscription(post, user));

        Event stubEvent = new Event();
        stubEvent.setPostId(postId);
        stubEvent.setEventType(EventType.STUB_SUBSCRIBE);
        stubEvent.setSessionId("system");
        stubEvent.setUsername(subscriberUsername);
        eventRepository.save(stubEvent);

        return true;
    }

    private static final int MAX_STUBS_PER_POST = 10;

    private void resolveWikilinksInto(List<Long> links, List<PostDTO.Request.WikilinkRequest> wikilinks,
                                       User author, List<String> parentSubjects) {
        if (wikilinks == null) return;

        int stubCount = links.isEmpty() ? 0 : postRepository.countByIdInAndStubTrue(links);

        for (PostDTO.Request.WikilinkRequest wikilink : wikilinks) {
            Optional<Post> found = postRepository.findPostByTitle(wikilink.getTitle());

            Long resolvedId;
            if (found.isPresent()) {
                Post existing = found.get();
                if (existing.isStub() && !links.contains(existing.getId())) {
                    if (stubCount >= MAX_STUBS_PER_POST) continue;
                    stubCount++;
                }
                resolvedId = existing.getId();
            } else {
                if (stubCount >= MAX_STUBS_PER_POST) continue;
                List<String> stubSubjects = (wikilink.getSubjects() != null && !wikilink.getSubjects().isEmpty())
                        ? new ArrayList<>(wikilink.getSubjects())
                        : new ArrayList<>(parentSubjects);
                Post stub = new Post();
                stub.setTitle(wikilink.getTitle());
                stub.setContent("");
                stub.setAuthor(author);
                stub.setSubjects(stubSubjects);
                stub.setLinks(new ArrayList<>());
                stub.setStub(true);
                resolvedId = postRepository.save(stub).getId();
                stubCount++;
            }

            if (!links.contains(resolvedId)) links.add(resolvedId);
        }
    }
}
