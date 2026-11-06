//Repository là tầng chịu trách nhiệm giao tiếp với Database.
package com.worldexplorer.world_explorer.repository;

import com.worldexplorer.world_explorer.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface DestinationRepository
    extends JpaRepository<Destination, UUID>, // JpaRepository: cung cấp CRUD, tìm kiếm theo ID, lưu và xóa Destination.
        JpaSpecificationExecutor<Destination> { // JpaSpecificationExecutor: hỗ trợ tìm kiếm động, lọc theo quốc gia, loại địa điểm, trạng thái và phân trang.

}
