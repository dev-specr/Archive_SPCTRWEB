package com.spectre.ship.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ShipDtos {
    @Builder
    @Getter
    public static class ShipResponse {
        private Long id;
        private String uuid;
        private String name;
        private String manufacturer;
        private String type;
        private String focus;
        private String size;
        private Integer cargoCapacity;
        private Integer scmSpeed;
        private Integer navMaxSpeed;
        private Double pitch;
        private Double yaw;
        private Double roll;
        private Integer hp;
        private String pledgeUrl;
        private String description;
    }
}

