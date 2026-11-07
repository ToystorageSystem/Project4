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
  return value == null || !Number.isFinite(numeric) ? "—" : new Intl.NumberFormat("vi-VN").format(numeric);
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
  if (code === 401) return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để làm mới trang tổng quan.";
  if (code === 403) return "Một số thông tin không hiển thị do quyền truy cập của tài khoản.";
  return "Không tải được một phần dữ liệu tổng quan. Bạn vẫn có thể sử dụng các chức năng khác.";
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

function SectionTitle({ title, subtitle, to, action = "Xem tất cả" }) {
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
      { icon: "plus", label: "Tạo đơn mua hàng", description: "Lập đơn mua hàng mới gửi nhà cung cấp", path: "/business/purchase-orders/new" },
      { icon: "products", label: "Thêm sản phẩm", description: "Tạo sản phẩm mới và lưu trực tiếp", path: "/business/products/new" },
      { icon: "distribution", label: "Tạo phiếu điều chuyển", description: "Lập kế hoạch phân phối hàng tồn kho", path: "/business/stock-transfers/new" },
      { icon: "receipts", label: "Tạo phiếu nhập kho", description: "Ghi nhận đợt giao hàng dự kiến", path: "/business/goods-receipts" },
    ] : []),
  ];

  return (
    <BusinessOperationsLayout>
      <div className="bo-dashboard">
        <div className="bo-page-heading">
          <div><span className="bo-eyebrow">KHÔNG GIAN NGHIỆP VỤ</span><h1>Tổng quan hoạt động</h1>
            <p>Theo dõi mua hàng, luân chuyển tồn kho và công việc kinh doanh mỗi ngày.</p></div>
          <button type="button" className="bo-refresh" onClick={refresh} disabled={loading}>
            <BusinessIcon name="refresh" size={17} /> {loading ? "Đang làm mới..." : "Làm mới dữ liệu"}
          </button>
        </div>
        {message && <div className="bo-dashboard__notice" role="status"><BusinessIcon name="alert" size={18}/>{message}</div>}
        <div className="bo-metrics">
          {canBusinessOperations && <>
            <Metric icon="products" label="Sản phẩm đã đăng ký" value={totalOf(data.products)} helper="Các sản phẩm hiện có trong hệ thống" to="/business/products" />
            <Metric icon="orders" label="Đơn mua hàng" value={totalOf(data.purchaseOrders)} helper="Các đơn mua hàng bạn có quyền xem" to="/business/purchase-orders" />
            <Metric icon="receipts" label="Phiếu nhập kho" value={totalOf(data.receipts)} helper="Các phiếu nhập kho đã được lập" to="/business/goods-receipts/list" />
          </>}
          {canBusinessOperations && <Metric icon="products" label="Sản phẩm đang hoạt động" value={totalOf(data.activeProducts)} helper="Sản phẩm có thể sử dụng ngay" to="/business/products" />}
        </div>
        <div className="bo-dashboard-grid">
          <section className="bo-panel bo-worklist">
            <SectionTitle title="Công việc cần xử lý" subtitle="Các công việc cần bạn xử lý" />
            <div className="bo-worklist__items">
              {canBusinessOperations && <Link to="/business/purchase-orders" className="bo-work-item">
                <span className="bo-work-item__icon"><BusinessIcon name="orders" /></span>
                <span className="bo-work-item__body"><strong>Đơn mua hàng nháp</strong><small>Hoàn thiện và gửi các đơn mua hàng còn ở trạng thái nháp</small></span>
                <span className="bo-work-item__value">{data.purchaseOrders == null ? "—" : numberOrDash(orderCounts.DRAFT || 0)}</span><BusinessIcon name="right" size={15} />
              </Link>}
              {canBusinessOperations && <Link to="/business/purchase-orders" className="bo-work-item">
                <span className="bo-work-item__icon"><BusinessIcon name="orders" /></span>
                <span className="bo-work-item__body"><strong>Đơn mua hàng đã đặt</strong><small>Theo dõi đơn đã gửi nhà cung cấp và tiến độ nhận hàng</small></span>
                <span className="bo-work-item__value">{data.purchaseOrders == null ? "—" : numberOrDash(orderCounts.ORDERED || 0)}</span><BusinessIcon name="right" size={15} />
              </Link>}
              {canBusinessOperations && <Link to="/business/goods-receipts/list" className="bo-work-item">
                <span className="bo-work-item__icon"><BusinessIcon name="receipts" /></span>
                <span className="bo-work-item__body"><strong>Theo dõi phiếu nhập kho</strong><small>Kiểm tra tiến độ nhận hàng của kho</small></span>
                <BusinessIcon name="right" size={15} />
              </Link>}
            </div>
          </section>
          <section className="bo-panel bo-quick-actions">
            <SectionTitle title="Thao tác nhanh" subtitle="Các thao tác nghiệp vụ thường dùng" />
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
            <SectionTitle title="Đơn mua hàng gần đây" subtitle="Những đơn mua hàng mới nhất trong hệ thống" to="/business/purchase-orders" />
            <div className="bo-table-scroll"><table className="bo-data-table">
              <thead><tr><th>Đơn mua hàng</th><th>Nhà cung cấp</th><th>Trạng thái</th><th>Xem</th></tr></thead>
              <tbody>{firstOrders.length ? firstOrders.map((order, index) => (
                <tr key={order.id ?? order.orderCode ?? index}>
                  <td><strong>{order.orderCode || order.purchaseOrderCode || `Đơn mua hàng số ${order.id ?? index + 1}`}</strong></td>
                  <td>{order.supplierName || order.supplier?.name || "—"}</td>
                  <td><span className={`bo-status bo-status--${String(order.status || "unknown").toLowerCase()}`}>{formatBusinessStatus(order.status)}</span></td>
                  <td><Link className="bo-table-link" to={`/business/purchase-orders/${order.id}`}>Chi tiết</Link></td>
                </tr>
              )) : <tr><td colSpan={4}>{loading ? "Đang tải đơn mua hàng..." : "Chưa có đơn mua hàng để hiển thị."}</td></tr>}</tbody>
            </table></div>
          </section>}
          {canBusinessOperations && <section className="bo-panel bo-inventory-panel">
            <SectionTitle title="Tổng quan tồn kho" subtitle="Tổng hợp số lượng tồn kho trên toàn hệ thống" to="/business/stock-transactions" action="Giao dịch" />
            <div className="bo-inventory-stats">
              <div><span>Số lượng tồn thực tế</span><strong>{numberOrDash(data.inventory?.totalQuantity)}</strong></div>
              <div><span>Số lượng khả dụng</span><strong>{numberOrDash(data.inventory?.totalAvailableQuantity)}</strong></div>
              <div><span>Số lượng đã giữ chỗ</span><strong>{numberOrDash(data.inventory?.totalReservedQuantity)}</strong></div>
            </div>
            <p className="bo-inventory-note">Số liệu tồn kho được cập nhật từ hệ thống. Phiếu nhập và điều chuyển không tự làm tăng tồn kho tại điểm nhận cho đến khi quy trình nhận hàng hoàn tất.</p>
          </section>}
        </div>
      </div>
    </BusinessOperationsLayout>
  );
}
