// Store Manager menu (one page per function). Keys start with "store-manager" so canAccess() uses isStoreManager().
// To add a page: one line here + the same path in src/routes/storeRoutes.jsx.
// The first entry is the Store Manager's home page (see homePath in src/auth/access.js).
export const STORE_NAV = [
  ["store-manager-dashboard", "Overview", "Dashboard", "/store/dashboard"],
  ["store-manager-receiving", "Store receiving", "Incoming deliveries", "/store/receiving"],
  ["store-manager-low-shelf", "Store shelves", "Restock shelves", "/store/low-shelf"],
  ["store-manager-shelves", "Store shelves", "Shelves", "/store/shelves"],
  ["store-manager-products", "Store shelves", "Products & stock", "/store/products"],
  ["store-manager-sales", "Store sales", "Sales", "/store/sales"],
  ["store-manager-stock-transactions", "Store sales", "Stock history", "/store/stock-transactions"],
].map(([key, group, label, path]) => ({ key, group, label, path }));
