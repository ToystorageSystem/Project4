package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import com.toystorage.backend.exceptions.Forbidden;

import com.toystorage.backend.repository.warehouses.WarehouseTaskClaimRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WarehouseTaskClaimService {

    private final WarehouseTaskClaimRepository
            claimRepository;


    // =====================================================
    // CLAIM
    // =====================================================

    @Transactional
    public WarehouseTaskClaim claim(
            WarehouseTaskType taskType,
            Long referenceId,
            Users staff
    ) {

        /*
         * Nếu chính mình claim rồi
         * thì return luôn -> idempotent.
         */
        var existing =
                claimRepository
                        .findByTaskTypeAndReferenceId(
                                taskType,
                                referenceId
                        );


        if (existing.isPresent()) {

            WarehouseTaskClaim claim =
                    existing.get();


            if (claim.getClaimedBy()
                    .getId()
                    .equals(staff.getId())) {

                return claim;
            }


            throw new Forbidden(
                    "Task is being handled by another staff"
            );
        }


        try {

            WarehouseTaskClaim claim =
                    WarehouseTaskClaim
                            .builder()

                            .taskType(
                                    taskType
                            )

                            .referenceId(
                                    referenceId
                            )

                            .claimedBy(
                                    staff
                            )

                            .claimedAt(
                                    LocalDateTime.now()
                            )

                            .build();


            /*
             * saveAndFlush rất quan trọng:
             *
             * DB sẽ kiểm UNIQUE ngay tại đây.
             */
            return claimRepository
                    .saveAndFlush(
                            claim
                    );

        } catch (DataIntegrityViolationException ex) {

            /*
             * Hai Staff cùng INSERT:
             *
             * người đầu tiên insert thành công.
             * người thứ hai dính UNIQUE constraint.
             */

            WarehouseTaskClaim winner =
                    claimRepository
                            .findByTaskTypeAndReferenceId(
                                    taskType,
                                    referenceId
                            )

                            .orElseThrow(() ->
                                    new Forbidden(
                                            "Task has already been claimed"
                                    )
                            );


            if (winner.getClaimedBy()
                    .getId()
                    .equals(staff.getId())) {

                return winner;
            }


            throw new Forbidden(
                    "Task has already been claimed by another staff"
            );
        }
    }


    // =====================================================
    // OWNER CHECK
    // =====================================================

    @Transactional(readOnly = true)
    public void validateOwner(
            WarehouseTaskType taskType,
            Long referenceId,
            Users staff
    ) {

        WarehouseTaskClaim claim =
                claimRepository
                        .findByTaskTypeAndReferenceId(
                                taskType,
                                referenceId
                        )

                        .orElseThrow(() ->
                                new Forbidden(
                                        "Task has not been claimed"
                                )
                        );


        if (!claim.getClaimedBy()
                .getId()
                .equals(staff.getId())) {

            throw new Forbidden(
                    "Task is being handled by another staff"
            );
        }
    }


    // =====================================================
    // COMPLETE / RELEASE
    // =====================================================

    @Transactional
    public void release(
            WarehouseTaskType taskType,
            Long referenceId,
            Users staff
    ) {

        validateOwner(
                taskType,
                referenceId,
                staff
        );


        WarehouseTaskClaim claim =
                claimRepository
                        .findByTaskTypeAndReferenceId(
                                taskType,
                                referenceId
                        )
                        .orElseThrow();


        claim.setReleasedAt(
                LocalDateTime.now()
        );


        claimRepository.save(
                claim
        );
    }
}