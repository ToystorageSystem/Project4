import { ROLE, hasAuthority } from "../../auth/roleRouting";

// Menu da gop role. Backend van kiem tra quyen tren tung API.
export const BUSINESS_NAV = [
  { title: "Tổng quan", icon: "overview", links: [
    { label: "Trang tổng quan", path: "/business/dashboard" },
  ] },
  { title: "Sản phẩm và nhà cung cấp", icon: "catalog", links: [
    { label: "Sản phẩm", path: "/business/products" },
    { label: "Danh mục", path: "/business/categories" },
    { label: "Nhà cung cấp", path: "/business/suppliers" },
  ] },
  { title: "Mua hàng", icon: "purchasing", links: [
    { label: "Đơn mua hàng", path: "/business/purchase-orders" },
    { label: "Hóa đơn nhà cung cấp", path: "/business/supplier-invoices" },
    { label: "Phiếu nhập kho", path: "/business/goods-receipts/list" },
    { label: "Tạo phiếu nhập kho", path: "/business/goods-receipts" },
  ] },
  { title: "Phân phối hàng", icon: "distribution", links: [
    { label: "Phiếu điều chuyển", path: "/business/stock-transfers" },
    { label: "Bảng kê giao hàng", path: "/business/shipment-manifests" },
    { label: "Hàng đang vận chuyển", path: "/business/in-transit-shipments" },
  ] },
  { title: "Tồn kho", icon: "inventory", links: [
    { label: "Báo cáo tồn kho", path: "/business/inventory-report" },
    { label: "Giao dịch tồn kho", path: "/business/stock-transactions" },
    { label: "Chênh lệch hàng hóa", path: "/business/discrepancies" },
  ] },
  { title: "Báo cáo", icon: "reports", links: [
    { label: "Hoạt động nghiệp vụ", path: "/business/activity-reports" },
  ] },
];

// Chi can 1 role cho toan bo khu vuc, khong can filter tung menu theo role cu.
export function getBusinessNavigation(user) {
  return hasAuthority(user, ROLE.BUSINESS_OPERATIONS) ? BUSINESS_NAV : [];
}

export function resolveBusinessLocation(pathname, user) {
  const sections = getBusinessNavigation(user);
  const all = sections.flatMap((section) => section.links.map((link) => ({ ...link, section: section.title })));
  // A detail route should match the more specific parent route first.
  const match = all
    .filter((link) => pathname === link.path || pathname.startsWith(`${link.path}/`))
    .sort((a, b) => b.path.length - a.path.length)[0];
  if (match) return { section: match.section, page: match.label };
  if (pathname.includes("/new")) return { section: "Không gian làm việc", page: "Tạo mới" };
  return { section: "Không gian làm việc", page: "Nghiệp vụ kinh doanh" };
}
