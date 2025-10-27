package com.spectre.commodity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "commodity_prices",
       indexes = {
           @Index(name = "idx_commodity_name", columnList = "commodity"),
           @Index(name = "idx_commodity_location", columnList = "location"),
           @Index(name = "idx_uex_ids", columnList = "uex_commodity_id, uex_location_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_commodity_location", columnNames = {"commodity", "location"})
       }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommodityPrice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String commodity;

    @Column(name = "star_system", length = 80)
    private String system;

    @Column(nullable = false, length = 160)
    private String location;

    @Column
    private Double buy;  

    @Column
    private Double sell; 

    @Column(length = 8)
    private String currency; 

    @Column(name = "uex_commodity_id")
    private Long uexCommodityId;

    @Column(name = "uex_location_id")
    private Long uexLocationId;

    @Column(name = "uex_category_id")
    private Long uexCategoryId;

    
    @Column(name = "prev_buy")
    private Double prevBuy;

    @Column(name = "prev_sell")
    private Double prevSell;

    @Column(name = "prev_updated_at")
    private Instant prevUpdatedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist @PreUpdate
    void touch() { if (updatedAt == null) updatedAt = Instant.now(); else updatedAt = Instant.now(); }
}
