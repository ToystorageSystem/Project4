import { getInventoryReport, getInventoryWarehouseOptions } from "../../api/business/inventoryReport";
import api from "../../api/api";
import DashboardChart from "../../components/dashboard/DashboardCharts";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { formatBusinessStatus } from "../../utils/viStatus";
import { useAuth } from "../../auth/AuthContext";
import { hasAuthority, ROLE } from "../../auth/roleRouting";
import { getBusinessManagerInventory } from "../../api/business/businessDashboard";
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

  </>;
  return to ? <Link className="bo-metric" to={to}>{inner}</Link> : <div className="bo-metric">{inner}</div>;
}

function SectionTitle({ title, subtitle, to, action = "View All" }) {
  return <div className="bo-panel__heading">
    <div><h2>{title}</h2></div>
    {to && <Link to={to} className="bo-subtle-link">{action} <BusinessIcon name="right" size={15} /></Link>}
  </div>;
}

// Read completed sales by store; never derive revenue from stock or purchase orders.
async function fetchStoreRevenue(signal) {
    const options = await getInventoryWarehouseOptions(signal);
    const stores = (Array.isArray(options) ? options : []).filter((s) => String(s.type).toUpperCase() === "STORE");
    const settled = await Promise.allSettled(stores.map((store) =>
        api.get("/analytics/dashboard/sales", { params: { storeId: store.id }, signal })
    ));
    return {
        complete: settled.every((r) => r.status === "fulfilled"),
        rows: settled.flatMap((r, i) => r.status === "fulfilled" ? [{
            storeName: stores[i].name,
            completedRevenue: Number(r.value?.data?.completedRevenue || 0),
            completedOrders: Number(r.value?.data?.completedOrders || 0),
        }] : []),
    };
}

// Inventory Report is the source of truth for available stock and minimum levels.
// Group by warehouse/store + physical barcode so supplier-specific Product Codes
// do not create duplicate alerts for the same physical item.
async function fetchLowStockLocations(signal) {
    const first = await getInventoryReport({ page: 0, size: 100 }, signal);
    const pageCount = Number(first?.totalPages);
    // Do not present a partial list as if it were the full inventory picture.
    if (!Number.isSafeInteger(pageCount) || pageCount < 0 || pageCount > 30) {
        return { complete: false, rows: [] };
    }

    const pages = [first];
    // Limit concurrent API calls to avoid flooding the backend.
    for (let page = 1; page < pageCount; page += 5) {
        const batch = Array.from(
            { length: Math.min(5, pageCount - page) },
            (_, index) => getInventoryReport({ page: page + index, size: 100 }, signal)
        );
        pages.push(...await Promise.all(batch));
    }

    const groups = new Map();
    for (const item of pages.flatMap((result) => Array.isArray(result?.items) ? result.items : [])) {
        if (item.warehouseId == null || !item.productName) continue;
        const available = Number(item.availableQuantity);
        const minimum = Number(item.minimumStockLevel ?? 0);
        const physical = Number(item.onHandQuantity ?? 0);
        if (![available, minimum, physical].every(Number.isFinite)) continue;

        const productKey = String(item.barcode || `product:${item.productId}`);
        const key = JSON.stringify([item.warehouseId, productKey]);
        let group = groups.get(key);
        if (!group) {
            group = {
                key, locationName: item.warehouseName || `Location #${item.warehouseId}`,
                locationType: String(item.warehouseType || '').toUpperCase(),
                productName: item.productName,
                barcode: item.barcode || '',
                available: 0, physical: 0, minimum: 0, zones: new Set(),
            };
            groups.set(key, group);
        }
        group.available += available;
        group.physical += physical;
        group.minimum += Math.max(0, minimum);
        const zone = String(item.zone || item.locationName || '').trim();
        if (zone) group.zones.add(zone);
    }

    const rows = [...groups.values()].map((group) => ({
        ...group,
        zones: [...group.zones].sort(),
        severity: group.available <= 0
            ? (group.physical > 0 ? 'UNAVAILABLE' : 'OUT_OF_STOCK')
            : group.available <= group.minimum ? 'LOW_STOCK' : 'IN_STOCK',
    })).filter((item) => item.severity !== 'IN_STOCK');

    const severityOrder = { OUT_OF_STOCK: 0, UNAVAILABLE: 0, LOW_STOCK: 1 };
    rows.sort((a, b) =>
        severityOrder[a.severity] - severityOrder[b.severity]
        || a.available - b.available
        || a.locationName.localeCompare(b.locationName)
        || a.productName.localeCompare(b.productName)
    );
    return { complete: true, rows };
}

function LowStockAlerts({ result, loading }) {
    const rows = result?.rows || [];
    const criticalCount = rows.filter((row) => row.severity !== 'LOW_STOCK').length;
    const visibleRows = rows.slice(0, 8);

    return <section className="bo-panel" aria-label="Inventory low-stock notifications" aria-live="polite">
        <div className="bo-panel__heading">
            <div>
                <h2 className="flex flex-wrap items-center gap-2">
                    <BusinessIcon name="alert" size={19} /> Low-Stock Alerts by Location
                    {result?.complete && rows.length > 0 && (
                        <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-semibold text-orange-800">
                            {rows.length} alerts
                        </span>
                    )}
                </h2>
                <p>Products running low at each warehouse or store, based on available stock.</p>
            </div>
            <Link to="/business/inventory-report" className="bo-subtle-link">
                Inventory Report <BusinessIcon name="right" size={15} />
            </Link>
        </div>
        {loading && !result ? (
            <p className="px-6 pb-5 text-sm text-slate-500">Checking inventory levels...</p>
        ) : !result ? (
            <p className="px-6 pb-5 text-sm text-amber-800">Low-stock alerts could not be loaded. Check your inventory report or permissions.</p>
        ) : !result.complete ? (
            <p className="px-6 pb-5 text-sm text-amber-800">Inventory data is too large to summarize completely here. Open Inventory Report for the full list.</p>
        ) : rows.length === 0 ? (
            <p className="px-6 pb-5 text-sm text-green-700">No low-stock products found in the locations you can access.</p>
        ) : <div className="px-5 pb-5">
            <div className="mb-3 flex flex-wrap items-center gap-3 text-sm">
                {criticalCount > 0 && <span className="font-semibold text-red-700">{criticalCount} out of stock or unavailable</span>}
                <span className="font-semibold text-amber-700">{rows.length - criticalCount} low stock</span>
            </div>
            <div className={`grid gap-2 ${visibleRows.length > 1 ? "md:grid-cols-2" : "grid-cols-1"}`}>
                {visibleRows.map((row) => {
                    const critical = row.severity !== 'LOW_STOCK';
                    const statusLabel = row.severity === 'UNAVAILABLE' ? 'Unavailable'
                        : row.severity === 'OUT_OF_STOCK' ? 'Out of Stock' : 'Low Stock';
                    return <div key={row.key} className={`rounded-lg border p-3 ${critical ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}>
                        <div className="flex items-start justify-between gap-2">
                            <div className="min-w-0">
                                <p className="text-xs font-semibold text-slate-600">
                                    {row.locationType === 'STORE' ? 'Store' : 'Warehouse'}: {row.locationName}
                                </p>
                                <strong className="mt-1 block break-words text-sm text-slate-900">{row.productName}</strong>
                                {row.barcode && <p className="mt-1 text-xs text-slate-500">Barcode: {row.barcode}</p>}
                                {row.zones.length > 0 && <p className="mt-1 text-xs text-slate-500">Area: {row.zones.slice(0, 3).join(', ')}{row.zones.length > 3 ? '...' : ''}</p>}
                            </div>
                            <span className={`shrink-0 rounded-full px-2 py-1 text-xs font-semibold ${critical ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-800'}`}>
                                {statusLabel}
                            </span>
                        </div>
                        <p className="mt-2 text-xs text-slate-700">
                            Available: <strong>{numberOrDash(row.available)}</strong>
                            <span className="mx-2 text-slate-400">|</span>
                            Minimum: <strong>{numberOrDash(row.minimum)}</strong>
                        </p>
                    </div>;
                })}
            </div>
            {rows.length > visibleRows.length && <p className="mt-3 text-xs text-slate-600">
                Showing {visibleRows.length} of {rows.length} alerts. Open Inventory Report for more details.
            </p>}
        </div>}
    </section>;
}

const moneyVnd = (n) => new Intl.NumberFormat("en-GB", { maximumFractionDigits: 0 }).format(n) + " VND";

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
      requests.push(["products", listProducts({ page: 0, size: 1 }, controller.signal)]);
      requests.push(["activeProducts", listProducts({ status: "ACTIVE", page: 0, size: 1 }, controller.signal)]);
      requests.push(["purchaseOrders", listPurchaseOrders({}, controller.signal)]);
      requests.push(["receipts", getBusinessGoodsReceipts({ page: 0, size: 1 }, controller.signal)]);
      requests.push(["storeRevenue", fetchStoreRevenue(controller.signal)]);
      requests.push(["lowStockLocations", fetchLowStockLocations(controller.signal)]);
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

  const revenueRanked = [...(data.storeRevenue?.rows || [])]
    .sort((a, b) => b.completedRevenue - a.completedRevenue);
  const highest = revenueRanked[0];
  const lowest = revenueRanked.at(-1);

  return (
    <BusinessOperationsLayout>
      <div className="bo-dashboard">
        <div className="bo-page-heading">
          <div><span className="bo-eyebrow">BUSINESS OPERATIONS WORKSPACE</span><h1>Operations Overview</h1>
            </div>
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
        {canBusinessOperations && <LowStockAlerts result={data.lowStockLocations} loading={loading} />}
        {/* Fill the space to the right with cards previously stacked below. */}
        {canBusinessOperations && (
          <div className="grid grid-cols-1 items-start gap-5 lg:grid-cols-2">
            <div className="flex min-w-0 flex-col gap-5">
              <DashboardChart title="Purchase order statuses" type="donut" loading={loading} unavailable={data.purchaseOrders == null} data={Object.entries(orderCounts).map(([label, value]) => ({ label: formatBusinessStatus(label), value }))} />
              <section className="bo-panel">
                <SectionTitle title="Store Revenue Ranking" subtitle="Completed sales, all available dates" />
                {!data.storeRevenue ? <p className="p-4 text-sm">Revenue figures unavailable.</p>
                  : !data.storeRevenue.complete ? <p className="p-4 text-sm">Some stores could not be loaded. Ranking is unavailable.</p>
                  : !highest ? <p className="p-4 text-sm">No active stores are available.</p>
                  : <div className="grid gap-3 p-4 sm:grid-cols-2">
                    <div className="rounded-lg bg-orange-50 p-4"><p className="text-sm">Highest Revenue Store</p><strong>{highest.storeName}</strong><p>{moneyVnd(highest.completedRevenue)}</p></div>
                    <div className="rounded-lg bg-slate-50 p-4"><p className="text-sm">Lowest Revenue Store</p><strong>{lowest.storeName}</strong><p>{moneyVnd(lowest.completedRevenue)}</p></div>
                  </div>}
              </section>
            </div>
            <div className="flex min-w-0 flex-col gap-5">
              <section className="bo-panel bo-worklist">
                <SectionTitle title="Action Required" subtitle="Tasks that require your attention" />
                <div className="bo-worklist__items">
                  {canBusinessOperations && <Link to="/business/purchase-orders" className="bo-work-item">
                    <span className="bo-work-item__icon"><BusinessIcon name="orders" /></span>
                    <span className="bo-work-item__body"><strong>Draft Purchase Orders</strong></span>
                    <span className="bo-work-item__value">{data.purchaseOrders == null ? "—" : numberOrDash(orderCounts.DRAFT || 0)}</span><BusinessIcon name="right" size={15} />
                  </Link>}
                  {canBusinessOperations && <Link to="/business/purchase-orders" className="bo-work-item">
                    <span className="bo-work-item__icon"><BusinessIcon name="orders" /></span>
                    <span className="bo-work-item__body"><strong>Placed Purchase Orders</strong></span>
                    <span className="bo-work-item__value">{data.purchaseOrders == null ? "—" : numberOrDash(orderCounts.ORDERED || 0)}</span><BusinessIcon name="right" size={15} />
                  </Link>}
                  {canBusinessOperations && <Link to="/business/goods-receipts/list" className="bo-work-item">
                    <span className="bo-work-item__icon"><BusinessIcon name="receipts" /></span>
                    <span className="bo-work-item__body"><strong>Track Goods Receipts</strong></span>
                    <BusinessIcon name="right" size={15} />
                  </Link>}
                </div>
              </section>
              <section className="bo-panel bo-inventory-panel">
                <SectionTitle title="Inventory Overview" subtitle="Inventory summary across all warehouses and stores" to="/business/inventory-report" action="Inventory Report" />
                <div className="bo-inventory-stats">
                  <div><span>Physical Stock</span><strong>{numberOrDash(data.inventory?.totalQuantity)}</strong></div>
                  <div><span>Available Stock</span><strong>{numberOrDash(data.inventory?.totalAvailableQuantity)}</strong></div>
                  <div><span>Reserved Stock</span><strong>{numberOrDash(data.inventory?.totalReservedQuantity)}</strong></div>
                </div>
              </section>
            </div>
          </div>
        )}
      </div>
    </BusinessOperationsLayout>
  );
}

