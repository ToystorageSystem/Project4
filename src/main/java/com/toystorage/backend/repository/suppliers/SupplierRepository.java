package com.toystorage.backend.repository.suppliers;

import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository
        extends JpaRepository<Suppliers, Long> {

    List<Suppliers> findByStatusOrderByNameAsc(
            CommonStatus status
    );
}