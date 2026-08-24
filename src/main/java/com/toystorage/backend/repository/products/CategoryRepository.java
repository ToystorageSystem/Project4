package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository
        extends JpaRepository<Categories, Long> {

    Optional<Categories> findByIdAndStatus(
            Long id,
            CommonStatus status
    );
}