function DashboardIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" d="M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z" />
        </svg>
    );
}

function SupplierIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M3 7h18v13H3zM6 7V4h12v3M7 11h10M7 15h6" />
        </svg>
    );
}

function ProductIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M4 7 12 3l8 4v10l-8 4-8-4V7Zm0 0 8 4 8-4M12 11v10" />
        </svg>
    );
}

function GoodsReceiptIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M3 8h18v12H3V8ZM6 8V4h12v4M8 13h8M12 10v6M7 17h10" />
        </svg>
    );
}

function PurchaseOrderIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M5 3h12l3 3v15H5V3Zm12 0v4h3M8 11h9M8 15h9M8 19h6" />
        </svg>
    );
}

function SupplierInvoiceIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M5 3h12l3 3v15H5V3Zm12 0v4h3M8 11h9M8 15h9M8 19h5" />
        </svg>
    );
}

function StockTransferIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M4 7h10l-2.5-2.5M14 7l-2.5 2.5M20 17H10l2.5-2.5M10 17l2.5 2.5M4 11v9h7M20 13V4h-7" />
        </svg>
    );
}
function InventoryReportIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
            aria-hidden="true"
        >
            <path
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M4 20V10h4v10M10 20V5h4v15M16 20v-8h4v8M3 20h18"
            />
        </svg>
    );
}

// Only add entries once their pages exist.
export const businessStaffMenuItems = [
    {
        key: "business-staff-dashboard",
        label: "Overview",
        path: "/business/dashboard",
        icon: <DashboardIcon />,
    },
    {
        key: "business-staff-suppliers",
        label: "Suppliers",
        path: "/business/suppliers",
        icon: <SupplierIcon />,
    },
    {
        key: "business-staff-products",
        label: "Product Management",
        path: "/business/products",
        icon: <ProductIcon />,
    },
    {
        key: "business-staff-categories",
        label: "Product Categories",
        path: "/business/categories",
        icon: <ProductIcon />,
    },
    {
        key: "business-staff-purchase-orders",
        label: "Purchase Orders",
        path: "/business/purchase-orders",
        icon: <PurchaseOrderIcon />,
    },
    {
        key: "business-staff-supplier-invoices",
        label: "Supplier Invoices",
        path: "/business/supplier-invoices",
        icon: <SupplierInvoiceIcon />,
    },
    {
        key: "business-staff-stock-transfer-list",
        label: "My Stock Transfers",
        path: "/business/stock-transfers",
        icon: <StockTransferIcon />,
    },
    {
        key: "business-staff-stock-transfer-create",
        label: "Create Stock Transfer",
        path: "/business/stock-transfers/new",
        icon: <StockTransferIcon />,
    },
    {
    key: "business-staff-inventory-report",
    label: "Inventory Report",
    path: "/business/inventory-report",
    icon: <InventoryReportIcon />,
    },
    {
    key: "business-staff-shipment-manifests",
    label: "Shipment Manifests",
    path: "/business/shipment-manifests",
    icon: <StockTransferIcon />,
    },
    {
        key: "business-staff-in-transit-shipments",
        label: "Shipment Tracking",
        path: "/business/in-transit-shipments",
        icon: <StockTransferIcon />,
    },
    {
        key: "business-staff-goods-receipt-list",
        label: "Track Goods Receipts",
        path: "/business/goods-receipts/list",
        icon: <GoodsReceiptIcon />,
    },
    {
        key: "business-staff-goods-receipt",
        label: "Create Goods Receipt for Main Warehouse",
        path: "/business/goods-receipts",
        icon: <GoodsReceiptIcon />,
    },
];

export const businessManagerMenuItems = [
    {
        key: "business-manager-dashboard",
        label: "Overview",
        path: "/business/dashboard",
        icon: <DashboardIcon />,
    },
    {
        key: "business-manager-stock-transactions",
        label: "Track Outbound / Inbound Receipts",
        path: "/business/stock-transactions",
        icon: <StockTransferIcon />,
    },
    {
        key: "business-manager-activity-reports",
        label: "View Activity Reports",
        path: "/business/activity-reports",
        icon: <InventoryReportIcon />,
    },
    {
        key: "business-manager-purchase-orders",
        label: "Purchase Order List",
        path: "/business/purchase-orders",
        icon: <PurchaseOrderIcon />,
    },
    
    {
        key: "business-manager-discrepancies",
        label: "Resolve Discrepancies",
        path: "/business/discrepancies",
        icon: <StockTransferIcon />,
    },

];
