package com.taxrecordsportal.tax_records_portal_backend.common_domain.security;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        // if no token, skip — let SecurityConfig handle it
        if (authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        final JwtService.ParsedToken parsed;
        try {
            parsed = jwtService.parseToken(token);
        } catch (Exception e) {
            // Token is expired or invalid — skip authentication so Spring Security returns 401
            filterChain.doFilter(request, response);
            return;
        }

        // if user ID found and user not yet authenticated
        if (parsed.userId() != null && SecurityContextHolder.getContext().getAuthentication() == null){

            // lightweight query — no role/permissions/position joins
            User user = userRepository.findByIdLightweight(UUID.fromString(parsed.userId())).orElse(null);

            if (user != null) {
                // use permissions from JWT claims instead of loading from DB
                List<GrantedAuthority> authorities = parsed.permissions().stream()
                        .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p))
                        .toList();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
