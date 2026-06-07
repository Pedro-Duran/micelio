package com.puredo.blog.Repository.Like;

import com.puredo.blog.Entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByPostIdAndUserUsername(Long postId, String username);

    @Transactional
    void deleteByPostIdAndUserUsername(Long postId, String username);

    long countByPostId(Long postId);

    @Query("SELECT l.post.id, COUNT(l) FROM Like l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT l.post.id FROM Like l WHERE l.post.id IN :postIds AND l.user.username = :username")
    List<Long> findLikedPostIds(@Param("postIds") List<Long> postIds, @Param("username") String username);
}