package com.puredo.blog.Repository.Follow;

import com.puredo.blog.Entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerUsernameAndFollowedUsername(String followerUsername, String followedUsername);

    List<Follow> findByFollowerUsername(String followerUsername);

    List<Follow> findByFollowedUsername(String followedUsername);

    boolean existsByFollowerUsernameAndFollowedUsername(String followerUsername, String followedUsername);

    @Query("SELECT f.followed.id FROM Follow f WHERE f.follower.username = :username")
    List<Long> findFollowedIdsByFollowerUsername(@Param("username") String username);
}
