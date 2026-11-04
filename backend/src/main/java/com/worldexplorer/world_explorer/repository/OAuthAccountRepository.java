// ruy vấn và quản lý các tài khoản đăng nhập Google được liên kết với người dùng trong hệ thống

package com.worldexplorer.world_explorer.repository;

import com.worldexplorer.world_explorer.entity.OAuthAccount;
import com.worldexplorer.world_explorer.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository
        extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByProviderAndProviderAccountId(
            OAuthProvider provider,
            String providerAccountId
    );
}
