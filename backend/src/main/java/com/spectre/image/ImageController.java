package com.spectre.image;

import com.spectre.image.dto.ImageDtos;
import com.spectre.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService service;

    
    @GetMapping
    public Page<ImageDtos.ListItem> list(@RequestParam(name = "page", required = false) Integer page,
                                         @RequestParam(name = "size", required = false) Integer size) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        return service.list(page, size, uid);
    }

    
    @GetMapping("/{id}/raw")
    public ResponseEntity<byte[]> raw(@PathVariable("id") Long id) {
        ImageEntity img = service.get(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, img.getContentType())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(img.getContent());
    }

    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER','MEMBER','ADMIN')")
    public ResponseEntity<ImageDtos.UploadResponse> upload(@RequestPart("file") MultipartFile file,
                                                           @RequestPart(value = "title", required = false) String title) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        var resp = service.upload(title, file, uid);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MEMBER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, Authentication auth) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        boolean isAdmin = SecurityUtils.isAdmin(auth);
        service.delete(id, uid, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
