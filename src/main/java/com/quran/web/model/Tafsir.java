package com.quran.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mst_tafsir",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ayah_id", "tafsir_source_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tafsir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ayah_id", nullable = false)
    private Ayah ayah;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tafsir_source_id", nullable = false)
    private TafsirSource tafsirSource;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
