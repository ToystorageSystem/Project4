package com.toystorage.backend.services.warehouses.taskclaim;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import com.toystorage.backend.exceptions.Forbidden;

import com.toystorage.backend.repository.warehouses.taskclaim.WarehouseTaskClaimRepository;

import lombok.RequiredArgsConstructor;


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
        WarehouseTaskClaim activeClaim =
                claimRepository
                        .findFirstByTaskTypeAndReferenceIdAndReleasedAtIsNull(
                                taskType,
                                referenceId
                        )
                        .orElse(null);


        if (activeClaim != null) {

            /*
             * Chính Staff hiện tại đã claim.
             * Cho phép gọi API lại mà không tạo attempt mới.
             */
            if (activeClaim
                    .getClaimedBy()
                    .getId()
                    .equals(staff.getId())) {

                return activeClaim;
            }


            throw new Forbidden(
                    "Task is being handled by another staff"
            );
        }

        int nextAttemptNo =
                claimRepository
                        .findFirstByTaskTypeAndReferenceIdOrderByAttemptNoDesc(
                                taskType,
                                referenceId
                        )
                        .map(previousClaim ->
                                previousClaim.getAttemptNo() + 1
                        )
                        .orElse(1);

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

                            .attemptNo(
                                    nextAttemptNo
                            )

                            .build();


            return claimRepository
                    .saveAndFlush(
                            claim
                    );
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
                        .findFirstByTaskTypeAndReferenceIdAndReleasedAtIsNull(
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
    @Transactional(readOnly = true)
    public WarehouseTaskClaim getActiveClaim(
            WarehouseTaskType taskType,
            Long referenceId
    ) {

        return claimRepository
                .findFirstByTaskTypeAndReferenceIdAndReleasedAtIsNull(
                        taskType,
                        referenceId
                )
                .orElse(null);
    }
    // =====================================================
    // COMPLETE / RELEASE
    // =====================================================
    @Transactional(readOnly = true)
    public WarehouseTaskClaim getLatestClaim(
            WarehouseTaskType taskType,
            Long referenceId
    ) {

        return claimRepository
                .findFirstByTaskTypeAndReferenceIdOrderByAttemptNoDesc(
                        taskType,
                        referenceId
                )
                .orElse(null);
    }
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
                        .findFirstByTaskTypeAndReferenceIdAndReleasedAtIsNull(
                                taskType,
                                referenceId
                        )
                        .orElseThrow(() ->
                                new Forbidden(
                                        "Active task claim was not found"
                                )
                        );
        claim.setReleasedAt(
                LocalDateTime.now()
        );
        claimRepository.save(
                claim
        );
    }
}