package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.warehouses.WorkRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Vai tro cua mot nhan vien trong mot ngay lam viec (mot nhan vien co the co nhieu vai tro). */
@Entity
@Table(name = "staff_shift_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_ssa_staff_date_role", columnNames = {"staff_id", "work_date", "work_role"}),
        indexes = @Index(name = "idx_ssa_warehouse_date", columnList = "warehouse_id, work_date"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffShiftAssignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Users staff;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_role", nullable = false, length = 20)
    private WorkRole workRole;

    @Column(name = "note", length = 300)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private Users assignedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
