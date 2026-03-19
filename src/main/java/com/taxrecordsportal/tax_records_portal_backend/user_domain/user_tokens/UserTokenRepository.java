package com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {

    @EntityGraph(attributePaths = {"user"})
    Optional<UserToken> findByTokenAndType(String token, TokenType type);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM UserToken ut WHERE ut.user = :user AND ut.type = :type")
    void deleteByUserAndType(User user, TokenType type);
}
