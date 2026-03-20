package com.taxrecordsportal.tax_records_portal_backend.common_domain.security;


import com.taxrecordsportal.tax_records_portal_backend.user_domain.permission.Permission;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Getter
    @Value("${application.security.jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    public String generateAccessToken(UserDetails userDetails){
        return buildAccessToken(userDetails, accessTokenExpiration);
    }

    private String buildAccessToken(UserDetails userDetails, long expiration){
        User user = (User) userDetails;

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("name", formatNameWithTitles(user));
        claims.put("role", user.getRole().getName());
        claims.put("roleKey", user.getRole().getKey().name());
        claims.put("position", user.getPosition() != null ? user.getPosition().getName() : null);
        claims.put("permissions", user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList()));
        claims.put("status", user.getStatus());
        claims.put("profile_url", user.getProfileUrl());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    // parses token once and returns all claims
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Object permissions = parseClaims(token).get("permissions");
        return permissions instanceof List<?> list ? (List<String>) list : List.of();
    }

    // parses once, returns subject + permissions + validates expiry
    @SuppressWarnings("unchecked")
    public ParsedToken parseToken(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        Object perms = claims.get("permissions");
        List<String> permissions = perms instanceof List<?> list ? (List<String>) list : List.of();
        return new ParsedToken(subject, permissions);
    }

    public record ParsedToken(String userId, List<String> permissions) {}

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String subject = extractSubject(token);
        User user = (User) userDetails;
        return subject.equals(user.getId().toString()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private String formatNameWithTitles(User user) {
        String fullName = user.getFirstName() + " " + user.getLastName();
        List<UserTitle> titles = user.getTitles();
        if (titles == null || titles.isEmpty()) {
            return fullName;
        }

        String prefixes = titles.stream()
                .filter(UserTitle::prefix)
                .map(UserTitle::title)
                .collect(Collectors.joining(", "));

        String suffixes = titles.stream()
                .filter(t -> !t.prefix())
                .map(UserTitle::title)
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        if (!prefixes.isEmpty()) {
            sb.append(prefixes).append(" ");
        }
        sb.append(fullName);
        if (!suffixes.isEmpty()) {
            sb.append(", ").append(suffixes);
        }
        return sb.toString();
    }

    // decodes secret key from application.yml
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
