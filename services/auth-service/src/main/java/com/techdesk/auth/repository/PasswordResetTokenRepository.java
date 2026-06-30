package com.techdesk.auth.repository;

import com.techdesk.auth.entity.PasswordResetToken;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data Redis repository for PasswordResetToken.
 * Token expires automatically via the @TimeToLive TTL (15 minutes).
 */
public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, String> {
}
