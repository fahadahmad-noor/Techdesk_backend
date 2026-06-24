package com.techdesk.auth.repository;

import com.techdesk.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Spring Data Redis repository for RefreshToken.
 * The @Indexed annotation on userId enables findByUserId queries.
 */
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    /**
     * Finds all active refresh tokens for a user.
     * Used during replay attack detection to invalidate ALL sessions.
     */
    List<RefreshToken> findByUserId(String userId);
}
