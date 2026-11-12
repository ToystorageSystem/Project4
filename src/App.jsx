import { lazy, Suspense } from "react";
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import ProtectedRoute from "./auth/ProtectedRoute";
import { authPath, homePath } from "./auth/access";
import { businessRoutes } from "./routes/businessRoutes";
import { storeRoutes } from "./routes/storeRoutes";
import AdminPage from "./features/admin/AdminPage";
import MainLayout from "./components/layout/MainLayout";
import WorkspaceWelcome from "./components/layout/WorkspaceWelcome";
import "./integration-theme.css";
const adminModules = import.meta.glob(["./features/admin/*Page.jsx", "!./features/admin/AdminPage.jsx"]);
const AdminOverview = lazy(() => import("./features/admin/AdminOverview"));
const adminRoutes = [
  ["/admin/dashboard", AdminOverview],
  ["/admin/users", lazy(adminModules["./features/admin/UserAccountsPage.jsx"])],
  ["/admin/roles", lazy(adminModules["./features/admin/RolesPermissionsPage.jsx"])],
  ["/admin/warehouses", lazy(adminModules["./features/admin/WarehousesPage.jsx"])],
  ["/admin/activity-logs", lazy(adminModules["./features/admin/ActivityLogsPage.jsx"])],
  ["/admin/security-alerts", lazy(adminModules["./features/admin/SecurityAlertsPage.jsx"])],
];
function HomeRedirect() { const { user } = useAuth(); return <Navigate to={authPath(user)} replace />; }
function LegacyBusinessRedirect() {
  const location = useLocation();
  let path = location.pathname.replace(/^\/business-(staff|manager)(?=\/|$)/, "/business");
  path = path.replace(/^\/business\/product-approvals/, "/business/approvals/products");
  return <Navigate to={path + location.search + location.hash} replace />;
}
function UnassignedWorkspace() { return <MainLayout><WorkspaceWelcome/><section className="ops-panel" style={{padding:24}}><h2>No workspace is assigned to this account.</h2><p>Ask your administrator to check the account's role and permissions.</p></section></MainLayout>; }
function NotFound() { const {user} = useAuth(); return <div className="session-screen"><h1>Page not found</h1><Link to={user ? homePath(user) : "/login"}>Back to my workspace</Link></div>; }
const Fleet = lazy(() => import("./pages/fleet/FleetPage"));
const Login = lazy(() => import("./pages/auth/Login"));
const ReceivingPending = lazy(() => import("./pages/warehouses/receiving/ReceivingPendingPage"));
const Dashboard = lazy(
  () => import("./pages/warehouses/dashboard/WarehouseDashboardPage"),
);
const ReceivingProcess = lazy(
  () => import("./pages/warehouses/staff/StaffReceivingPage"),
);
const ReturnList = lazy(
  () => import("./pages/warehouses/staff/StaffReturnsPage"),
);
const ReturnDetail = lazy(
  () => import("./pages/warehouses/staff/StaffReturnsPage"),
);
const ReturnReview = lazy(
  () => import("./pages/warehouses/store-returns/StoreReturnManagerPage"),
);
const Monitor = lazy(
  () => import("./pages/warehouses/receiving/ReceivingMonitorPage"),
);
const Review = lazy(
  () => import("./pages/warehouses/receiving/ReceivingWaitingReviewPage"),
);
const Completed = lazy(
  () => import("./pages/warehouses/receiving/ReceivingCompletedPage"),
);
const Incidents = lazy(
  () => import("./pages/warehouses/receiving/ReceivingIncidentReportsPage"),
);
const IncidentPrint = lazy(
  () => import("./pages/warehouses/receiving/ReceivingIncidentReportPrintPage"),
);
const Discrepancies = lazy(
  () => import("./pages/warehouses/discrepancies/WarehouseDiscrepancyPage"),
);
const Putaway = lazy(
  () => import("./pages/warehouses/putaway/PutawayTasksPage"),
);
const PutawayDetail = lazy(
  () => import("./pages/warehouses/putaway/PutawayDetailPage"),
);
const Packing = lazy(
  () => import("./pages/warehouses/packing/PackingConfirmationPage"),
);
const PackingDetail = lazy(
  () => import("./pages/warehouses/packing/PackingConfirmationDetailPage"),
);
const Dispatch = lazy(
  () => import("./pages/warehouses/dispatch/DispatchHandoverListPage"),
);
const DispatchDetail = lazy(
  () => import("./pages/warehouses/dispatch/DispatchHandoverPage"),
);
const Shipments = lazy(
  () => import("./pages/warehouses/shipments/ShipmentHistoryPage"),
);
const Manifest = lazy(
  () => import("./pages/warehouses/shipments/ShipmentManifestPage"),
);
const Damage = lazy(
  () => import("./pages/warehouses/damagedgoods/DamagedGoodsPage"),
);
const DamageDetail = lazy(
  () => import("./pages/warehouses/damagedgoods/DamagedGoodsDetailPage"),
);
const Locations = lazy(
  () => import("./pages/warehouses/locations/WarehouseLocationsPage"),
);
const Inventory = lazy(
  () => import("./pages/warehouses/inventory/InventoryPage"),
);
const Counts = lazy(
  () => import("./pages/warehouses/stockcount/StockCountsPage"),
);
const Stores = lazy(() => import("./pages/warehouses/stores/StoresPage"));
const StaffPacking = lazy(
  () => import("./pages/warehouses/staff/StaffPackingPage"),
);
const Trips = lazy(() => import("./pages/warehouses/delivery/TripsPage"));
const StaffDamage = lazy(
  () => import("./pages/warehouses/staff/StaffDamagePage"),
);
const ScanWorkflow = lazy(
  () => import("./pages/warehouses/staff/ScanWorkflowPage"),
);
// Warehouse Manager: daily tasks, staff performance, low-stock report (overview lives on the dashboard).
const DailyTasks = lazy(() => import("./pages/warehouses/manager/DailyTasksPage"));
const StaffPerformance = lazy(() => import("./pages/warehouses/manager/StaffPerformancePage"));
const LowStock = lazy(() => import("./pages/warehouses/manager/LowStockPage"));
const InboundSchedule = lazy(() => import("./pages/warehouses/manager/InboundSchedulePage"));
const MyTasks = lazy(() => import("./pages/warehouses/staff/MyTasksPage"));
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Suspense
          fallback={
            <div className="session-screen" role="status">
              Opening your workspace…
            </div>
          }
        >
          <Routes>
            <Route element={<ProtectedRoute access="fleet" />}><Route path="/fleet" element={<Fleet/>}/></Route>
            <Route element={<ProtectedRoute access="receiving-pending"/>}><Route path="/warehouse/receiving/pending" element={<ReceivingPending/>}/></Route>
            <Route element={<ProtectedRoute access="admin-dashboard" />}>
              <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
              {adminRoutes.map(([path, Page]) => <Route key={path} path={path} element={<AdminPage page={Page} overview={path === "/admin/dashboard"}/>} />)}
            </Route>
            <Route element={<ProtectedRoute access="business-operations" />}>
              <Route path="/business" element={<Navigate to="/business/dashboard" replace />} />
              {businessRoutes.map(({path, Page}) => <Route key={path} path={path} element={<Page/>} />)}
              {[
                ["supplier-invoices", "purchase-orders"],
                ["shipment-manifests", "stock-transfers"],
                ["in-transit-shipments", "stock-transfers"],
              ].map(([from, to]) => <Route key={from} path={`/business/${from}/*`} element={<Navigate to={`/business/${to}`} replace />} />)}
              {["products/requests","products/rejected","approvals/products"].map(path => <Route key={path} path={`/business/${path}`} element={<Navigate to="/business/products" replace/>}/>)}
              <Route path="/business/approvals/purchase-orders" element={<Navigate to="/business/purchase-orders" replace/>}/>
              <Route path="/business-staff/*" element={<LegacyBusinessRedirect/>}/>
              <Route path="/business-manager/*" element={<LegacyBusinessRedirect/>}/>
              <Route path="/business-staff" element={<Navigate to="/business/dashboard" replace/>}/>
              <Route path="/business-manager" element={<Navigate to="/business/dashboard" replace/>}/>
            </Route>
            <Route element={<ProtectedRoute access="store-manager" />}>
              <Route path="/store" element={<Navigate to="/store/receiving" replace />} />
              {storeRoutes.map(({path, Page}) => <Route key={path} path={path} element={<Page/>} />)}
            </Route>
            <Route element={<ProtectedRoute/>}><Route path="/workspace" element={<UnassignedWorkspace/>}/><Route path="/" element={<HomeRedirect/>}/></Route>
            {[
              "/login",
              "/change-password",
              "/2fa/setup",
              "/2fa",
              "/forgot-password",
              "/reset-password",
            ].map((path) => (
              <Route key={path} path={path} element={<Login />} />
            ))}
            <Route element={<ProtectedRoute access="dashboard" />}>
              <Route path="/warehouse/dashboard" element={<Dashboard />} />
            </Route>
            <Route element={<ProtectedRoute access="receiving-process" />}>
              <Route
                path="/warehouse/receiving/process"
                element={<ReceivingProcess />}
              />
            </Route>
            <Route element={<ProtectedRoute access="store-returns" />}>
              <Route path="/warehouse/store-returns" element={<ReturnList />} />
            </Route>
            <Route element={<ProtectedRoute access="store-returns" />}>
              <Route
                path="/warehouse/store-returns/:returnId"
                element={<ReturnDetail />}
              />
            </Route>
            <Route element={<ProtectedRoute access="return-review" />}>
              <Route
                path="/warehouse/store-returns/review"
                element={<ReturnReview />}
              />
            </Route>
            <Route element={<ProtectedRoute access="receiving-in-progress" />}>
              <Route
                path="/warehouse/receiving/in-progress"
                element={<Monitor />}
              />
            </Route>
            <Route
              element={<ProtectedRoute access="receiving-waiting-review" />}
            >
              <Route
                path="/warehouse/receiving/waiting-review"
                element={<Review />}
              />
            </Route>
            <Route element={<ProtectedRoute access="receiving-completed" />}>
              <Route
                path="/warehouse/receiving/completed"
                element={<Completed />}
              />
            </Route>
            <Route element={<ProtectedRoute access="receiving-incidents" />}>
              <Route
                path="/warehouse/receiving/incident-reports"
                element={<Incidents />}
              />
            </Route>
            <Route element={<ProtectedRoute access="receiving-incidents" />}>
              <Route
                path="/warehouse/receiving/incident-reports/:id/print"
                element={<IncidentPrint />}
              />
            </Route>
            <Route element={<ProtectedRoute access="discrepancies" />}>
              <Route
                path="/warehouse/discrepancies"
                element={<Discrepancies />}
              />
            </Route>
            <Route element={<ProtectedRoute access="putaway" />}>
              <Route path="/warehouse/putaway" element={<Putaway />} />
            </Route>
            <Route element={<ProtectedRoute access="putaway" />}>
              <Route
                path="/warehouse/putaway/:taskId"
                element={<PutawayDetail />}
              />
            </Route>
            <Route element={<ProtectedRoute access="packing-confirmation" />}>
              <Route path="/warehouse/packing" element={<Packing />} />
            </Route>
            <Route element={<ProtectedRoute access="packing-confirmation" />}>
              <Route
                path="/warehouse/packing/:transferId"
                element={<PackingDetail />}
              />
            </Route>
            <Route element={<ProtectedRoute access="dispatch-handover" />}>
              <Route path="/warehouse/dispatch" element={<Dispatch />} />
            </Route>
            <Route element={<ProtectedRoute access="dispatch-handover" />}>
              <Route
                path="/warehouse/dispatch/:transferId"
                element={<DispatchDetail />}
              />
            </Route>
            <Route element={<ProtectedRoute access="shipment-history" />}>
              <Route
                path="/warehouse/shipment-history"
                element={<Shipments />}
              />
            </Route>
            <Route element={<ProtectedRoute access="shipment-history" />}>
              <Route
                path="/warehouse/manifests/transfer/:transferId"
                element={<Manifest />}
              />
            </Route>
            <Route element={<ProtectedRoute access="damaged-goods" />}>
              <Route path="/warehouse/damaged-goods" element={<Damage />} />
            </Route>
            <Route element={<ProtectedRoute access="damaged-goods" />}>
              <Route
                path="/warehouse/damaged-goods/:reportId"
                element={<DamageDetail />}
              />
            </Route>
            <Route element={<ProtectedRoute access="warehouse-locations" />}>
              <Route path="/warehouse/locations" element={<Locations />} />
            </Route>
            <Route element={<ProtectedRoute access="inventory" />}>
              <Route path="/warehouse/inventory" element={<Inventory />} />
            </Route>
            <Route element={<ProtectedRoute access="stock-counts" />}>
              <Route path="/warehouse/stock-counts" element={<Counts />} />
            </Route>
            <Route element={<ProtectedRoute access="stores" />}>
              <Route path="/warehouse/stores" element={<Stores />} />
            </Route>
            <Route element={<ProtectedRoute access="staff-packing" />}>
              <Route
                path="/warehouse/staff-packing"
                element={<StaffPacking />}
              />
            </Route>
            <Route element={<ProtectedRoute access="trips" />}>
              <Route path="/warehouse/trips" element={<Trips />} />
            </Route>
            <Route element={<ProtectedRoute access="picking" />}>
              <Route
                path="/warehouse/picking"
                element={<ScanWorkflow key="picking" kind="picking" />}
              />
            </Route>
            <Route element={<ProtectedRoute access="staff-putaway" />}>
              <Route
                path="/warehouse/staff-putaway"
                element={<ScanWorkflow key="putaway" kind="putaway" />}
              />
            </Route>
            <Route element={<ProtectedRoute access="staff-counts" />}>
              <Route
                path="/warehouse/staff-counts"
                element={<ScanWorkflow key="counts" kind="counts" />}
              />
            </Route>
            <Route path="/warehouse/operations" element={<Navigate to="/warehouse/dashboard" replace />} />
            <Route element={<ProtectedRoute access="inbound-schedule" />}>
              <Route path="/warehouse/inbound-schedule" element={<InboundSchedule />} />
            </Route>
            <Route element={<ProtectedRoute access="daily-tasks" />}>
              <Route path="/warehouse/daily-tasks" element={<DailyTasks />} />
            </Route>
            <Route element={<ProtectedRoute access="staff-performance" />}>
              <Route path="/warehouse/performance" element={<StaffPerformance />} />
            </Route>
            <Route element={<ProtectedRoute access="low-stock" />}>
              <Route path="/warehouse/low-stock" element={<LowStock />} />
            </Route>
            <Route element={<ProtectedRoute access="my-tasks" />}>
              <Route path="/warehouse/my-tasks" element={<MyTasks />} />
            </Route>
            <Route element={<ProtectedRoute access="staff-damage" />}>
              <Route path="/warehouse/staff-damage" element={<StaffDamage />} />
            </Route>
            <Route
              path="/warehouse/receiving"
              element={
                <Navigate to="/warehouse/receiving/in-progress" replace />
              }
            />
            <Route
              path="/warehouse"
              element={<Navigate to="/warehouse/dashboard" replace />}
            />
            <Route
              path="*"
              element={<NotFound/>}
            />
          </Routes>
        </Suspense>
      </AuthProvider>
    </BrowserRouter>
  );
}
