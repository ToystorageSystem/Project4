package com.toystorage.backend.repository.suppliers;

import com.toystorage.backend.entity.suppliers.Suppliers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository
        extends JpaRepository<Suppliers, Long> {
}