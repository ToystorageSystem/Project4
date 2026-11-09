package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository
        extends JpaRepository<Categories, Long> {

    Optional<Categories> findByIdAndStatus(
            Long id,
            CommonStatus status
    );

    boolean existsByCategoriesCodeIgnoreCase(
            String categoriesCode
    );

    boolean existsByCategoriesCodeIgnoreCaseAndIdNot(
            String categoriesCode,
            Long categoryId
    );

    boolean existsByNameIgnoreCase(
            String name
    );

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long categoryId
    );

    List<Categories> findByStatusOrderByNameAsc(
            CommonStatus status
    );

    @Query("""
            SELECT c
            FROM Categories c
            WHERE (:status IS NULL OR c.status = :status)
              AND (
                    :keyword IS NULL
                    OR LOWER(c.categoriesCode)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(c.name)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Categories> search(
            @Param("keyword") String keyword,
            @Param("status") CommonStatus status,
            Pageable pageable
    );
}