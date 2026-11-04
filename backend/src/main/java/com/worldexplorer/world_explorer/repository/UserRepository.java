
// định nghĩa các thao tác truy vấn và làm việc với bảng User trong PostgreSQL.
package com.worldexplorer.world_explorer.repository;

import com.worldexplorer.world_explorer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
