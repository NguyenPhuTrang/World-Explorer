
package com.worldexplorer.world_explorer.auth.service;

import com.worldexplorer.world_explorer.auth.security.GoogleTokenVerifier;
import com.worldexplorer.world_explorer.entity.OAuthAccount;
import com.worldexplorer.world_explorer.entity.OAuthProvider;
import com.worldexplorer.world_explorer.entity.Role;
import com.worldexplorer.world_explorer.entity.RoleName;
import com.worldexplorer.world_explorer.entity.User;
import com.worldexplorer.world_explorer.repository.OAuthAccountRepository;
import com.worldexplorer.world_explorer.repository.RoleRepository;
import com.worldexplorer.world_explorer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GoogleAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;

    public GoogleAuthService(
            GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository,
            RoleRepository roleRepository,
            RefreshTokenService refreshTokenService
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public GoogleLoginResult loginWithGoogle(
            String idToken,
            String userAgent,
            String ipAddress
    ) {

        // 1. Xác minh Google ID Token
        GoogleTokenVerifier.GoogleUserInfo googleUser =
                googleTokenVerifier.verify(idToken);

        // 2. Kiểm tra tài khoản Google đã được liên kết chưa
        OAuthAccount existingAccount = oauthAccountRepository
                .findByProviderAndProviderAccountId(
                        OAuthProvider.GOOGLE,
                        googleUser.googleId()
                )
                .orElse(null);

        User user;

        // 3. Nếu đã liên kết, lấy User tương ứng
        if (existingAccount != null) {

            user = userRepository.findById(existingAccount.getUserId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Không tìm thấy người dùng liên kết với Google"
                            )
                    );

        } else {

            // 4. Nếu chưa liên kết, kiểm tra email đã tồn tại chưa
            user = userRepository
                    .findByEmail(googleUser.email())
                    .orElse(null);

            // 5. Nếu chưa có User, tạo tài khoản mới
            if (user == null) {

                Role customerRole = roleRepository
                        .findByName(RoleName.CUSTOMER)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Không tìm thấy vai trò CUSTOMER"
                                )
                        );

                user = new User();
                user.setEmail(googleUser.email());
                user.setFullName(googleUser.name());
                user.setAvatarUrl(googleUser.pictureUrl());
                user.setRoleId(customerRole.getId());
                user.setActive(true);

                user = userRepository.save(user);
            }

            // 6. Liên kết tài khoản Google với User
            OAuthAccount oauthAccount = new OAuthAccount();
            oauthAccount.setUserId(user.getId());
            oauthAccount.setProvider(OAuthProvider.GOOGLE);
            oauthAccount.setProviderAccountId(googleUser.googleId());

            oauthAccountRepository.save(oauthAccount);
        }

        // 7. Kiểm tra trạng thái tài khoản
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new IllegalArgumentException(
                    "Tài khoản đã bị vô hiệu hóa hoặc xóa"
            );
        }

        // 8. Tạo Refresh Token và lưu hash vào database
        String refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                userAgent,
                ipAddress
        );

        // 9. Trả về User và Refresh Token
        return new GoogleLoginResult(user, refreshToken);
    }

    public User getUserById(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Không tìm thấy người dùng"
                        )
                );
    }

    public record GoogleLoginResult(
            User user,
            String refreshToken
    ) {
    }
}
