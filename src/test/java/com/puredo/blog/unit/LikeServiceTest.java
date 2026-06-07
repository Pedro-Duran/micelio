package com.puredo.blog.unit;

import com.puredo.blog.Entity.Like;
import com.puredo.blog.Entity.Post;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.Like.LikeRepository;
import com.puredo.blog.Repository.Post.PostRepository;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.Like.LikeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock LikeRepository likeRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    @InjectMocks LikeServiceImpl likeService;

    private Post post;
    private User alice;

    @BeforeEach
    void setUp() {
        post = new Post();
        post.setId(1L);
        post.setTitle("Test Post");

        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
    }

    // ---- like ----

    @Test
    void like_newLike_savesAndReturnsTrue() {
        when(likeRepository.existsByPostIdAndUserUsername(1L, "alice")).thenReturn(false);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        boolean result = likeService.like(1L, "alice");

        assertThat(result).isTrue();
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void like_duplicate_returnsFalseWithoutSaving() {
        when(likeRepository.existsByPostIdAndUserUsername(1L, "alice")).thenReturn(true);

        boolean result = likeService.like(1L, "alice");

        assertThat(result).isFalse();
        verify(likeRepository, never()).save(any());
    }

    @Test
    void like_postNotFound_throws() {
        when(likeRepository.existsByPostIdAndUserUsername(99L, "alice")).thenReturn(false);
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.like(99L, "alice"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // ---- unlike ----

    @Test
    void unlike_delegatesToRepository() {
        likeService.unlike(1L, "alice");

        verify(likeRepository).deleteByPostIdAndUserUsername(1L, "alice");
    }

    // ---- countByPost ----

    @Test
    void countByPost_returnsDelegatedCount() {
        when(likeRepository.countByPostId(1L)).thenReturn(7L);

        assertThat(likeService.countByPost(1L)).isEqualTo(7L);
    }

    // ---- isLikedByUser ----

    @Test
    void isLikedByUser_true_whenLikeExists() {
        when(likeRepository.existsByPostIdAndUserUsername(1L, "alice")).thenReturn(true);

        assertThat(likeService.isLikedByUser(1L, "alice")).isTrue();
    }

    @Test
    void isLikedByUser_false_whenLikeAbsent() {
        when(likeRepository.existsByPostIdAndUserUsername(1L, "bob")).thenReturn(false);

        assertThat(likeService.isLikedByUser(1L, "bob")).isFalse();
    }

    // ---- countByPosts ----

    @Test
    void countByPosts_emptyList_returnsEmptyMap() {
        Map<Long, Long> result = likeService.countByPosts(List.of());

        assertThat(result).isEmpty();
        verify(likeRepository, never()).countByPostIds(any());
    }

    @Test
    void countByPosts_withData_returnsCorrectMap() {
        when(likeRepository.countByPostIds(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}, new Object[]{2L, 1L}));

        Map<Long, Long> result = likeService.countByPosts(List.of(1L, 2L));

        assertThat(result).hasSize(2).containsEntry(1L, 3L).containsEntry(2L, 1L);
    }

    // ---- likedPostIds ----

    @Test
    void likedPostIds_emptyList_returnsEmptySet() {
        Set<Long> result = likeService.likedPostIds(List.of(), "alice");

        assertThat(result).isEmpty();
        verify(likeRepository, never()).findLikedPostIds(any(), any());
    }

    @Test
    void likedPostIds_nullUsername_returnsEmptySet() {
        Set<Long> result = likeService.likedPostIds(List.of(1L, 2L), null);

        assertThat(result).isEmpty();
        verify(likeRepository, never()).findLikedPostIds(any(), any());
    }

    @Test
    void likedPostIds_withData_returnsSetOfLikedIds() {
        when(likeRepository.findLikedPostIds(List.of(1L, 2L, 3L), "alice"))
                .thenReturn(List.of(1L, 3L));

        Set<Long> result = likeService.likedPostIds(List.of(1L, 2L, 3L), "alice");

        assertThat(result).containsExactlyInAnyOrder(1L, 3L);
        assertThat(result).doesNotContain(2L);
    }
}