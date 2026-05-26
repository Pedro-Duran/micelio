package com.puredo.blog.Repository.User;


import com.puredo.blog.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;



@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByUsernameContainingIgnoreCase(String username);

    @Query(value = "SELECT * FROM USERS", nativeQuery = true)
    List<User> findAllNative();

}
