package com.toystorage.backend.entity.receipts;

import java.time.LocalDateTime;

import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Store count of ONE product inside ONE received package (F2).
 * Table created by hand: store_package_item_checks_create.sql
 * (UNIQUE package_id + product_id and CHECKs live in the DB, not repeated here).
 */
@Entity
@Table(name = "store_package_item_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorePackageItemChecks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SPIC-{packageId}-{productId} */
    @Column(name = "store_package_item_checks_code", nullable = false, length = 80)
    private String storePackageItemChecksCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_receipt_id", nullable = false)
    private StoreReceipts storeReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private Packages packageEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /** from package_items; 0 = product was not in the package (surplus) */
    @Builder.Default
    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity = 0;

    /** counted by the store, good + damaged */
    @Builder.Default
    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity = 0;

    /** part of actualQuantity */
    @Builder.Default
    @Column(name = "damaged_quantity", nullable = false)
    private Integer damagedQuantity = 0;

    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_by", nullable = false)
    private Users checkedBy;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
}