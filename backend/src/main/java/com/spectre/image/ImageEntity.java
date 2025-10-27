package com.spectre.image;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "images", indexes = {
        @Index(name = "idx_images_user_id", columnList = "uploaderId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long uploaderId;

    @Column(nullable = true, length = 150)
    private String uploaderName; 

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = true, length = 255)
    private String filename;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    @Column(nullable = false, length = 100)
    private String contentType; 

    @Column(nullable = false)
    private Instant uploadedAt;

    @PrePersist
    void onCreate() {
        uploadedAt = Instant.now();
    }
}
