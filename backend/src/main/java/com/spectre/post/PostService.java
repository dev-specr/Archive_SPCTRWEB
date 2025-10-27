package com.spectre.post;

import com.spectre.post.dto.PostDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository repo;

    public Page<PostDtos.Response> list(Integer page, Integer size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page == null ? 0 : page, size == null ? 20 : size);
        return repo.findAll(pageable).map(p -> toResponse(p, currentUserId));
    }

    public PostDtos.Response get(Long id, Long currentUserId) {
        Post p = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Post not found"));
        return toResponse(p, currentUserId);
    }

    public PostDtos.Response create(PostDtos.CreateRequest req, Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Login required");
        }
        Post p = Post.builder()
                .userId(currentUserId)
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .build();
        p = repo.save(p);
        return toResponse(p, currentUserId);
    }

    public PostDtos.Response update(Long id, PostDtos.UpdateRequest req, Long currentUserId, boolean isAdmin) {
        Post p = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Post not found"));
        if (!isAdmin && (currentUserId == null || !p.getUserId().equals(currentUserId))) {
            throw new ResponseStatusException(FORBIDDEN, "You can only edit your own posts");
        }
        p.setTitle(req.getTitle().trim());
        p.setContent(req.getContent().trim());
        p = repo.save(p);
        return toResponse(p, currentUserId);
    }

    public void delete(Long id, Long currentUserId, boolean isAdmin) {
        Post p = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Post not found"));
        if (!isAdmin && (currentUserId == null || !p.getUserId().equals(currentUserId))) {
            throw new ResponseStatusException(FORBIDDEN, "You can only delete your own posts");
        }
        repo.delete(p);
    }

    private PostDtos.Response toResponse(Post p, Long currentUserId) {
        boolean mine = currentUserId != null && currentUserId.equals(p.getUserId());
        return PostDtos.Response.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .title(p.getTitle())
                .content(p.getContent())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .mine(mine)
                .build();
    }
}