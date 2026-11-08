import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { formatBusinessStatus } from "../../utils/viStatus";
import { useAuth } from "../../auth/AuthContext";
import { hasAuthority, ROLE } from "../../auth/roleRouting";
import { getBusinessStaffSuppliers, getBusinessManagerInventory } from "../../api/business/businessDashboard";
import { listProducts } from "../../api/business/products";
import { listPurchaseOrders } from "../../api/business/purchaseOrders";
import { getBusinessGoodsReceipts } from "../../api/business/goodsReceipts";
import BusinessOperationsLayout from "../../components/business/BusinessOperationsLayout";
import BusinessIcon from "../../components/business/BusinessIcon";

function numberOrDash(value) {
  const numeric = Number(value);
  return value == null || !Number.isFinite(numeric) ? "—" : new Intl.NumberFormat("en-GB").format(numeric);
}

function toList(value) {
  if (Array.isArray(value)) return value;
  if (Array.isArray(value?.content)) return value.content;
  if (Array.isArray(value?.items)) return value.items;
  if (Array.isArray(value?.data)) return value.data;
  return [];
}

function totalOf(value) {
  if (typeof value?.totalElements === "number") return value.totalElements;
  if (Array.isArray(value)) return value.length;
  if (Array.isArray(value?.items)) return value.items.length;
  return null;
}

function readResult(result) { return result?.status === "fulfilled" ? result.value : null; }
function noticeText(error) {
  if (!error) return "";
  const code = error.response?.status;
  if (code === 401) return "Your session has expired. Please sign in again to refresh the dashboard.";
  if (code === 403) return "Some information is unavailable due to your account permissions.";
  return "Some dashboard data could not be loaded. Other features remain available.";
}

function Metric({ icon, label, value, helper, to }) {
  const inner = <>
    <span className="bo-metric__icon"><BusinessIcon name={icon} size={22} /></span>
    <BusinessIcon name="right" size={16} className="bo-metric__arrow" />
    <span className="bo-metric__label">{label}</span>
    <strong className="bo-metric__value">{numberOrDash(value)}</strong>
    <small>{helper}</small>
  </>;
  return to ? <Link className="bo-metric" to={to}>{inner}</Link> : <div className="bo-metric">{inner}</div>;
}

function SectionTitle({ title, subtitle, to, action = "View All" }) {
  return <div className="bo-panel__heading">
    <div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
    {to && <Link to={to} className="bo-subtle-link">{action} <BusinessIcon name="right" size={15} /></Link>}
  </div>;
}

export default function BusinessDashboardPage() {
  const { user } = useAuth();
  // Một role xem các chức năng nghiệp vụ và dữ liệu liên quan.
  const canBusinessOperations = hasAuthority(user, ROLE.BUSINESS_OPERATIONS);
  const [refreshKey, setRefreshKey] = useState(0);
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState({});
  const [message, setMessage] = useState("");

  const refresh = useCallback(() => setRefreshKey((value) => value + 1), []);

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    setLoading(true);
    setMessage("");

    // Read-only API calls. A failed endpoint must not hide all other widgets.
    const requests = [];
    if (canBusinessOperations) {
      requests.push(["suppliers", getBusinessStaffSuppliers()]);
      requests.push(["products", listProducts({ page: 0, size: 1 }, controller.signal)]);
      requests.push(["activeProducts", listProducts({ status: "ACTIVE", page: 0, size: 1 }, controller.signal)]);
      requests.push(["purchaseOrders", listPurchaseOrders({}, controller.signal)]);
      requests.push(["receipts", getBusinessGoodsReceipts({ page: 0, size: 1 }, controller.signal)]);
    }
    if (canBusinessOperations) {
      requests.push(["inventory", getBusinessManagerInventory()]);
    }

    Promise.allSettled(requests.map(([, promise]) => promise)).then((results) => {
      if (cancelled) return;
      const next = {};
      for (let i = 0; i < requests.length; i += 1) {
        next[requests[i][0]] = readResult(results[i]);
      }
      const rejected = results.find((result) => result.status === "rejected");
      setData(next);
      setMessage(noticeText(rejected?.reason));
    }).finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; controller.abort(); };
  }, [canBusinessOperations, refreshKey]);

  const purchaseOrders = useMemo(() => toList(data.purchaseOrders), [data.purchaseOrders]);
  const orderCounts = useMemo(() => purchaseOrders.reduce((acc, order) => {
    const status = String(order.status || "UNKNOWN").toUpperCase();
    acc[status] = (acc[status] || 0) + 1;
    return acc;
  }, {}), [purchaseOrders]);
  const firstOrders = purchaseOrders.slice(0, 5);
  const quickActions = [
    ...(canBusinessOperations ? [
      { icon: "plus", label: "Create Purchase Order", description: "Create a purchase order for a supplier", path: "/business/purchase-orders/new" },
      { icon: "products", label: "Add Product", description: "Create and save a new product directly", path: "/business/products/new" },
      { icon: "distribution", label: "Create Stock Transfer", description: "Plan inventory distribution", path: "/business/stock-transfers/new" },
      { icon: "receipts", label: "Create Goods Receipt", description: "Record an expected delivery", path: "/business/goods-receipts" },
    ] : []),
  ];

  return (
    <BusinessOperationsLayout>
      <div className="bo-dashboard">
        <div className="bo-page-heading">
          <div><span className="bo-eyebrow">BUSINESS OPERATIONS WORKSPACE</span><h1>Operations Overview</h1>
            <p>Monitor purchasing, inventory movements, and daily operations.</p></div>
          <button type="button" className="bo-refresh" onClick={refresh} disabled={loading}>
            <BusinessIcon name="refresh" size={17} /> {loading ? "Refreshing..." : "Refresh Data"}
          </button>
        </div>
        {message && <div className="bo-dashboard__notice" role="status"><BusinessIcon name="alert" size={18}/>{message}</div>}
        <div className="bo-metrics">
          {canBusinessOperations && <>
            <Metric icon="products" label="Registered Products" value={totalOf(data.products)} helper="Products currently registered in the system" to="/business/products" />
            <Metric icon="orders" label="Purchase Orders" value={totalOf(data.purchaseOrders)} helper="Purchase orders you are authorized to view" to="/business/purchase-orders" />
            <Metric icon="receipts" label="Goods Receipts" value={totalOf(data.receipts)} helper="Goods receipts created in the system" to="/business/goods-receipts/list" />
          </>}
          {canBusinessOperations && <Metric icon="products" label="Active Products" value={totalOf(data.activeProducts)} helper="Products available for use" to="/business/products" />}
        </div>
        <div className="bo-dashboard-grid">
          <section className="bo-panel bo-worklist">
            <SectionTitle title="Action Required" subtitle="Tasks that require your attention" />
            <div className="bo-worklist__items">
              {canBusinessOperations && <Link to="/business/purchase-orders" className="bo-work-item">
                <span className="bo-work-item__icon"><BusinessIcon name="orders" /></span>
                <span className="bo-work-item__body"><strong>Draft Purchase Orders</strong><small>Complete and submit pending draft purchase orders</small></span>
                <span className="bo-work-item__value">{data.purchaseOrders == null ? "—" : numberOrDash(orderCounts.DRAFT || 0)}</span><BusinessIcon name="right" size={15} />
              </Link>}
              {canBusinessOperations && <Link to="/business/purchase-orders" className="bo-work-item">
                <span className="bo-work-item__icon"><BusinessIcon name="orders" /></span>
                <span className="bo-work-item__body"><strong>Placed Purchase Orders</strong><small>Track orders submitted to suppliers and receiving progress</small></span>
                <span className="bo-work-item__value">{data.purchaseOrders == null ? "—" : numberOrDash(orderCounts.ORDERED || 0)}</span><BusinessIcon name="right" size={15} />
              </Link>}
              {canBusinessOperations && <Link to="/business/goods-receipts/list" className="bo-work-item">
                <span className="bo-work-item__icon"><BusinessIcon name="receipts" /></span>
                <span className="bo-work-item__body"><strong>Track Goods Receipts</strong><small>Monitor warehouse receiving progress</small></span>
                <BusinessIcon name="right" size={15} />
              </Link>}
            </div>
          </section>
          <section className="bo-panel bo-quick-actions">
            <SectionTitle title="Quick Actions" subtitle="Frequently used business tasks" />
            <div className="bo-quick-actions__items">
              {quickActions.map((action) => <Link className="bo-quick-action" key={action.path} to={action.path}>
                <span className="bo-quick-action__icon"><BusinessIcon name={action.icon} size={18}/></span>
                <span><strong>{action.label}</strong><small>{action.description}</small></span>
                <BusinessIcon name="right" size={15} />
              </Link>)}
            </div>
          </section>
        </div>
        <div className="bo-dashboard-grid bo-dashboard-grid--secondary">
          {canBusinessOperations && <section className="bo-panel bo-orders-panel">
            <SectionTitle title="Recent Purchase Orders" subtitle="Latest purchase orders in the system" to="/business/purchase-orders" />
            <div className="bo-table-scroll"><table className="bo-data-table">
              <thead><tr><th>Purchase Orders</th><th>Suppliers</th><th>Status</th><th>View</th></tr></thead>
              <tbody>{firstOrders.length ? firstOrders.map((order, index) => (
                <tr key={order.id ?? order.orderCode ?? index}>
                  <td><strong>{order.orderCode || order.purchaseOrderCode || `Purchase Order # ${order.id ?? index + 1}`}</strong></td>
                  <td>{order.supplierName || order.supplier?.name || "—"}</td>
                  <td><span className={`bo-status bo-status--${String(order.status || "unknown").toLowerCase()}`}>{formatBusinessStatus(order.status)}</span></td>
                  <td><Link className="bo-table-link" to={`/business/purchase-orders/${order.id}`}>Details</Link></td>
                </tr>
              )) : <tr><td colSpan={4}>{loading ? "Loading purchase orders..." : "No purchase orders to display."}</td></tr>}</tbody>
            </table></div>
          </section>}
          {canBusinessOperations && <section className="bo-panel bo-inventory-panel">
            <SectionTitle title="Inventory Overview" subtitle="Inventory summary across all warehouses and stores" to="/business/stock-transactions" action="Transactions" />
            <div className="bo-inventory-stats">
              <div><span>Physical Stock</span><strong>{numberOrDash(data.inventory?.totalQuantity)}</strong></div>
              <div><span>Available Stock</span><strong>{numberOrDash(data.inventory?.totalAvailableQuantity)}</strong></div>
              <div><span>Reserved Stock</span><strong>{numberOrDash(data.inventory?.totalReservedQuantity)}</strong></div>
            </div>
            <p className="bo-inventory-note">Inventory balances are synchronized with the system. Goods receipts and transfers do not increase stock at the receiving location until the receiving workflow is completed.</p>
          </section>}
        </div>
      </div>
    </BusinessOperationsLayout>
  );
}

