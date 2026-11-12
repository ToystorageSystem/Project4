package com.toystorage.backend.services.warehouses.location;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocationProduct;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.repository.warehouses.location.WarehouseLocationProductRepository;
import com.toystorage.backend.services.warehouses.manager.ManagerOpsAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Gan san pham cho vi tri / khu vuc kho: khi tao hoac sua vi tri, Manager chon cac san pham de o vi tri do.
 * Dung de nhan vien biet hang nao cat len ke nao, va de xem nhanh vi tri dang chua nhung mat hang nao.
 */
@Service
@RequiredArgsConstructor
public class WarehouseLocationProductService {

    public record ProductRef(Long id, String code, String name, String barcode, String unit) {}

    public record LocationProducts(Long locationId, String locationCode, String zone, String shelf, List<ProductRef> products) {}

    public record AssignRequest(List<Long> productIds) {}

    @PersistenceContext
    private EntityManager em;

    private final WarehouseLocationProductRepository repository;
    private final ManagerOpsAccess access;

    /** San pham dang kinh doanh de chon (tim theo ma, ten, barcode). */
    @Transactional(readOnly = true)
    public List<ProductRef> productOptions(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return em.createQuery("""
                SELECT p FROM Products p WHERE p.status = :s
                  AND (:k = '' OR LOWER(p.name) LIKE CONCAT('%', :k, '%')
                       OR LOWER(p.productsCode) LIKE CONCAT('%', :k, '%') OR p.barcode LIKE CONCAT('%', :k, '%'))
                ORDER BY p.name ASC
                """, Products.class)
                .setParameter("s", ProductStatus.ACTIVE).setParameter("k", k).setMaxResults(200)
                .getResultList().stream().map(WarehouseLocationProductService::ref).toList();
    }

    /** Tat ca vi tri co gan san pham trong kho cua nguoi dang nhap. */
    @Transactional(readOnly = true)
    public List<LocationProducts> forWarehouse() {
        Users user = access.currentWarehouseUser();
        Map<Long, LocationProducts> map = new LinkedHashMap<>();
        for (WarehouseLocationProduct lp : repository.findForWarehouse(user.getWarehouse().getId())) {
            WarehouseLocations l = lp.getLocation();
            map.computeIfAbsent(l.getId(), id -> new LocationProducts(id, l.getWarehouseCode(), l.getZone(), l.getShelf(), new ArrayList<>()))
                    .products().add(ref(lp.getProduct()));
        }
        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<ProductRef> forLocation(Long locationId) {
        location(locationId, access.currentWarehouseUser());
        return repository.findForLocation(locationId).stream().map(lp -> ref(lp.getProduct())).toList();
    }

    /** Thay toan bo danh sach san pham cua vi tri (danh sach rong = bo gan). */
    @Transactional
    public List<ProductRef> replace(Long locationId, AssignRequest request) {
        Users manager = access.currentWarehouseUser();
        WarehouseLocations location = location(locationId, manager);
        Set<Long> ids = new LinkedHashSet<>(request == null || request.productIds() == null ? List.of() : request.productIds());
        ids.remove(null);
        if (ids.size() > 500) throw bad("A location can have at most 500 products");
        List<Products> products = ids.isEmpty() ? List.of() : em.createQuery(
                "SELECT p FROM Products p WHERE p.id IN :ids", Products.class).setParameter("ids", ids).getResultList();
        if (products.size() != ids.size()) throw bad("Some selected products no longer exist");
        repository.deleteForLocation(locationId);
        for (Products p : products) {
            repository.save(WarehouseLocationProduct.builder().location(location).product(p).assignedBy(manager).build());
        }
        return products.stream().map(WarehouseLocationProductService::ref)
                .sorted(Comparator.comparing(ProductRef::name, Comparator.nullsLast(String::compareToIgnoreCase))).toList();
    }

    private WarehouseLocations location(Long id, Users user) {
        WarehouseLocations l = em.find(WarehouseLocations.class, id);
        if (l == null || l.getWarehouse() == null || !l.getWarehouse().getId().equals(user.getWarehouse().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found");
        }
        return l;
    }

    private static ProductRef ref(Products p) {
        return new ProductRef(p.getId(), p.getProductsCode(), p.getName(), p.getBarcode(), p.getBaseUnit());
    }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
