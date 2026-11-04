
package com.worldexplorer.world_explorer.auth.service;

import com.worldexplorer.world_explorer.entity.RefreshToken;
import com.worldexplorer.world_explorer.entity.User;
import com.worldexplorer.world_explorer.repository.RefreshTokenRepository;
import com.worldexplorer.world_explorer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expirationDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${jwt.refresh-token-expiration-days:30}") long expirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.expirationDays = expirationDays;
    }

    // Tạo Refresh Token mới
    @Transactional
    public String createRefreshToken(
            UUID userId,
            String userAgent,
            String ipAddress
    ) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(
                OffsetDateTime.now().plusDays(expirationDays)
        );
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    // Kiểm tra và xoay vòng Refresh Token
    @Transactional
    public RefreshTokenResult rotateRefreshToken(
            String rawToken,
            String userAgent,
            String ipAddress
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token không hợp lệ");
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken existingToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException("Refresh Token không hợp lệ")
                );

        OffsetDateTime now = OffsetDateTime.now();

        // Kiểm tra Refresh Token đã bị thu hồi chưa
        if (existingToken.getRevokedAt() != null) {
            throw new IllegalArgumentException("Refresh Token đã bị thu hồi");
        }

        // Kiểm tra Refresh Token đã hết hạn chưa
        if (!existingToken.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Refresh Token đã hết hạn");
        }

        UUID userId = existingToken.getUserId();

        // Lấy User tương ứng với Refresh Token
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy người dùng")
                );

        // Kiểm tra trạng thái tài khoản
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new IllegalArgumentException(
                    "Tài khoản đã bị vô hiệu hóa hoặc xóa"
            );
        }

        // Thu hồi Refresh Token cũ
        existingToken.setLastUsedAt(now);
        existingToken.setRevokedAt(now);
        refreshTokenRepository.save(existingToken);

        // Tạo Refresh Token mới
        String newRefreshToken = createRefreshToken(
                userId,
                userAgent,
                ipAddress
        );

        return new RefreshTokenResult(userId, newRefreshToken);
    }

    // Hash Refresh Token bằng SHA-256
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể hash Refresh Token", e
            );
        }
    }

    // Kết quả trả về sau khi xoay vòng Refresh Token
    public record RefreshTokenResult(
            UUID userId,
            String refreshToken
    ) {
    }

    // Thu hồi Refresh Token khi người dùng đăng xuất
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token không hợp lệ");
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken existingToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException("Refresh Token không hợp lệ")
                );

        if (existingToken.getRevokedAt() != null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        existingToken.setRevokedAt(now);
        existingToken.setLastUsedAt(now);

        refreshTokenRepository.save(existingToken);
    }
}
