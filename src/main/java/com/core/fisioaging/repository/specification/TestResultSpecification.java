package com.core.fisioaging.repository.specification;

import com.core.fisioaging.model.TestResultModel;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TestResultSpecification {

    public static Specification<TestResultModel> filter(
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final String patientName,
            final Long healthUnitId,
            final Integer patientAge
    ) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            var predicates = criteriaBuilder.conjunction();

            predicates = criteriaBuilder.and(
                    predicates,
                    criteriaBuilder.between(root.get("testDateTime"), startDate, endDate)
            );

            // Exclui resultados de testes que foram excluídos (soft delete).
            predicates = criteriaBuilder.and(
                    predicates,
                    criteriaBuilder.isFalse(root.get("testExecution").get("isDeleted"))
            );

            if (patientName != null && !patientName.isBlank()) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("patientName")),
                                "%" + patientName.toLowerCase() + "%"
                        )
                );
            }

            if (healthUnitId != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("healthUnit").get("id"), healthUnitId)
                );
            }

            if (patientAge != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("patientAge"), patientAge)
                );
            }

            return predicates;
        };
    }
}
