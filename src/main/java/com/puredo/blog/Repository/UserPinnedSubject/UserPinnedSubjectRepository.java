package com.puredo.blog.Repository.UserPinnedSubject;

import com.puredo.blog.Entity.UserPinnedSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserPinnedSubjectRepository extends JpaRepository<UserPinnedSubject, Long> {
    boolean existsByUserUsernameAndSubjectName(String username, String subjectName);
    @Query("SELECT p.subjectName FROM UserPinnedSubject p WHERE p.user.username = :username ORDER BY p.pinnedAt ASC")
    List<String> findSubjectNamesByUsername(@Param("username") String username);
    @Transactional
    void deleteByUserUsernameAndSubjectName(String username, String subjectName);
}
