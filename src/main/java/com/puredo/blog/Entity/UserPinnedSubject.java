package com.puredo.blog.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_pinned_subjects", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "subject_name"})
})
@Data
@NoArgsConstructor
public class UserPinnedSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;

    public UserPinnedSubject(User user, String subjectName) {
        this.user = user;
        this.subjectName = subjectName;
    }

    @PrePersist
    protected void onCreate() {
        this.pinnedAt = LocalDateTime.now();
    }
}
