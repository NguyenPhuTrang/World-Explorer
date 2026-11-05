// phục vụ chức năng Admin User Management, giúp ADMIN có thể tìm kiếm và quản lý danh sách tài khoản.
package com.worldexplorer.world_explorer.user.specification;

import com.worldexplorer.world_explorer.entity.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class UserSpecification {

    private UserSpecification() {
    }

    // Build dynamic user search and filter conditions
    public static Specification<User> build(
            String keyword,
            UUID roleId,
            Boolean isActive
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Search by email or full name
            if (keyword != null && !keyword.isBlank()) {

                String searchKeyword =
                        "%" + keyword.trim().toLowerCase() + "%";

                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("email")),
                        searchKeyword
                );

                Predicate fullNamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("fullName")),
                        searchKeyword
                );

                predicates.add(
                        criteriaBuilder.or(
                                emailPredicate,
                                fullNamePredicate
                        )
                );
            }

            // Filter by role ID
            if (roleId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.<UUID>get("roleId"),
                                roleId
                        )
                );
            }

            // Filter by account status
            if (isActive != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.<Boolean>get("isActive"),
                                isActive
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
