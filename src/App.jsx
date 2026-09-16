import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from "react-router-dom";

import { AuthProvider } from "./auth/AuthContext";

import Login from "./pages/auth/Login";
import ReceivingMonitorPage
    from "./pages/warehouses/receiving/ReceivingMonitorPage";
import ReceivingWaitingReviewPage
    from "./pages/warehouses/receiving/ReceivingWaitingReviewPage";
import ReceivingCompletedPage
    from "./pages/warehouses/receiving/ReceivingCompletedPage";
import ReceivingIncidentReportsPage
    from "./pages/warehouses/receiving/ReceivingIncidentReportsPage";
import ReceivingIncidentReportPrintPage
    from "./pages/warehouses/receiving/ReceivingIncidentReportPrintPage";
import WarehouseDashboardPage
    from "./pages/warehouses/dashboard/WarehouseDashboardPage.jsx";
import WarehouseLocationsPage
    from "./pages/warehouses/locations/WarehouseLocationsPage";
import PutawayTasksPage
    from "./pages/warehouses/putaway/PutawayTasksPage";
import PutawayDetailPage
    from "./pages/warehouses/putaway/PutawayDetailPage";
import PackingConfirmationPage
    from "./pages/warehouses/packing/PackingConfirmationPage";
import ShipmentHistoryPage
    from "./pages/warehouses/shipments/ShipmentHistoryPage";
import DispatchHandoverListPage
    from "./pages/warehouses/dispatch/DispatchHandoverListPage";

import DispatchHandoverPage
    from "./pages/warehouses/dispatch/DispatchHandoverPage";
import ShipmentManifestPage
    from "./pages/warehouses/shipments/ShipmentManifestPage";
import PackingConfirmationDetailPage
    from "./pages/warehouses/packing/PackingConfirmationDetailPage";
import DamagedGoodsPage
    from "./pages/warehouses/damagedgoods/DamagedGoodsPage.jsx";

import DamagedGoodsDetailPage
    from "./pages/warehouses/damagedgoods/DamagedGoodsDetailPage.jsx";
export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route
                        path="/login"
                        element={<Login />}
                    />

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
                        element={
                            <PackingConfirmationPage />
                        }
                    />

                    <Route
                        path="/warehouse/packing/:transferId"
                        element={
                            <PackingConfirmationDetailPage />
                        }
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
                        path="/warehouse/damaged-goods"
                        element={
                            <DamagedGoodsPage />
                        }
                    />


                    <Route
                        path="/warehouse/damaged-goods/:reportId"
                        element={
                            <DamagedGoodsDetailPage />
                        }
                    />

                    <Route
                        path="/warehouse/putaway/:taskId"
                        element={<PutawayDetailPage />}
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
                        element={
                            <ShipmentHistoryPage />
                        }
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

                    <Route
                        path="/"
                        element={<Navigate to="/login" replace />}
                    />

                    <Route
                        path="*"
                        element={<Navigate to="/login" replace />}
                    />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
