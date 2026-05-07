package com.quran.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mst_ayah",
        uniqueConstraints = @UniqueConstraint(columnNames = {"surah_id", "ayah_number"}),
        indexes = {
                @Index(name = "idx_juz", columnList = "juz_number"),
                @Index(name = "idx_page", columnList = "page_number")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ayah {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surah_id", nullable = false)
    private Surah surah;

    @Column(name = "ayah_number", nullable = false)
    private Integer ayahNumber;

    @Column(name = "text_arabic", nullable = false, columnDefinition = "TEXT")
    private String textArabic;

    @Column(name = "text_simple", columnDefinition = "TEXT")
    private String textSimple;

    @Column(name = "juz_number")
    private Integer juzNumber;

    @Column(name = "hizb_number")
    private Integer hizbNumber;

    @Column(name = "manzil_number")
    private Integer manzilNumber;

    @Column(name = "page_number")
    private Integer pageNumber;

    private Boolean sajda = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "ayah", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Word> words;

    @OneToMany(mappedBy = "ayah", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Translation> translations;

    @OneToMany(mappedBy = "ayah", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Tafsir> tafsirs;

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
