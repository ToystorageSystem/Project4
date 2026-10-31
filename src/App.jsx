import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from "react-router-dom";

import { AuthProvider, useAuth } from "./auth/AuthContext";
import RequireRole from "./auth/RequireRole";
import { getHomePath, ROLE } from "./auth/roleRouting";
import BusinessStaffDashboardPage from "./pages/businessstaff/BusinessStaffDashboardPage";
import BusinessManagerDashboardPage from "./pages/businessmanager/BusinessManagerDashboardPage";
import ProductApprovalsPage from "./pages/businessmanager/ProductApprovalsPage";
import StockTransactionsPage from "./pages/businessmanager/StockTransactionsPage";
import BusinessActivityReportsPage from "./pages/businessmanager/BusinessActivityReportsPage";
import PurchaseOrdersPage from "./pages/businessmanager/PurchaseOrdersPage";
import SupplierListPage from "./pages/businessstaff/suppliers/SupplierListPage";
import SupplierDetailPage from "./pages/businessstaff/suppliers/SupplierDetailPage";
import SupplierFormPage from "./pages/businessstaff/suppliers/SupplierFormPage";
import ProductListPage from "./pages/businessstaff/products/ProductListPage";
import CategoryListPage from "./pages/businessstaff/categories/CategoryListPage";
import ProductDetailPage from "./pages/businessstaff/products/ProductDetailPage";
import ProductFormPage from "./pages/businessstaff/products/ProductFormPage";
import ProductImportPage from "./pages/businessstaff/products/ProductImportPage";
import ProductRequestsPage from "./pages/businessstaff/products/ProductRequestsPage";
import RejectedProductsPage from "./pages/businessstaff/products/RejectedProductsPage";
import GoodsReceiptCreatePage from "./pages/businessstaff/goodsreceipts/GoodsReceiptCreatePage";
import GoodsReceiptDetailPage from "./pages/businessstaff/goodsreceipts/GoodsReceiptDetailPage";
import PurchaseOrderListPage from "./pages/businessstaff/purchaseorders/PurchaseOrderListPage";
import PurchaseOrderDetailPage from "./pages/businessstaff/purchaseorders/PurchaseOrderDetailPage";
import PurchaseOrderFormPage from "./pages/businessstaff/purchaseorders/PurchaseOrderFormPage";
import SupplierInvoiceListPage from "./pages/businessstaff/supplierinvoices/SupplierInvoiceListPage";
import SupplierInvoiceDetailPage from "./pages/businessstaff/supplierinvoices/SupplierInvoiceDetailPage";
import SupplierInvoiceFormPage from "./pages/businessstaff/supplierinvoices/SupplierInvoiceFormPage";
import StockTransferCreatePage from "./pages/businessstaff/stocktransfers/StockTransferCreatePage";
import StockTransferDetailPage from "./pages/businessstaff/stocktransfers/StockTransferDetailPage";
import InventoryReportPage from "./pages/businessstaff/inventoryreport/InventoryReportPage";
import ShipmentManifestListPage from "./pages/businessstaff/shipments/ShipmentManifestListPage";
import ShipmentManifestDetailPage from "./pages/businessstaff/shipments/ShipmentManifestDetailPage";
import InTransitShipmentListPage from "./pages/businessstaff/shipments/InTransitShipmentListPage";
import InTransitShipmentDetailPage from "./pages/businessstaff/shipments/InTransitShipmentDetailPage";


import Login from "./pages/auth/Login";
import ReceivingPendingPage from "./pages/warehouses/receiving/ReceivingPendingPage";
import ReceivingMonitorPage from "./pages/warehouses/receiving/ReceivingMonitorPage";
import ReceivingWaitingReviewPage from "./pages/warehouses/receiving/ReceivingWaitingReviewPage";
import ReceivingCompletedPage from "./pages/warehouses/receiving/ReceivingCompletedPage";
import ReceivingIncidentReportsPage from "./pages/warehouses/receiving/ReceivingIncidentReportsPage";
import ReceivingIncidentReportPrintPage from "./pages/warehouses/receiving/ReceivingIncidentReportPrintPage";
import WarehouseDiscrepancyPage from "./pages/warehouses/discrepancies/WarehouseDiscrepancyPage.jsx";
import WarehouseDashboardPage from "./pages/warehouses/dashboard/WarehouseDashboardPage.jsx";
import WarehouseLocationsPage from "./pages/warehouses/locations/WarehouseLocationsPage";
import PutawayTasksPage from "./pages/warehouses/putaway/PutawayTasksPage";
import PutawayDetailPage from "./pages/warehouses/putaway/PutawayDetailPage";
import PackingConfirmationPage from "./pages/warehouses/packing/PackingConfirmationPage";
import PackingConfirmationDetailPage from "./pages/warehouses/packing/PackingConfirmationDetailPage";
import ShipmentHistoryPage from "./pages/warehouses/shipments/ShipmentHistoryPage";
import ShipmentManifestPage from "./pages/warehouses/shipments/ShipmentManifestPage";
import DispatchHandoverListPage from "./pages/warehouses/dispatch/DispatchHandoverListPage";
import DispatchHandoverPage from "./pages/warehouses/dispatch/DispatchHandoverPage";
import DamagedGoodsPage from "./pages/warehouses/damagedgoods/DamagedGoodsPage.jsx";
import DamagedGoodsDetailPage from "./pages/warehouses/damagedgoods/DamagedGoodsDetailPage.jsx";

function HomeRedirect() {
    const { user } = useAuth();
    return <Navigate to={user ? getHomePath(user) : "/login"} replace />;
}

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route path="/login" element={<Login />} />

                    {/* Business routes are checked in the FE and enforced by BE @PreAuthorize. */}
                    <Route element={<RequireRole authority={ROLE.BUSINESS_STAFF} />}>
                        <Route path="/business-staff/dashboard" element={<BusinessStaffDashboardPage />} />
                        <Route path="/business-staff/suppliers" element={<SupplierListPage />} />
                        <Route path="/business-staff/suppliers/new" element={<SupplierFormPage />} />
                        <Route path="/business-staff/suppliers/:supplierId/edit" element={<SupplierFormPage />} />
                        <Route path="/business-staff/suppliers/:supplierId" element={<SupplierDetailPage />} />
                        <Route path="/business-staff/products" element={<ProductListPage />} />
                        <Route
                            path="/business-staff/categories"
                            element={<CategoryListPage />}
                        />
                        <Route path="/business-staff/products/new" element={<ProductFormPage />} />
                        <Route path="/business-staff/products/import" element={<ProductImportPage />} />
                        <Route path="/business-staff/products/requests" element={<ProductRequestsPage />} />
                        <Route path="/business-staff/products/rejected" element={<RejectedProductsPage />} />
                        <Route path="/business-staff/products/:productId/edit" element={<ProductFormPage />} />
                        <Route path="/business-staff/products/:productId" element={<ProductDetailPage />} />
                        <Route path="/business-staff/goods-receipts" element={<GoodsReceiptCreatePage />} />
                        <Route path="/business-staff/goods-receipts/:receiptId" element={<GoodsReceiptDetailPage />} />
                        <Route path="/business-staff/purchase-orders" element={<PurchaseOrderListPage />} />
                        <Route path="/business-staff/purchase-orders/new" element={<PurchaseOrderFormPage />} />
                        <Route path="/business-staff/purchase-orders/:orderId/edit" element={<PurchaseOrderFormPage />} />
                        <Route path="/business-staff/purchase-orders/:orderId" element={<PurchaseOrderDetailPage />} />
                        <Route path="/business-staff/supplier-invoices" element={<SupplierInvoiceListPage />} />
                        <Route path="/business-staff/supplier-invoices/new" element={<SupplierInvoiceFormPage />} />
                        <Route path="/business-staff/supplier-invoices/:invoiceId/edit" element={<SupplierInvoiceFormPage />} />
                        <Route path="/business-staff/supplier-invoices/:invoiceId" element={<SupplierInvoiceDetailPage />} />
                        <Route path="/business-staff/stock-transfers/new" element={<StockTransferCreatePage />} />
                        <Route path="/business-staff/stock-transfers/:transferId/edit" element={<StockTransferCreatePage />} />
                        <Route path="/business-staff/stock-transfers/:transferId" element={<StockTransferDetailPage />} />
                        {/* Issue #13 - In-Transit Shipment Tracking */}
                        <Route
                            path="/business-staff/in-transit-shipments"
                            element={<InTransitShipmentListPage />}
                        />
                        <Route
                            path="/business-staff/in-transit-shipments/:deliveryId"
                            element={<InTransitShipmentDetailPage />}
                        />
                        {/* Issue #14 - Shipment Manifests */}
                        <Route
                            path="/business-staff/shipment-manifests"
                            element={<ShipmentManifestListPage />}
                        />
                        <Route
                            path="/business-staff/shipment-manifests/:manifestId"
                            element={<ShipmentManifestDetailPage />}
                        />
                        {/* Issue #15 - Inventory Report */}
                        <Route
                            path="/business-staff/inventory-report"
                            element={<InventoryReportPage />}
                        />
                    </Route>
                    <Route element={<RequireRole authority={ROLE.BUSINESS_MANAGER} />}>
                        <Route path="/business-manager/dashboard" element={<BusinessManagerDashboardPage />} />
                        <Route path="/business-manager/product-approvals" element={<ProductApprovalsPage />} />
                        <Route path="/business-manager/stock-transactions" element={<StockTransactionsPage />} />
                        <Route path="/business-manager/activity-reports" element={<BusinessActivityReportsPage />} />
                        <Route path="/business-manager/purchase-orders" element={<PurchaseOrdersPage />} />
                    </Route>

                    <Route path="/warehouse/receiving/pending" element={<ReceivingPendingPage />} />

                    <Route
                        path="/warehouse/receiving/in-progress"
                        element={<ReceivingMonitorPage />}
                    />

                    <Route
                        path="/warehouse/receiving/waiting-review"
                        element={<ReceivingWaitingReviewPage />}
                    />

                    <Route
                        path="/warehouse/receiving/completed"
                        element={<ReceivingCompletedPage />}
                    />

                    <Route
                        path="/warehouse/discrepancies"
                        element={<WarehouseDiscrepancyPage />}
                    />

                    <Route
                        path="/warehouse/receiving/incident-reports"
                        element={<ReceivingIncidentReportsPage />}
                    />

                    <Route
                        path="/warehouse/receiving/incident-reports/:id/print"
                        element={<ReceivingIncidentReportPrintPage />}
                    />

                    <Route
                        path="/warehouse/dashboard"
                        element={<WarehouseDashboardPage />}
                    />

                    <Route
                        path="/warehouse/packing"
                        element={<PackingConfirmationPage />}
                    />

                    <Route
                        path="/warehouse/packing/:transferId"
                        element={<PackingConfirmationDetailPage />}
                    />

                    <Route
                        path="/warehouse/receiving"
                        element={
                            <Navigate
                                to="/warehouse/receiving/in-progress"
                                replace
                            />
                        }
                    />

                    <Route
                        path="/warehouse/putaway"
                        element={<PutawayTasksPage />}
                    />

                    <Route
                        path="/warehouse/putaway/:taskId"
                        element={<PutawayDetailPage />}
                    />

                    <Route
                        path="/warehouse/damaged-goods"
                        element={<DamagedGoodsPage />}
                    />

                    <Route
                        path="/warehouse/damaged-goods/:reportId"
                        element={<DamagedGoodsDetailPage />}
                    />

                    <Route
                        path="/warehouse/locations"
                        element={<WarehouseLocationsPage />}
                    />

                    <Route
                        path="/warehouse/manifests/transfer/:transferId"
                        element={<ShipmentManifestPage />}
                    />

                    <Route
                        path="/warehouse/shipment-history"
                        element={<ShipmentHistoryPage />}
                    />

                    <Route
                        path="/warehouse/dispatch"
                        element={<DispatchHandoverListPage />}
                    />

                    <Route
                        path="/warehouse/dispatch/:transferId"
                        element={<DispatchHandoverPage />}
                    />

                    <Route
                        path="/warehouse"
                        element={
                            <Navigate
                                to="/warehouse/dashboard"
                                replace
                            />
                        }
                    />

                    <Route path="/" element={<HomeRedirect />} />

                    <Route
                        path="*"
                        element={<Navigate to="/login" replace />}
                    />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
