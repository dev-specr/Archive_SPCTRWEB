package com.spectre.image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
    Page<ImageEntity> findAllByUploaderId(Long uploaderId, Pageable pageable);
}