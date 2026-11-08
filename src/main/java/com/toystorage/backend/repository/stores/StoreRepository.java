package com.toystorage.backend.repository.stores;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.Stores;

public interface StoreRepository extends JpaRepository<Stores, Long> {
}
