// xây dựng các điều kiện tìm kiếm và lọc địa điểm du lịch một cách linh hoạt trong Database.
package com.worldexplorer.world_explorer.destination.specification;

import com.worldexplorer.world_explorer.entity.Destination;
import com.worldexplorer.world_explorer.entity.DestinationType;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DestinationSpecification {

    private DestinationSpecification() {
    }

    // Build dynamic destination search and filter conditions
    public static Specification<Destination> build(
            String keyword,
            UUID countryId,
            UUID parentId,
            DestinationType type,
            Boolean isActive
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Search by destination name, slug or description
            if (keyword != null && !keyword.isBlank()) {

                String searchKeyword =
                        "%" + keyword.trim().toLowerCase() + "%";

                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("name")),
                        searchKeyword
                );

                Predicate slugPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("slug")),
                        searchKeyword
                );

                Predicate descriptionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("description")),
                        searchKeyword
                );

                predicates.add(
                        criteriaBuilder.or(
                                namePredicate,
                                slugPredicate,
                                descriptionPredicate
                        )
                );
            }

            // Filter by country ID
            if (countryId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.<UUID>get("countryId"),
                                countryId
                        )
                );
            }

            // Filter by parent destination ID
            if (parentId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.<UUID>get("parentId"),
                                parentId
                        )
                );
            }

            // Filter by destination type
            if (type != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.<DestinationType>get("type"),
                                type
                        )
                );
            }

            // Filter by active status
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
