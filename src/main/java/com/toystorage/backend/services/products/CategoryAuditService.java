package com.toystorage.backend.services.products;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryAuditService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public void logCreate(
            Categories category
    ) {

        saveLog(
                ActivityAction.CREATE,
                category.getId(),
                null,
                snapshot(category)
        );
    }

    public void logUpdate(
            Long categoryId,
            Map<String, Object> oldValue,
            Categories category
    ) {

        saveLog(
                ActivityAction.UPDATE,
                categoryId,
                oldValue,
                snapshot(category)
        );
    }

    public Map<String, Object> snapshot(
            Categories category
    ) {

        Map<String, Object> value =
                new LinkedHashMap<>();

        value.put(
                "id",
                category.getId()
        );

        value.put(
                "categoryCode",
                category.getCategoriesCode()
        );

        value.put(
                "name",
                category.getName()
        );

        value.put(
                "description",
                category.getDescription()
        );

        value.put(
                "status",
                category.getStatus() == null
                        ? null
                        : category.getStatus().name()
        );

        return value;
    }

    private void saveLog(
            ActivityAction action,
            Long entityId,
            Map<String, Object> oldValue,
            Map<String, Object> newValue
    ) {

        Users currentUser = getCurrentUser();

        ActivityLogs log =
                ActivityLogs.builder()
                        .user(currentUser)
                        .action(action)
                        .entityType(
                                ActivityEntityType.CATEGORY
                        )
                        .entityId(entityId)
                        .oldValue(
                                toJson(oldValue)
                        )
                        .newValue(
                                toJson(newValue)
                        )
                        .build();

        activityLogRepository.save(log);
    }

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                        authentication.getPrincipal()
                )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new Unauthorized(
                                "Authenticated user was not found"
                        )
                );
    }

    private String toJson(
            Map<String, Object> value
    ) {

        if (value == null) {
            return null;
        }

        try {

            return objectMapper
                    .writeValueAsString(value);

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException(
                    "Could not serialize category audit data",
                    ex
            );
        }
    }
}