package com.spectre.post;

import com.spectre.post.dto.PostDtos;
import com.spectre.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    
    @GetMapping
    public Page<PostDtos.Response> list(@RequestParam(name = "page", required = false) Integer page,
                                        @RequestParam(name = "size", required = false) Integer size) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        return service.list(page, size, uid);
    }

    
    @GetMapping("/{id}")
    public PostDtos.Response get(@PathVariable("id") Long id) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        return service.get(id, uid);
    }

    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public PostDtos.Response create(@Valid @RequestBody PostDtos.CreateRequest req) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        return service.create(req, uid);
    }

    
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public PostDtos.Response update(@PathVariable Long id,
                                    @Valid @RequestBody PostDtos.UpdateRequest req,
                                    Authentication auth) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        boolean isAdmin = SecurityUtils.isAdmin(auth);
        return service.update(id, req, uid, isAdmin);
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public void delete(@PathVariable Long id, Authentication auth) {
        Long uid = SecurityUtils.currentUserIdOrNull();
        boolean isAdmin = SecurityUtils.isAdmin(auth);
        service.delete(id, uid, isAdmin);
    }
}
