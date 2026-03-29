package com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "end_of_engagement_letter_templates")
public class EndOfEngagementLetterTemplate {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private java.util.UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> body;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
