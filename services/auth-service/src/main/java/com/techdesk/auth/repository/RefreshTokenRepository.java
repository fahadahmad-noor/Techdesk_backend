package com.techdesk.auth.repository;

import com.techdesk.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

// Spring Data Redis repo — @Indexed on userId enables the findByUserId query for session revocation
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    List<RefreshToken> findByUserId(String userId);
}
