package com.quran.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mst_word",
        indexes = @Index(name = "idx_ayah_position", columnList = "ayah_id,position"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ayah_id", nullable = false)
    private Ayah ayah;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "text_arabic", nullable = false, length = 200)
    private String textArabic;

    @Column(name = "text_simple", length = 200)
    private String textSimple;

    @Column(length = 200)
    private String transliteration;

    @Column(name = "root_word", length = 100)
    private String rootWord;

    @Column(name = "grammar_notes", columnDefinition = "TEXT")
    private String grammarNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WordTranslation> wordTranslations;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
