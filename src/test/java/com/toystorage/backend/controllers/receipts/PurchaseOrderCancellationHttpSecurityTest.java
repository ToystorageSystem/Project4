
package com.toystorage.backend.controllers.receipts;

import com.toystorage.backend.config.SecurityConfig;
import com.toystorage.backend.config.security.CustomUserDetailsService;
import com.toystorage.backend.dto.request.receipts.CancelPurchaseOrderRequest;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderDetailResponse;
import com.toystorage.backend.services.receipts.PurchaseOrderService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem tra bao mat HTTP cho API huy Purchase Order.
 * Su dung MockMvc va Mockito, khong ket noi MySQL.
 */
@WebMvcTest(PurchaseOrderController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class PurchaseOrderCancellationHttpSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Gia lap Service de khong thay doi Purchase Order that.
    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    // Cung cap dependency cho SecurityConfig trong test.
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ResultActions sendCancellation() throws Exception {

        return mockMvc.perform(
                patch("/api/purchase-orders/{orderId}/cancel", 23L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Supplier confirmed cancellation",
                                  "supplierAgreed": true
                                }
                                """)
        );
    }

    @Test
    @WithMockUser(authorities = "ROLE_ROLE-BIZ-OPS")
    void businessOperationsCanAccessCancellationApi() throws Exception {

        // Chi cap role moi, khong cap Staff hay Manager.
        when(purchaseOrderService.cancel(
                eq(23L),
                any(CancelPurchaseOrderRequest.class)
        )).thenReturn(
                PurchaseOrderDetailResponse.builder()
                        .id(23L)
                        .build()
        );

        sendCancellation()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(23));

        // Kiem tra request da duoc chuyen den Service.
        ArgumentCaptor<CancelPurchaseOrderRequest> captor =
                ArgumentCaptor.forClass(CancelPurchaseOrderRequest.class);

        verify(purchaseOrderService).cancel(eq(23L), captor.capture());

        assertEquals(
                "Supplier confirmed cancellation",
                captor.getValue().getReason()
        );

        assertEquals(
                Boolean.TRUE,
                captor.getValue().getSupplierAgreed()
        );
    }

    @Test
    @WithMockUser(authorities = "ROLE_ROLE-ADMIN")
    void adminCanAccessCancellationApi() throws Exception {

        sendCancellation()
                .andExpect(status().isOk());

        verify(purchaseOrderService).cancel(
                eq(23L),
                any(CancelPurchaseOrderRequest.class)
        );
    }

    @Test
    @WithMockUser(authorities = "ROLE_ROLE-BIZ-STAFF")
    void legacyBusinessStaffCannotCancel() throws Exception {

        sendCancellation()
                .andExpect(status().isForbidden());

        verifyNoInteractions(purchaseOrderService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ROLE-BIZ-MANAGER")
    void legacyBusinessManagerCannotCancel() throws Exception {

        sendCancellation()
                .andExpect(status().isForbidden());

        verifyNoInteractions(purchaseOrderService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ROLE-WH-STAFF")
    void warehouseStaffCannotCancel() throws Exception {

        sendCancellation()
                .andExpect(status().isForbidden());

        verifyNoInteractions(purchaseOrderService);
    }

    @Test
    void unauthenticatedUserCannotCancel() throws Exception {

        sendCancellation()
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(purchaseOrderService);
    }
}
