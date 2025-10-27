package com.spectre.commodity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "uex_commodities",
       indexes = {
           @Index(name = "idx_uexcommodities_name", columnList = "name")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UexCommodity {
    @Id
    @Column(name = "uex_id")
    private Long uexId; 

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 16)
    private String code;

    @Column(length = 64)
    private String kind;

    private Double weightScu;

    
    private Boolean buyable;
    private Boolean sellable;
    private Boolean extractable;
    private Boolean mineral;
    private Boolean raw;
    private Boolean refined;

    @Column(length = 512)
    private String wikiUrl;

    private Instant dateAdded;
    private Instant dateModified;
}

