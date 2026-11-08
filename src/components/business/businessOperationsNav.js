import { ROLE, hasAuthority } from "../../auth/roleRouting";

// Menu da gop role. Backend van kiem tra quyen tren tung API.
export const BUSINESS_NAV = [
  { title: "Overview", icon: "overview", links: [
    { label: "Dashboard", path: "/business/dashboard" },
  ] },
  { title: "Products & Suppliers", icon: "catalog", links: [
    { label: "Products", path: "/business/products" },
    { label: "Categories", path: "/business/categories" },
    { label: "Suppliers", path: "/business/suppliers" },
  ] },
  { title: "Purchasing", icon: "purchasing", links: [
    { label: "Purchase Orders", path: "/business/purchase-orders" },
    { label: "Supplier Invoices", path: "/business/supplier-invoices" },
    { label: "Goods Receipts", path: "/business/goods-receipts/list" },
    { label: "Create Goods Receipt", path: "/business/goods-receipts" },
  ] },
  { title: "Distribution", icon: "distribution", links: [
    { label: "Stock Transfers", path: "/business/stock-transfers" },
    { label: "Shipment Manifests", path: "/business/shipment-manifests" },
    { label: "In-Transit Shipments", path: "/business/in-transit-shipments" },
  ] },
  { title: "Inventory", icon: "inventory", links: [
    { label: "Inventory Report", path: "/business/inventory-report" },
    { label: "Inventory Transactions", path: "/business/stock-transactions" },
    { label: "Discrepancy Reports", path: "/business/discrepancies" },
  ] },
  { title: "Reports", icon: "reports", links: [
    { label: "Operational Activity", path: "/business/activity-reports" },
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
  if (pathname.includes("/new")) return { section: "Workspace", page: "Create New" };
  return { section: "Workspace", page: "Business Operations" };
}
