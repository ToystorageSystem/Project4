import { lazy } from "react";
// Store Manager pages, mounted under <ProtectedRoute access="store-manager"> in App.jsx.
const definitions = [
  ["receiving", "receiving/ReceivingListPage"],
  ["receiving/:code", "receiving/ReceivingDetailPage"],
  ["low-shelf", "shelves/LowShelfPage"],
  ["shelves", "shelves/ShelfListPage"],
  ["shelves/:id", "shelves/ShelfDetailPage"],
  ["products", "products/ProductListPage"],
  ["products/:id", "products/ProductDetailPage"],
  ["sales", "sales/SalesPage"],
  ["stock-transactions", "stock/StockTransactionsPage"],
];
const modules = import.meta.glob("../pages/storemanager/*/*Page.jsx");
export const storeRoutes = definitions.map(([path, module]) => ({
  path: `/store/${path}`,
  Page: lazy(modules[`../pages/storemanager/${module}.jsx`]),
}));
