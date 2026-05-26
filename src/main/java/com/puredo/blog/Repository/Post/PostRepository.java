package com.puredo.blog.Repository.Post;


import com.puredo.blog.Entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    public Optional<Post> findPostByTitle(String title);

    @Query("SELECT DISTINCT p.subject FROM Post p")
    List<String> findDistinctSubjects();

    @Query("SELECT p.id, p.title FROM Post p WHERE p.subject = :subject")
    List<Object[]> findPostIdsAndTitlesBySubject(@Param("subject") String subject);

    @Query("SELECT p.id FROM Post p WHERE p.author.username = :username")
    List<Long> findPostIdsByAuthorUsername(@Param("username") String username);

    @Query("SELECT p FROM Post p JOIN p.links l WHERE l = :linkedPostId")
    List<Post> findPostsByLinkId(@Param("linkedPostId") Long linkedPostId);

    Page<Post> findByAuthorUsernameNotAndStubFalse(String username, Pageable pageable);

    Page<Post> findByAuthorUsernameAndStubFalse(String username, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.author.id IN :authorIds AND p.stub = false")
    Page<Post> findFeedPosts(@Param("authorIds") List<Long> authorIds, Pageable pageable);
}
