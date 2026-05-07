package com.quran.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mst_surah")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Surah {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "surah_number", unique = true, nullable = false)
    private Integer surahNumber;

    @Column(name = "name_arabic", nullable = false, length = 100)
    private String nameArabic;

    @Column(name = "name_latin", nullable = false, length = 100)
    private String nameLatin;

    @Column(name = "name_translation", length = 200)
    private String nameTranslation;

    @Column(name = "total_ayahs", nullable = false)
    private Integer totalAyahs;

    @Enumerated(EnumType.STRING)
    @Column(name = "revelation_type", nullable = false)
    private RevelationType revelationType;

    @Column(name = "revelation_order")
    private Integer revelationOrder;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship
    @OneToMany(mappedBy = "surah", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ayah> ayahs;

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
