package com.toystorage.backend.repository.stores;

import com.toystorage.backend.entity.stores.Stores;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Stores, Long> {}
