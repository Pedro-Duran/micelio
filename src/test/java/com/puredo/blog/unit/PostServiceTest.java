package com.puredo.blog.unit;

import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.StubSubscription;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Event.EventRepository;
import com.puredo.blog.Repository.Follow.FollowRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.StubSubscription.StubSubscriptionRepository;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.Post.PostServiceImpl;
import com.puredo.blog.Service.Post.StubNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock StubSubscriptionRepository subscriptionRepository;
    @Mock StubNotificationService stubNotificationService;

    @InjectMocks PostServiceImpl postService;

    private User alice;
    private Post post;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setEmail("alice@test.com");

        post = new Post();
        post.setId(1L);
        post.setTitle("Test Post");
        post.setContent("Test content");
        post.setAuthor(alice);
        post.setLinks(new ArrayList<>());
        post.setStub(false);
        post.setSubject("Tech");
        post.setCreatedAt(LocalDateTime.now());
    }

    // ---- createPost ----

    @Test
    void createPost_authorFound_returnsPost() {
        PostDTO.Request.Create request = new PostDTO.Request.Create("Test", "Content", "alice", null, null, "Tech");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(postRepository.save(any())).thenReturn(post);

        Optional<Post> result = postService.createPost(request);

        assertThat(result).isPresent();
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_authorNotFound_returnsEmpty() {
        PostDTO.Request.Create request = new PostDTO.Request.Create("Test", "Content", "nobody", null, null, "Tech");
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        Optional<Post> result = postService.createPost(request);

        assertThat(result).isEmpty();
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_withNewWikilink_createsStubAndLinksIt() {
        PostDTO.Request.Create request = new PostDTO.Request.Create(
                "Main", "Content", "alice", null, List.of("New Topic"), "Tech");
        Post stubPost = new Post();
        stubPost.setStub(true);
        stubPost.setLinks(new ArrayList<>());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(postRepository.findPostByTitle("New Topic")).thenReturn(Optional.empty());
        when(postRepository.save(any())).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            if (p.isStub()) { p.setId(10L); return p; }
            return post;
        });

        Optional<Post> result = postService.createPost(request);

        assertThat(result).isPresent();
        verify(postRepository, times(2)).save(any(Post.class));
    }

    @Test
    void createPost_withExistingWikilink_resolvesWithoutCreatingStub() {
        Post existingWiki = new Post();
        existingWiki.setId(5L);
        existingWiki.setTitle("Existing");

        PostDTO.Request.Create request = new PostDTO.Request.Create(
                "Main", "Content", "alice", null, List.of("Existing"), "Tech");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(postRepository.findPostByTitle("Existing")).thenReturn(Optional.of(existingWiki));
        when(postRepository.save(any())).thenReturn(post);

        Optional<Post> result = postService.createPost(request);

        assertThat(result).isPresent();
        verify(postRepository, times(1)).save(any(Post.class));
    }

    // ---- updatePost ----

    @Test
    void updatePost_postFound_updatesAndReturns() {
        PostDTO.Request.Update request = new PostDTO.Request.Update(1L, "New Title", "New Content", null, null, "Science");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(post);

        Optional<Post> result = postService.updatePost(request);

        assertThat(result).isPresent();
        verify(postRepository).save(post);
    }

    @Test
    void updatePost_postNotFound_returnsEmpty() {
        PostDTO.Request.Update request = new PostDTO.Request.Update(99L, "Title", "Content", null, null, "Tech");
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(postService.updatePost(request)).isEmpty();
    }

    @Test
    void updatePost_stubBecomesPublished_triggersNotification() {
        post.setStub(true);
        post.setContent("");
        Post savedPost = new Post();
        savedPost.setStub(false);
        savedPost.setContent("Real Content");

        PostDTO.Request.Update request = new PostDTO.Request.Update(1L, "Title", "Real Content", null, null, "Tech");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(savedPost);

        postService.updatePost(request);

        verify(stubNotificationService).notifyAndCleanup(savedPost);
    }

    @Test
    void updatePost_stubRemainsStub_doesNotNotify() {
        post.setStub(true);
        post.setContent("");
        Post savedPost = new Post();
        savedPost.setStub(true);

        PostDTO.Request.Update request = new PostDTO.Request.Update(1L, "Title", "", null, null, "Tech");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(savedPost);

        postService.updatePost(request);

        verify(stubNotificationService, never()).notifyAndCleanup(any());
    }

    // ---- updateCover / removeCover ----

    @ParameterizedTest
    @CsvSource({"1, https://s3.aws/cover1.jpg", "2, https://s3.aws/cover2.png"})
    void updateCover_postFound_returnsUrl(Long postId, String url) {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(post);

        Optional<String> result = postService.updateCover(postId, url);

        assertThat(result).isPresent().contains(url);
    }

    @Test
    void updateCover_postNotFound_returnsEmpty() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(postService.updateCover(99L, "url")).isEmpty();
    }

    @Test
    void removeCover_postFound_returnsOldUrl() {
        post.setCoverImageUrl("https://s3.aws/old.jpg");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(post);

        Optional<String> result = postService.removeCover(1L);

        assertThat(result).isPresent().contains("https://s3.aws/old.jpg");
    }

    @Test
    void removeCover_postNotFound_returnsEmpty() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(postService.removeCover(99L)).isEmpty();
    }

    // ---- getFeed ----

    @Test
    void getFeed_noFollowedUsers_returnsEmptyPage() {
        when(followRepository.findFollowedIdsByFollowerUsername("alice")).thenReturn(List.of());

        Page<Post> result = postService.getFeed("alice", PageRequest.of(0, 10));

        assertThat(result.isEmpty()).isTrue();
        verify(postRepository, never()).findFeedPosts(any(), any());
    }

    @Test
    void getFeed_withFollowedUsers_returnsPosts() {
        when(followRepository.findFollowedIdsByFollowerUsername("alice")).thenReturn(List.of(2L));
        when(postRepository.findFeedPosts(anyList(), any())).thenReturn(new PageImpl<>(List.of(post)));

        Page<Post> result = postService.getFeed("alice", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    // ---- getExplore / getPostsByUser ----

    @Test
    void getExplore_returnsPostsFromOtherUsers() {
        when(postRepository.findByAuthorUsernameNotAndStubFalse(eq("alice"), any()))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<Post> result = postService.getExplore("alice", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getPostsByUser_returnsUserOwnPosts() {
        when(postRepository.findByAuthorUsernameAndStubFalse(eq("alice"), any()))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<Post> result = postService.getPostsByUser("alice", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    // ---- deletePostById ----

    @Test
    void deletePostById_deletesEventsRemovesLinksAndDeletesPost() {
        Post linkedPost = new Post();
        linkedPost.setId(2L);
        linkedPost.setLinks(new ArrayList<>(List.of(1L)));

        when(postRepository.findPostsByLinkId(1L)).thenReturn(List.of(linkedPost));

        postService.deletePostById(1L);

        verify(eventRepository).deleteByPostId(1L);
        verify(postRepository).saveAll(anyList());
        verify(postRepository).deleteById(1L);
        assertThat(linkedPost.getLinks()).doesNotContain(1L);
    }

    @Test
    void deletePostById_noLinkedPosts_stillDeletesPostAndEvents() {
        when(postRepository.findPostsByLinkId(1L)).thenReturn(List.of());

        postService.deletePostById(1L);

        verify(eventRepository).deleteByPostId(1L);
        verify(postRepository).deleteById(1L);
    }

    // ---- subscribeToStub ----

    @Test
    void subscribeToStub_stubPost_savesAndReturnsTrue() {
        post.setStub(true);
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(subscriptionRepository.existsByPostAndUser(post, bob)).thenReturn(false);

        boolean result = postService.subscribeToStub(1L, "bob");

        assertThat(result).isTrue();
        verify(subscriptionRepository).save(any(StubSubscription.class));
    }

    @Test
    void subscribeToStub_alreadySubscribed_returnsTrueWithoutDuplicate() {
        post.setStub(true);
        User bob = new User();
        bob.setUsername("bob");

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(subscriptionRepository.existsByPostAndUser(post, bob)).thenReturn(true);

        boolean result = postService.subscribeToStub(1L, "bob");

        assertThat(result).isTrue();
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribeToStub_notStubPost_returnsFalse() {
        post.setStub(false);
        User bob = new User();
        bob.setUsername("bob");

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        assertThat(postService.subscribeToStub(1L, "bob")).isFalse();
        verify(subscriptionRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nobody", "ghost"})
    void subscribeToStub_userNotFound_returnsFalse(String username) {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThat(postService.subscribeToStub(1L, username)).isFalse();
    }

    @Test
    void subscribeToStub_postNotFound_returnsFalse() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(new User()));

        assertThat(postService.subscribeToStub(99L, "bob")).isFalse();
    }

    // ---- findPostsBySubject ----

    @Test
    void findPostsBySubject_returnsIdTitleMap() {
        when(postRepository.findPostIdsAndTitlesBySubject("Tech"))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Post A"}, new Object[]{2L, "Post B"}));

        var result = postService.findPostsBySubject("Tech");

        assertThat(result).hasSize(2)
                .containsEntry(1L, "Post A")
                .containsEntry(2L, "Post B");
    }

    @Test
    void getDistinctSubjects_delegatesToRepository() {
        when(postRepository.findDistinctSubjects()).thenReturn(List.of("Tech", "Life", "Art"));

        List<String> result = postService.getDistinctSubjects();

        assertThat(result).containsExactlyInAnyOrder("Tech", "Life", "Art");
    }
}