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

// Only add entries once their pages exist.
export const businessStaffMenuItems = [
    {
        key: "business-staff-dashboard",
        label: "Dashboard",
        path: "/business-staff/dashboard",
        icon: <DashboardIcon />,
    },
    {
        key: "business-staff-suppliers",
        label: "Quản lý nhà cung cấp",
        path: "/business-staff/suppliers",
        icon: <SupplierIcon />,
    },
    {
        key: "business-staff-products",
        label: "Quản lý sản phẩm",
        path: "/business-staff/products",
        icon: <ProductIcon />,
    },
    {
        key: "business-staff-purchase-orders",
        label: "Quản lý đơn hàng nhập",
        path: "/business-staff/purchase-orders",
        icon: <PurchaseOrderIcon />,
    },
    {
        key: "business-staff-goods-receipt",
        label: "Tạo phiếu nhập kho tổng",
        path: "/business-staff/goods-receipts",
        icon: <GoodsReceiptIcon />,
    },
];

export const businessManagerMenuItems = [
    {
        key: "business-manager-dashboard",
        label: "Dashboard",
        path: "/business-manager/dashboard",
        icon: <DashboardIcon />,
    },
];
