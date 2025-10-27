package com.spectre.ship;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ships", indexes = {
        @Index(name = "idx_ships_name", columnList = "name"),
        @Index(name = "idx_ships_uuid", columnList = "uuid", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ship {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String uuid; 

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String manufacturer;

    @Column(length = 60)
    private String type;

    @Column(length = 80)
    private String focus;

    @Column(length = 40)
    private String size;

    private Integer cargoCapacity;
    private Integer scmSpeed;
    private Integer navMaxSpeed;
    private Double pitch;
    private Double yaw;
    private Double roll;
    private Integer hp;

    @Column(length = 1024)
    private String pledgeUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private Instant updatedAt;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String rawJson; 

    @PrePersist @PreUpdate
    void touch() { updatedAt = Instant.now(); }
}