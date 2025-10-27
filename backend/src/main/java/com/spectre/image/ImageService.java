package com.spectre.image;

import com.spectre.image.dto.ImageDtos;
import com.spectre.user.User;
import com.spectre.user.UserRepository;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository repo;
    private final UserRepository userRepository;

    public Page<ImageDtos.ListItem> list(Integer page, Integer size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page == null ? 0 : page, size == null ? 24 : size);
        return repo.findAll(pageable).map(img -> toListItem(img, currentUserId));
    }

    public ImageEntity get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Image not found"));
    }

    public ImageDtos.UploadResponse upload(String title, MultipartFile file, Long currentUserId) {
        if (currentUserId == null) throw new ResponseStatusException(UNAUTHORIZED, "Login required");
        if (file == null || file.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "Missing file");
        String originalType = file.getContentType();
        if (originalType == null) throw new ResponseStatusException(BAD_REQUEST, "Unknown content type");

        
        if (!List.of("image/jpeg", "image/png").contains(originalType.toLowerCase())) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "Only JPEG and PNG are supported");
        }

        byte[] jpegData;
        try {
            BufferedImage input = ImageIO.read(file.getInputStream());
            if (input == null) throw new IOException("Unreadable image");
            
            BufferedImage rgb = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setComposite(AlphaComposite.SrcOver);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(input, 0, 0, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            ImageIO.write(rgb, "jpeg", baos);
            jpegData = baos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Image processing failed: " + e.getMessage());
        }

        String uploaderName = resolveUploaderName(currentUserId);

        ImageEntity entity = ImageEntity.builder()
                .uploaderId(currentUserId)
                .uploaderName(uploaderName)
                .title(title == null ? "Untitled" : title.trim())
                .filename(file.getOriginalFilename())
                .content(jpegData)
                .contentType("image/jpeg")
                .build();

        entity = repo.save(entity);

        ImageDtos.UploadResponse resp = new ImageDtos.UploadResponse();
        resp.setId(entity.getId());
        resp.setTitle(entity.getTitle());
        resp.setUrl("/api/images/" + entity.getId() + "/raw");
        resp.setUploadedAt(entity.getUploadedAt());
        resp.setUploaderName(entity.getUploaderName());
        return resp;
    }

    public void delete(Long id, Long currentUserId, boolean isAdmin) {
        ImageEntity img = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Image not found"));
        if (!isAdmin && (currentUserId == null || !img.getUploaderId().equals(currentUserId))) {
            throw new ResponseStatusException(FORBIDDEN, "You can only delete your own images");
        }
        repo.delete(img);
    }

    private ImageDtos.ListItem toListItem(ImageEntity img, Long currentUserId) {
        boolean mine = currentUserId != null && currentUserId.equals(img.getUploaderId());
        String name = img.getUploaderName();
        if (name == null || name.isBlank()) {
            name = resolveUploaderName(img.getUploaderId());
        }
        return ImageDtos.ListItem.builder()
                .id(img.getId())
                .title(img.getTitle())
                .uploaderId(img.getUploaderId())
                .url("/api/images/" + img.getId() + "/raw")
                .uploadedAt(img.getUploadedAt())
                .mine(mine)
                .uploaderName(name)
                .build();
    }

    private String resolveUploaderName(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }
}
