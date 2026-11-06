
package com.worldexplorer.world_explorer.repository;

import com.worldexplorer.world_explorer.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CountryRepository
        extends JpaRepository<Country, UUID>,
        JpaSpecificationExecutor<Country> {
}
