package com.toystorage.backend.services.receipts;

import com.toystorage.backend.dto.request.receipts.CancelPurchaseOrderRequest;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderDetailResponse;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.PurchaseOrderCancellationStatus;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.receipts.PurchaseOrderMapper;
import com.toystorage.backend.repository.receipts.PurchaseOrderCancellationRepository;
import com.toystorage.backend.repository.receipts.PurchaseOrderItemRepository;
import com.toystorage.backend.repository.receipts.PurchaseOrderRepository;
import com.toystorage.backend.repository.suppliers.SupplierProductRepository;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Business Operations cancellation tests use Mockito and never access MySQL. */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderBusinessOperationsCancellationTest {
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock SupplierProductRepository supplierProductRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock UserRepository userRepository;
    @Mock ActivityLogRepository activityLogRepository;
    @Mock PurchaseOrderMapper purchaseOrderMapper;
    @Mock PurchaseOrderCancellationEligibilityService eligibilityService;
    @Mock PurchaseOrderCancellationRepository cancellationRequestRepository;
    @InjectMocks PurchaseOrderService service;

    private final Users actor = Users.builder().id(5L).name("Business Test").build();

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("business@example.com", "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ROLE-BIZ-OPS"))));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private PurchaseOrders setupOrder(PurchaseOrderStatus status, Users creator) {
        PurchaseOrders order = PurchaseOrders.builder().id(10L).orderCode("PO-TEST-10")
                .status(status).createdBy(creator).build();
        when(purchaseOrderRepository.findForCancellationUpdate(10L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("business@example.com")).thenReturn(Optional.of(actor));
        return order;
    }

    private CancelPurchaseOrderRequest request(String reason, boolean supplierAgreed) {
        CancelPurchaseOrderRequest request = new CancelPurchaseOrderRequest();
        request.setReason(reason);
        request.setSupplierAgreed(supplierAgreed);
        return request;
    }

    @Test
    void draftCanBeCancelledWithoutSupplierAgreement() {
        PurchaseOrders order = setupOrder(PurchaseOrderStatus.DRAFT, actor);
        when(purchaseOrderItemRepository.findByPurchaseOrder_IdOrderByIdAsc(10L)).thenReturn(List.of());
        when(purchaseOrderRepository.save(order)).thenReturn(order);
        when(purchaseOrderMapper.toDetailResponse(eq(order), anyList()))
                .thenReturn(PurchaseOrderDetailResponse.builder().id(10L).build());

        var result = service.cancel(10L, request("Wrong product", false));

        assertEquals(10L, result.getId());
        assertEquals(PurchaseOrderStatus.CANCELLED, order.getStatus());
        verify(eligibilityService).assertNoReceivingOrInvoices(order);
        verify(activityLogRepository).save(any());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    void orderedCanBeCancelledDirectlyWithSupplierAgreement() {
        PurchaseOrders order = setupOrder(PurchaseOrderStatus.ORDERED, actor);
        when(purchaseOrderItemRepository.findByPurchaseOrder_IdOrderByIdAsc(10L)).thenReturn(List.of());
        when(purchaseOrderRepository.save(order)).thenReturn(order);
        when(purchaseOrderMapper.toDetailResponse(eq(order), anyList()))
                .thenReturn(PurchaseOrderDetailResponse.builder().id(10L).build());

        var result = service.cancel(10L, request("Supplier confirmed cancellation", true));

        assertEquals(10L, result.getId());
        assertEquals(PurchaseOrderStatus.CANCELLED, order.getStatus());
        verify(activityLogRepository).save(any());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    void orderedCannotCancelWithoutSupplierAgreement() {
        PurchaseOrders order = setupOrder(PurchaseOrderStatus.ORDERED, actor);

        assertThrows(BadRequest.class,
                () -> service.cancel(10L, request("Supplier has not confirmed", false)));

        assertEquals(PurchaseOrderStatus.ORDERED, order.getStatus());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void cannotCancelIfGoodsReceiptOrInvoiceExists() {
        PurchaseOrders order = setupOrder(PurchaseOrderStatus.ORDERED, actor);
        doThrow(new BadRequest("Purchase order has linked goods receipts"))
                .when(eligibilityService).assertNoReceivingOrInvoices(order);

        assertThrows(BadRequest.class,
                () -> service.cancel(10L, request("Supplier confirmed cancellation", true)));

        assertEquals(PurchaseOrderStatus.ORDERED, order.getStatus());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void legacyPendingRequestMustBeReconciled() {
        PurchaseOrders order = setupOrder(PurchaseOrderStatus.ORDERED, actor);
        when(cancellationRequestRepository.existsByPurchaseOrder_IdAndStatus(
                10L, PurchaseOrderCancellationStatus.PENDING)).thenReturn(true);

        assertThrows(BadRequest.class,
                () -> service.cancel(10L, request("Supplier confirmed cancellation", true)));

        assertEquals(PurchaseOrderStatus.ORDERED, order.getStatus());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void anotherBusinessUserCanCancelSomeoneElsesOrderedOrderAndAuditActualActor() {
        Users creator = Users.builder().id(8L).name("Original Creator").build();
        PurchaseOrders order = setupOrder(PurchaseOrderStatus.ORDERED, creator);
        when(purchaseOrderItemRepository.findByPurchaseOrder_IdOrderByIdAsc(10L)).thenReturn(List.of());
        when(purchaseOrderRepository.save(order)).thenReturn(order);
        when(purchaseOrderMapper.toDetailResponse(eq(order), anyList()))
                .thenReturn(PurchaseOrderDetailResponse.builder().id(10L).build());

        PurchaseOrderDetailResponse result = service.cancel(10L,
                request("Supplier confirmed cancellation", true));

        assertEquals(10L, result.getId());
        assertEquals(PurchaseOrderStatus.CANCELLED, order.getStatus());
        assertEquals("Supplier confirmed cancellation", order.getCancelReason());
        assertEquals(8L, order.getCreatedBy().getId());
        ArgumentCaptor<ActivityLogs> logs = ArgumentCaptor.forClass(ActivityLogs.class);
        verify(activityLogRepository).save(logs.capture());
        ActivityLogs audit = logs.getValue();
        assertSame(actor, audit.getUser());
        assertEquals(ActivityAction.CANCEL, audit.getAction());
        assertEquals(ActivityEntityType.PURCHASE_ORDER, audit.getEntityType());
        assertEquals(10L, audit.getEntityId());
        assertTrue(audit.getOldValue().contains("status=ORDERED"));
        assertTrue(audit.getNewValue().contains("status=CANCELLED"));
        assertTrue(audit.getNewValue().contains("supplierCancellationConfirmed=true"));
        verify(eligibilityService).assertNoReceivingOrInvoices(order);
    }

    @Test
    void anotherBusinessUserCanCancelDraftOrder() {
        PurchaseOrders order = setupOrder(PurchaseOrderStatus.DRAFT,
                Users.builder().id(8L).build());
        when(purchaseOrderItemRepository.findByPurchaseOrder_IdOrderByIdAsc(10L)).thenReturn(List.of());
        when(purchaseOrderRepository.save(order)).thenReturn(order);
        when(purchaseOrderMapper.toDetailResponse(eq(order), anyList()))
                .thenReturn(PurchaseOrderDetailResponse.builder().id(10L).build());

        service.cancel(10L, request("Duplicate draft", false));

        assertEquals(PurchaseOrderStatus.CANCELLED, order.getStatus());
        verify(activityLogRepository).save(any(ActivityLogs.class));
    }

    @Test
    void otherRoleCannotCancelEvenWhenSameCreator() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("warehouse@example.com", "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ROLE-WH-STAFF"))));

        assertThrows(Forbidden.class,
                () -> service.cancel(10L, request("Unauthorized", true)));

        verifyNoInteractions(purchaseOrderRepository);
        verifyNoInteractions(activityLogRepository);
    }

    @Test
    void cancellationHistoryReturnsActorAndTimestampFromExistingActivityLogs() {
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 11, 7, 21, 30);
        ActivityLogs entry = ActivityLogs.builder()
                .id(30L).user(actor).action(ActivityAction.CANCEL)
                .entityType(ActivityEntityType.PURCHASE_ORDER)
                .entityId(10L).createdAt(cancelledAt).build();
        when(purchaseOrderRepository.existsById(10L)).thenReturn(true);
        when(activityLogRepository.findByEntityTypeAndEntityIdAndActionOrderByCreatedAtDesc(
                ActivityEntityType.PURCHASE_ORDER, 10L, ActivityAction.CANCEL))
                .thenReturn(List.of(entry));

        var results = service.getCancellationHistory(10L);

        assertEquals(1, results.size());
        assertEquals(30L, results.get(0).id());
        assertEquals(5L, results.get(0).cancelledById());
        assertEquals("Business Test", results.get(0).cancelledByName());
        assertEquals(cancelledAt, results.get(0).cancelledAt());
    }

    @Test
    void cancellationHistoryRejectsUnknownOrder() {
        when(purchaseOrderRepository.existsById(999L)).thenReturn(false);
        assertThrows(NotFound.class, () -> service.getCancellationHistory(999L));
        verifyNoInteractions(activityLogRepository);
    }
}
