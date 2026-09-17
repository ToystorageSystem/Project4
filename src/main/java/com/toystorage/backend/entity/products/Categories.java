package com.toystorage.backend.entity.products;

import com.toystorage.backend.enums.products.CommonStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_categories_name",
                        columnNames = "name"
                ),
                @UniqueConstraint(
                        name = "uk_categories_code",
                        columnNames = "categories_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_categories_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categories {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "categories_code",
            nullable = false,
            length = 50
    )
    private String categoriesCode;

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Lob
    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private CommonStatus status;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = CommonStatus.ACTIVE;
        }
    }
}