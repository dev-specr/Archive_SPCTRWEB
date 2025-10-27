package com.spectre.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

public class PostDtos {

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank
        @Size(max = 150)
        private String title;

        @NotBlank
        private String content;
    }

    @Getter @Setter
    public static class UpdateRequest {
        @NotBlank
        @Size(max = 150)
        private String title;

        @NotBlank
        private String content;
    }

    @Builder @Getter
    public static class Response {
        private Long id;
        private Long userId;
        private String title;
        private String content;
        private Instant createdAt;
        private Instant updatedAt;
        private boolean mine; 
    }
}