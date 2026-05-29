package com.puredo.blog.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty
    @Column(nullable = false, unique = true)
    private String username;

    @JsonProperty
    @Column
    private String password;

    @Column(unique = true)
    private String googleId;

    @Column(nullable = false)
    private boolean superuser = false;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String avatarUrl;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @ToString.Exclude
    private List<Post> posts;
}
