package com.toystorage.backend.services.suppliers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.response.suppliers.SupplierResponse;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupplierAuditService {

    private final ActivityLogRepository
            activityLogRepository;

    private final ObjectMapper
            objectMapper;

    public void record(
            Users actor,
            ActivityAction action,
            Long supplierId,
            SupplierResponse oldValue,
            SupplierResponse newValue,
            String ipAddress,
            String deviceInfo
    ) {

        ActivityLogs activityLog =
                ActivityLogs.builder()

                        .user(actor)

                        .action(action)

                        .entityType(
                                ActivityEntityType.SUPPLIER
                        )

                        .entityId(supplierId)

                        .oldValue(
                                convertToJson(oldValue)
                        )

                        .newValue(
                                convertToJson(newValue)
                        )

                        .ipAddress(ipAddress)

                        .deviceInfo(deviceInfo)

                        .build();

        activityLogRepository.save(
                activityLog
        );
    }

    private String convertToJson(
            SupplierResponse value
    ) {

        if (value == null) {
            return null;
        }

        try {

            return objectMapper
                    .writeValueAsString(value);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Cannot serialize supplier audit data",
                    exception
            );
        }
    }
}