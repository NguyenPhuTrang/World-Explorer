
package com.worldexplorer.world_explorer.repository;

import com.worldexplorer.world_explorer.entity.Role;
import com.worldexplorer.world_explorer.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
