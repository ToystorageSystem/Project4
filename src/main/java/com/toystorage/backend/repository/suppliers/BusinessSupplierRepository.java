
package com.toystorage.backend.repository.suppliers;

import com.toystorage.backend.entity.suppliers.Suppliers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BusinessSupplierRepository
        extends JpaRepository<Suppliers, Long> {

    List<Suppliers> findAllByOrderByNameAsc();
}
