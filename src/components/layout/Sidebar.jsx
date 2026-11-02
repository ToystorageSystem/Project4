import { useEffect, useState } from "react";
import {
    useLocation,
    useNavigate,
} from "react-router-dom";


function MenuIcon({
                      children,
                      active = false,
                  }) {
    return (
        <span
            className={
                "flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl transition " +
                (
                    active
                        ? "bg-[#f25d19] text-white"
                        : "bg-slate-100 text-slate-500 group-hover:bg-white group-hover:text-[#f25d19]"
                )
            }
        >
            {children}
        </span>
    );
}


/**
 * Menu mặc định của Warehouse.
 *
 * Nếu nơi khác truyền items riêng vào Sidebar
 * thì Sidebar vẫn dùng items được truyền vào.
 */
export const warehouseSidebarItems = [
    {
        key: "dashboard",
        label: "Dashboard",
        path: "/warehouse/dashboard",
        icon: (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-5 w-5"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M4 4h6v6H4V4Zm10 0h6v6h-6V4ZM4 14h6v6H4v-6Zm10 0h6v6h-6v-6Z"
                />
            </svg>
        ),
    },

    {
        key: "receiving",
        label: "Receiving",
        path: "/warehouse/receiving",
        icon: (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-5 w-5"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M4 7h16v13H4V7Zm3-3h10l3 3H4l3-3Zm2 8h6"
                />
            </svg>
        ),
    },

    {
        key: "putaway",
        label: "Putaway",
        path: "/warehouse/putaway",
        icon: (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-5 w-5"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M3 21h18M5 21V8h14v13M8 12h8M8 16h8M9 8V4h6v4"
                />
            </svg>
        ),
    },
];



const BUSINESS_STAFF_SECTIONS = [
    { type: "item", key: "business-staff-dashboard" },
    {
        type: "group",
        id: "catalog",
        label: "Sản phẩm & nhà cung cấp",
        keys: [
            "business-staff-products",
            "business-staff-categories",
            "business-staff-rejected-products",
            "business-staff-suppliers",
        ],
    },
    {
        type: "group",
        id: "purchasing",
        label: "Mua hàng & nhập kho",
        keys: [
            "business-staff-purchase-orders",
            "business-staff-supplier-invoices",
            "business-staff-goods-receipt",
        ],
    },
    {
        type: "group",
        id: "delivery",
        label: "Điều chuyển & giao nhận",
        keys: [
            "business-staff-stock-transfer-create",
            "business-staff-shipment-manifests",
            "business-staff-in-transit-shipments",
        ],
    },
    {
        type: "item",
        key: "business-staff-inventory-report",
    },
];

// Chi thay doi ten hien thi, khong doi route hay quyen.
const STAFF_MENU_LABELS = {
    "business-staff-products": "Danh sách sản phẩm",
    "business-staff-categories": "Danh mục sản phẩm",
    "business-staff-rejected-products": "Yêu cầu bị từ chối",
    "business-staff-suppliers": "Nhà cung cấp",
    "business-staff-purchase-orders": "Đơn mua hàng",
    "business-staff-supplier-invoices": "Hóa đơn nhà cung cấp",
    "business-staff-goods-receipt": "Tạo phiếu nhập kho tổng",
    "business-staff-stock-transfer-create": "Tạo phiếu điều chuyển",
    "business-staff-shipment-manifests": "Bảng kê đi hàng",
    "business-staff-in-transit-shipments": "Hàng đang vận chuyển",
};

function pathMatches(pathname, path) {
    return pathname === path || pathname.startsWith(`${path}/`);
}

export default function Sidebar({
    items = warehouseSidebarItems,
    activeKey,
    onSelect,
    title = "Warehouse",
}) {
    const [collapsed, setCollapsed] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    const visibleItems = items.filter(
        (item) => item && item.path
    );

    const isStaff = title === "Business Staff";

    // Uu tien route cu the nhat de tranh active hai menu.
    const activeItem = activeKey
        ? visibleItems.find((item) => item.key === activeKey)
        : visibleItems
            .filter((item) =>
                pathMatches(location.pathname, item.path)
            )
            .sort((a, b) => b.path.length - a.path.length)[0];

    const activeGroupId = isStaff
        ? BUSINESS_STAFF_SECTIONS.find(
            (section) =>
                section.type === "group" &&
                section.keys.includes(activeItem?.key)
        )?.id ?? null
        : null;

    const [openedGroup, setOpenedGroup] = useState(activeGroupId);

    // Tu mo dung nhom khi chuyen trang.
    useEffect(() => {
        if (isStaff) {
            setOpenedGroup(activeGroupId);
        }
    }, [isStaff, activeGroupId, location.pathname]);

    const handleSelect = (item) => {
        onSelect?.(item);

        if (item.path) {
            navigate(item.path);
        }
    };

    const renderMenuItem = (item, nested = false) => {
        const active = activeItem?.key === item.key;

        const label = isStaff
            ? STAFF_MENU_LABELS[item.key] ?? item.label
            : item.label;

        if (nested) {
            return (
                <button
                    key={item.key}
                    type="button"
                    onClick={() => handleSelect(item)}
                    aria-current={active ? "page" : undefined}
                    title={label}
                    className={
                        "group flex w-full items-center gap-3 " +
                        "rounded-lg border-l-2 px-3 py-2.5 " +
                        "text-left text-[13px] transition-colors " +
                        (active
                            ? "border-[#f25d19] bg-[#fff1e9] font-semibold text-[#d94f12]"
                            : "border-transparent text-slate-600 hover:bg-slate-100 hover:text-slate-900")
                    }
                >
                    <span
                        className={
                            "h-1.5 w-1.5 shrink-0 rounded-full " +
                            (active
                                ? "bg-[#f25d19]"
                                : "bg-slate-300")
                        }
                    />

                    <span className="min-w-0 flex-1 leading-5">
                        {label}
                    </span>
                </button>
            );
        }

        return (
            <button
                key={item.key}
                type="button"
                onClick={() => handleSelect(item)}
                aria-current={active ? "page" : undefined}
                title={collapsed ? label : undefined}
                className={
                    "group flex w-full items-center rounded-xl " +
                    "text-left text-sm font-medium transition-colors " +
                    (collapsed
                        ? "justify-center px-2 py-2"
                        : "gap-3 px-3 py-2.5") +
                    " " +
                    (active
                        ? "bg-[#fff1e9] text-[#d94f12]"
                        : "text-slate-600 hover:bg-slate-100 hover:text-slate-900")
                }
            >
                <MenuIcon active={active}>
                    {item.icon}
                </MenuIcon>

                {!collapsed && (
                    <span className="min-w-0 flex-1 leading-5">
                        {label}
                    </span>
                )}
            </button>
        );
    };

    const renderStaffSection = (section) => {
        if (section.type === "item") {
            const item = visibleItems.find(
                (entry) => entry.key === section.key
            );

            return item ? renderMenuItem(item) : null;
        }

        const children = section.keys
            .map((key) =>
                visibleItems.find((item) => item.key === key)
            )
            .filter(Boolean);

        if (children.length === 0) {
            return null;
        }

        const open = openedGroup === section.id;

        const hasActiveChild = children.some(
            (item) => item.key === activeItem?.key
        );

        return (
            <div key={section.id} className="space-y-1">
                <button
                    type="button"
                    onClick={() => {
                        if (collapsed) {
                            setCollapsed(false);
                            setOpenedGroup(section.id);
                        } else {
                            setOpenedGroup(
                                open ? null : section.id
                            );
                        }
                    }}
                    aria-expanded={!collapsed && open}
                    aria-controls={`staff-group-${section.id}`}
                    title={collapsed ? section.label : undefined}
                    className={
                        "group flex w-full items-center rounded-xl " +
                        "text-left text-sm font-semibold transition-colors " +
                        (collapsed
                            ? "justify-center px-2 py-2"
                            : "gap-3 px-3 py-2.5") +
                        " " +
                        (open || hasActiveChild
                            ? "bg-slate-50 text-slate-800"
                            : "text-slate-600 hover:bg-slate-100 hover:text-slate-900")
                    }
                >
                    <MenuIcon active={false}>
                        {children[0].icon}
                    </MenuIcon>

                    {!collapsed && (
                        <>
                            <span className="min-w-0 flex-1 leading-5">
                                {section.label}
                            </span>

                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                className={
                                    "h-4 w-4 shrink-0 text-slate-400 transition-transform " +
                                    (open ? "rotate-180" : "")
                                }
                                aria-hidden="true"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2"
                                    d="m6 9 6 6 6-6"
                                />
                            </svg>
                        </>
                    )}
                </button>

                {!collapsed && open && (
                    <div
                        id={`staff-group-${section.id}`}
                        role="group"
                        aria-label={section.label}
                        className={
                            "ml-5 space-y-0.5 border-l " +
                            "border-slate-200 pl-3"
                        }
                    >
                        {children.map((item) =>
                            renderMenuItem(item, true)
                        )}
                    </div>
                )}
            </div>
        );
    };

    // Khong an cac chuc nang moi chua duoc phan nhom.
    const assignedKeys = new Set(
        BUSINESS_STAFF_SECTIONS.flatMap((section) =>
            section.type === "item"
                ? [section.key]
                : section.keys
        )
    );

    const otherItems = isStaff
        ? visibleItems.filter(
            (item) => !assignedKeys.has(item.key)
        )
        : [];

    return (
        <aside
            className={
                "hidden min-h-screen self-stretch flex-shrink-0 " +
                "border-r border-slate-200 bg-white " +
                "transition-all duration-200 md:flex md:flex-col " +
                (collapsed ? "w-20" : "w-72")
            }
        >
            {/* Header */}
            <div className="flex items-center justify-between border-b border-slate-100 p-3">
                {!collapsed && (
                    <div className="min-w-0 px-2">
                        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                            Workspace
                        </p>
                        <p className="truncate text-sm font-semibold text-slate-800">
                            {title}
                        </p>
                    </div>
                )}

                <button
                    type="button"
                    onClick={() =>
                        setCollapsed((value) => !value)
                    }
                    className={
                        "ml-auto flex h-9 w-9 items-center " +
                        "justify-center rounded-xl text-slate-500 " +
                        "transition hover:bg-[#fff1e9] hover:text-[#f25d19]"
                    }
                    aria-label={
                        collapsed
                            ? "Expand sidebar"
                            : "Collapse sidebar"
                    }
                    title={
                        collapsed
                            ? "Expand sidebar"
                            : "Collapse sidebar"
                    }
                >
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        className="h-5 w-5"
                        aria-hidden="true"
                    >
                        {collapsed ? (
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth="2"
                                d="m9 18 6-6-6-6"
                            />
                        ) : (
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth="2"
                                d="m15 18-6-6 6-6"
                            />
                        )}
                    </svg>
                </button>
            </div>

            {/* Menu */}
            <nav
                aria-label={`${title} navigation`}
                className="flex-1 space-y-1 overflow-y-auto p-3"
            >
                {isStaff ? (
                    <>
                        {BUSINESS_STAFF_SECTIONS.map(
                            renderStaffSection
                        )}

                        {otherItems.map((item) =>
                            renderMenuItem(item)
                        )}
                    </>
                ) : (
                    visibleItems.map((item) =>
                        renderMenuItem(item)
                    )
                )}
            </nav>

            {/* Footer */}
            <div className="mt-auto border-t border-slate-100 p-3">
                {!collapsed ? (
                    <div className="rounded-xl bg-[#fff8f4] px-3 py-3">
                        <div className="flex items-center gap-2">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                className="h-5 w-5 text-[#f25d19]"
                                aria-hidden="true"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2"
                                    d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6"
                                />
                            </svg>

                            <p className="text-xs font-medium text-[#c94810]">
                                Toy Storage System
                            </p>
                        </div>

                        <p className="mt-2 text-xs leading-5 text-slate-500">
                            Centralized warehouse operations management.
                        </p>
                    </div>
                ) : (
                    <div className="mx-auto h-2 w-2 rounded-full bg-[#f25d19]" />
                )}
            </div>
        </aside>
    );
}
