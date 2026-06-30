package com.techdesk.auth.repository;

import com.techdesk.auth.entity.PasswordResetToken;
import org.springframework.data.repository.CrudRepository;

// Spring Data Redis repo — token expires automatically via @TimeToLive after 15 minutes
public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, String> {
}
