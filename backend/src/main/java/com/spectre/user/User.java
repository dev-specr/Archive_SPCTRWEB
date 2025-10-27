package com.spectre.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String discordId;

    private String username;
    private String avatar;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public UserDetails toUserDetails() {
        Collection<? extends GrantedAuthority> auth = roles == null ?
                Set.<Role>of().stream().map(r -> new SimpleGrantedAuthority(r.name())).collect(Collectors.toSet())
                : roles.stream().map(r -> new SimpleGrantedAuthority(r.name())).collect(Collectors.toSet());
        return new org.springframework.security.core.userdetails.User(
                String.valueOf(id),
                "",
                auth
        );
    }
}