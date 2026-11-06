
package com.worldexplorer.world_explorer.country.specification;

import com.worldexplorer.world_explorer.entity.Country;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CountrySpecification {

    private CountrySpecification() {}

    // Build dynamic country search and filter conditions
    public static Specification<Country> build(
            String keyword,
            Boolean isActive
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Search by country name, code or slug
            if (keyword != null && !keyword.isBlank()) {

                String searchKeyword =
                        "%" + keyword.trim().toLowerCase() + "%";

                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("name")),
                        searchKeyword
                );

                Predicate codePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("code")),
                        searchKeyword
                );

                Predicate slugPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("slug")),
                        searchKeyword
                );

                predicates.add(
                        criteriaBuilder.or(
                                namePredicate,
                                codePredicate,
                                slugPredicate
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
