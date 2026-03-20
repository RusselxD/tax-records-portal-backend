package com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "type"})
}, indexes = {
        @Index(name = "idx_user_tokens_token", columnList = "token"),
        @Index(name = "idx_user_tokens_user_id", columnList = "user_id")
})
public class UserToken {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TokenType type;
}