package com.toystorage.backend.mapper.products;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.response.products.ProductApprovalRequestResponse;
import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.ProductChangeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductApprovalMapper {

    private final ObjectMapper objectMapper;


    // =====================================================
    // ENTITY -> RESPONSE
    // =====================================================

    public ProductApprovalRequestResponse toResponse(
            ProductChangeRequests changeRequest
    ) {

        ProductApprovalRequestResponse response =
                new ProductApprovalRequestResponse();

        Products product =
                changeRequest.getProduct();

        Users createdBy =
                changeRequest.getCreatedBy();

        Users approvedBy =
                changeRequest.getApprovedBy();


        response.setId(
                changeRequest.getId()
        );

        response.setRequestCode(
                changeRequest.getProductChangeRequestsCode()
        );

        response.setRequestType(
                resolveRequestType(changeRequest)
        );

        response.setStatus(
                changeRequest.getStatus()
        );


        // =================================================
        // PRODUCT
        // =================================================

        if (product != null) {

            response.setProductId(
                    product.getId()
            );

            response.setProductCode(
                    product.getProductsCode()
            );

            response.setProductName(
                    product.getName()
            );
        }


        // =================================================
        // OLD / NEW VALUE
        // =================================================

        response.setOldValue(
                parseJson(
                        changeRequest.getOldValue()
                )
        );

        response.setNewValue(
                parseJson(
                        changeRequest.getNewValue()
                )
        );


        response.setRequestReason(
                changeRequest.getRequestReason()
        );

        response.setRejectionReason(
                changeRequest.getRejectionReason()
        );


        // =================================================
        // CREATED BY
        // =================================================

        if (createdBy != null) {

            response.setCreatedById(
                    createdBy.getId()
            );

            response.setCreatedByName(
                    createdBy.getName()
            );
        }


        // =================================================
        // APPROVED BY
        // =================================================

        if (approvedBy != null) {

            response.setApprovedById(
                    approvedBy.getId()
            );

            response.setApprovedByName(
                    approvedBy.getName()
            );
        }


        response.setCreatedAt(
                changeRequest.getCreatedAt()
        );

        response.setApprovedAt(
                changeRequest.getApprovedAt()
        );


        return response;
    }


    // =====================================================
    // RESOLVE REQUEST TYPE
    // =====================================================

    public ProductChangeType resolveRequestType(
            ProductChangeRequests changeRequest
    ) {

        String oldValue =
                changeRequest.getOldValue();

        String newValue =
                changeRequest.getNewValue();


        /*
         * Không có dữ liệu cũ
         * -> sản phẩm mới.
         */
        if (
                oldValue == null
                        || oldValue.isBlank()
        ) {

            return ProductChangeType.CREATE;
        }


        JsonNode oldNode =
                parseJson(oldValue);

        JsonNode newNode =
                parseJson(newValue);


        /*
         * ACTIVE -> INACTIVE
         *
         * được xem là yêu cầu ẩn sản phẩm.
         */
        if (
                newNode != null
                        && newNode.hasNonNull("status")
                        && "INACTIVE".equalsIgnoreCase(
                                newNode
                                        .get("status")
                                        .asText()
                        )
        ) {

            String oldStatus =
                    oldNode != null
                            && oldNode.hasNonNull("status")
                            ? oldNode
                                    .get("status")
                                    .asText()
                            : null;

            if (
                    oldStatus == null
                            || !"INACTIVE".equalsIgnoreCase(
                                    oldStatus
                            )
            ) {

                return ProductChangeType.DEACTIVATE;
            }
        }


        return ProductChangeType.UPDATE;
    }


    // =====================================================
    // JSON STRING -> JSON NODE
    // =====================================================

    private JsonNode parseJson(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }

        try {

            return objectMapper.readTree(
                    value
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Invalid JSON stored in product change request",
                    exception
            );
        }
    }
}