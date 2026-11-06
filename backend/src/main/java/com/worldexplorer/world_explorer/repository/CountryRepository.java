
package com.worldexplorer.world_explorer.repository;

import com.worldexplorer.world_explorer.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CountryRepository
        extends JpaRepository<Country, UUID> { // ó chức năng giao tiếp với bảng "Country" trong PostgreSQL, giúp Backend truy vấn, thêm, cập nhật và xóa dữ liệu quốc gia.
}
