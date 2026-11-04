
package com.worldexplorer.world_explorer.auth.controller;

import com.worldexplorer.world_explorer.auth.dto.AuthResponse;
import com.worldexplorer.world_explorer.auth.dto.GoogleLoginRequest;
import com.worldexplorer.world_explorer.auth.dto.RefreshTokenRequest;
import com.worldexplorer.world_explorer.auth.dto.UserProfileResponse;
import com.worldexplorer.world_explorer.auth.security.JwtService;
import com.worldexplorer.world_explorer.auth.service.GoogleAuthService;
import com.worldexplorer.world_explorer.auth.service.RefreshTokenService;
import com.worldexplorer.world_explorer.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            GoogleAuthService googleAuthService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.googleAuthService = googleAuthService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    // Google Login
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest
    ) {

        GoogleAuthService.GoogleLoginResult result =
                googleAuthService.loginWithGoogle(
                        request.getIdToken(),
                        httpRequest.getHeader("User-Agent"),
                        httpRequest.getRemoteAddr()
                );

        User user = result.user();

        String accessToken =
                jwtService.generateAccessToken(user.getId());

        AuthResponse response = new AuthResponse(
                accessToken,
                result.refreshToken(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl()
        );

        return ResponseEntity.ok(response);
    }

    // Refresh Access Token
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshAccessToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {

        RefreshTokenService.RefreshTokenResult result =
                refreshTokenService.rotateRefreshToken(
                        request.getRefreshToken(),
                        httpRequest.getHeader("User-Agent"),
                        httpRequest.getRemoteAddr()
                );

        User user = googleAuthService.getUserById(result.userId());

        String accessToken =
                jwtService.generateAccessToken(user.getId());

        AuthResponse response = new AuthResponse(
                accessToken,
                result.refreshToken(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl()
        );

        return ResponseEntity.ok(response);
    }

    // Get current user
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            Authentication authentication
    ) {

        UUID userId = (UUID) authentication.getPrincipal();

        User user = googleAuthService.getUserById(userId);

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl()
        );

        return ResponseEntity.ok(response);
    }


    // Logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        refreshTokenService.revokeRefreshToken(
                request.getRefreshToken()
        );

        return ResponseEntity.noContent().build();
    }

}
