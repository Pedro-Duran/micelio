package com.puredo.blog.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Cascade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private User author;

    @ElementCollection
    @CollectionTable(name = "post_links", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "link", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<Long> links;

    @ElementCollection
    @CollectionTable(name = "post_subjects", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "subject", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<String> subjects = new ArrayList<>();

    @Column(name = "is_stub", nullable = false)
    private boolean stub = false;

    @Column
    private String coverImageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
