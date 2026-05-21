package com.puredo.blog.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Cascade;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.LocalDateTime;
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
    @Column(name = "link", nullable = false)
    @CollectionTable(name = "post_links", joinColumns = @JoinColumn(name = "post_id"))
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<Long> links;

    @Column(nullable = false)
    private String subject = "Sem Assunto";

    @Column(name = "is_stub", nullable = false)
    private boolean stub = false;




    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
