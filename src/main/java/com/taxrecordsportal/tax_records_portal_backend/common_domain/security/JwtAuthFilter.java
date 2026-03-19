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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

        // 3. extract token and user ID
        final String token = authHeader.substring(7); // strips "Bearer "
        final String userId;
        try {
            userId = jwtService.extractSubject(token);
        } catch (Exception e) {
            // Token is expired or invalid — skip authentication so Spring Security returns 401
            filterChain.doFilter(request, response);
            return;
        }

        // if user ID found and user not yet authenticated
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null){

            // load user from DB
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);

            // validate token
            if (user != null && jwtService.isTokenValid(token, user)){

                // tell Spring this user is authenticated
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // continue the request
        filterChain.doFilter(request, response);
    }
}
