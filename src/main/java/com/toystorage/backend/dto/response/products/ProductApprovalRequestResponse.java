package com.toystorage.backend.dto.response.products;

import com.fasterxml.jackson.databind.JsonNode;
import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import com.toystorage.backend.enums.products.ProductChangeType;

import java.time.LocalDateTime;

public class ProductApprovalRequestResponse {

    private Long id;
    private String requestCode;

    /**
     * CREATE / UPDATE / DEACTIVATE
     */
    private ProductChangeType requestType;

    /**
     * PENDING / APPROVED / REJECTED
     */
    private ProductChangeRequestStatus status;

    private Long productId;
    private String productCode;
    private String productName;

    /**
     * Dữ liệu trước khi thay đổi.
     *
     * CREATE thường sẽ là null.
     */
    private JsonNode oldValue;

    /**
     * Dữ liệu Business Staff yêu cầu thay đổi.
     */
    private JsonNode newValue;

    private String requestReason;
    private String rejectionReason;

    private Long createdById;
    private String createdByName;

    private Long approvedById;
    private String approvedByName;

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public ProductApprovalRequestResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public ProductChangeType getRequestType() {
        return requestType;
    }

    public void setRequestType(ProductChangeType requestType) {
        this.requestType = requestType;
    }

    public ProductChangeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ProductChangeRequestStatus status) {
        this.status = status;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public JsonNode getOldValue() {
        return oldValue;
    }

    public void setOldValue(JsonNode oldValue) {
        this.oldValue = oldValue;
    }

    public JsonNode getNewValue() {
        return newValue;
    }

    public void setNewValue(JsonNode newValue) {
        this.newValue = newValue;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Long getApprovedById() {
        return approvedById;
    }

    public void setApprovedById(Long approvedById) {
        this.approvedById = approvedById;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}