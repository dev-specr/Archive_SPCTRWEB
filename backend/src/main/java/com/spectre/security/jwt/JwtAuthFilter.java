package com.spectre.security.jwt;

import com.spectre.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        try {
            String subject = jwtService.extractSubject(token);
            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (subject.startsWith("d:")) {
                    
                    String username = jwtService.extractClaim(token, c -> (String) c.get("username"));
                    String discordId = jwtService.extractClaim(token, c -> (String) c.get("discordId"));
                    java.util.List<?> rolesClaim = jwtService.extractClaim(token, c -> c.get("roles", java.util.List.class));
                    java.util.Collection<org.springframework.security.core.GrantedAuthority> auths = new java.util.ArrayList<>();
                    if (rolesClaim != null) {
                        for (Object r : rolesClaim) {
                            String name = String.valueOf(r);
                            if (name != null && !name.isBlank()) auths.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(name));
                        }
                    }
                    String uname = (username != null && !username.isBlank()) ? username : (discordId != null ? discordId : subject);
                    UserDetails principal = new org.springframework.security.core.userdetails.User(uname, "", auths);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, auths);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    
                    Optional<com.spectre.user.User> userOpt = Optional.empty();
                    try {
                        userOpt = userRepository.findById(Long.parseLong(subject));
                    } catch (NumberFormatException ignore) { /* fall through */ }
                    if (userOpt.isPresent() && jwtService.isTokenValid(token, userOpt.get())) {
                        UserDetails principal = userOpt.get().toUserDetails();
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        } catch (Exception ignored) {
            
        }
        filterChain.doFilter(request, response);
    }
}
