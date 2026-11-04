
package com.worldexplorer.world_explorer.auth.security;

import com.worldexplorer.world_explorer.entity.User;
import com.worldexplorer.world_explorer.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy Authorization Header
        String authHeader = request.getHeader("Authorization");

        // 2. Kiểm tra Bearer Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3. Xác minh JWT và lấy User ID
            UUID userId = jwtService.extractUserId(token);

            // 4. Tìm User trong database
            User user = userRepository.findById(userId)
                    .orElse(null);

            // 5. Kiểm tra trạng thái tài khoản
            if (user == null
                    || !user.isActive()
                    || user.getDeletedAt() != null) {

                SecurityContextHolder.clearContext();

                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Tài khoản không hợp lệ hoặc đã bị vô hiệu hóa"
                );

                return;
            }

            // 6. Tạo Authentication nếu chưa có
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // 7. Lưu Authentication vào Security Context
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException e) {

            // JWT không hợp lệ hoặc hết hạn
            SecurityContextHolder.clearContext();
        }

        // 8. Tiếp tục Filter Chain
        filterChain.doFilter(request, response);
    }
}
