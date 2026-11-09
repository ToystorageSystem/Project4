package com.toystorage.backend.entity.analytics;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.analytics.AnalysisType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "analytics_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class AnalyticResults {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 50)
    private AnalysisType analysisType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Warehouses store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Lob
    @Column(name = "result_data", nullable = false)
    private String resultData;

    @Column(name = "score", precision = 10, scale = 4)
    private BigDecimal score;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "analytics_results_code", nullable = false, length = 50)
    private String analyticsResultCode;



}
