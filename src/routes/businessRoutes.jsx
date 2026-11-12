import { lazy } from "react";
const definitions = [
  ["dashboard", "business/BusinessDashboardPage"],
  ["products", "businessstaff/products/ProductListPage"],
  ["categories", "businessstaff/categories/CategoryListPage"],
  ["products/new", "businessstaff/products/ProductFormPage"],
  ["products/import", "businessstaff/products/ProductImportPage"],
  ["products/:productId/edit", "businessstaff/products/ProductFormPage"],
  ["products/:productId", "businessstaff/products/ProductDetailPage"],
  ["goods-receipts", "businessstaff/goodsreceipts/GoodsReceiptCreatePage"],
  ["goods-receipts/list", "businessstaff/goodsreceipts/GoodsReceiptListPage"],
  ["goods-receipts/:receiptId", "businessstaff/goodsreceipts/GoodsReceiptDetailPage"],
  ["purchase-orders", "businessstaff/purchaseorders/PurchaseOrderListPage"],
  ["purchase-orders/new", "businessstaff/purchaseorders/PurchaseOrderFormPage"],
  ["purchase-orders/:orderId/edit", "businessstaff/purchaseorders/PurchaseOrderFormPage"],
  ["purchase-orders/:orderId", "businessstaff/purchaseorders/PurchaseOrderDetailPage"],
  ["stock-transfers", "businessstaff/stocktransfers/StockTransferListPage"],
  ["stock-transfers/new", "businessstaff/stocktransfers/StockTransferCreatePage"],
  ["stock-transfers/:transferId/edit", "businessstaff/stocktransfers/StockTransferCreatePage"],
  ["stock-transfers/:transferId", "businessstaff/stocktransfers/StockTransferDetailPage"],
  ["inventory-report", "businessstaff/inventoryreport/InventoryReportPage"],
  ["discrepancies", "businessmanager/DiscrepanciesPage"],
  ["low-stock-reports", "businessmanager/LowStockReportsPage"],
];
const modules = import.meta.glob("../pages/{business,businessstaff,businessmanager}/**/*.jsx");
export const businessRoutes = definitions.map(([path, module]) => ({ path:`/business/${path}`, Page:lazy(modules[`../pages/${module}.jsx`]) }));
