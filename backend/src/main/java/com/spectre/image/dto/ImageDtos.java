package com.spectre.image.dto;

import lombok.*;

import java.time.Instant;

public class ImageDtos {

    @Getter @Setter
    public static class UploadResponse {
        private Long id;
        private String title;
        private String url;
        private Instant uploadedAt;
        private String uploaderName;
    }

    @Builder @Getter
    public static class ListItem {
        private Long id;
        private String title;
        private String url;
        private Long uploaderId;
        private Instant uploadedAt;
        private boolean mine;
        private String uploaderName;
    }
}
