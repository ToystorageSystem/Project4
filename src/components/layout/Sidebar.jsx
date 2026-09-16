import { useState } from "react";

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


export default function Sidebar({
                                    items = warehouseSidebarItems,
                                    activeKey,
                                    onSelect,
                                    title = "Warehouse",
                                }) {
    const [collapsed, setCollapsed] =
        useState(false);

    const navigate =
        useNavigate();

    const location =
        useLocation();


    const isItemActive = (item) => {
        if (
            activeKey &&
            item?.key === activeKey
        ) {
            return true;
        }

        if (!item?.path) {
            return false;
        }

        if (
            location.pathname ===
            item.path
        ) {
            return true;
        }

        /*
         * Ví dụ:
         *
         * menu:
         * /warehouse/putaway
         *
         * detail:
         * /warehouse/putaway/15
         *
         * Putaway vẫn active.
         */
        return location.pathname.startsWith(
            `${item.path}/`
        );
    };


    const handleSelect = (item) => {
        onSelect?.(item);

        if (item?.path) {
            navigate(item.path);
        }
    };


    const visibleItems =
        items.filter(
            (item) =>
                item &&
                item.path
        );


    return (
        <aside
            className={
                "hidden min-h-screen self-stretch flex-shrink-0 border-r border-slate-200 bg-white transition-all duration-200 md:flex md:flex-col " +
                (
                    collapsed
                        ? "w-20"
                        : "w-72"
                )
            }
        >

            {/* HEADER */}

            <div className="flex items-center justify-between border-b border-slate-100 p-3">

                {!collapsed && (
                    <div className="min-w-0 px-2">

                        <p className="truncate text-xs font-semibold uppercase tracking-wider text-slate-400">
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
                        setCollapsed(
                            (value) => !value
                        )
                    }
                    className="ml-auto flex h-9 w-9 items-center justify-center rounded-xl text-slate-500 transition hover:bg-[#fff1e9] hover:text-[#f25d19]"
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

                    {collapsed ? (

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
                                d="m9 18 6-6-6-6"
                            />
                        </svg>

                    ) : (

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
                                d="m15 18-6-6 6-6"
                            />
                        </svg>

                    )}

                </button>

            </div>


            {/* MENU */}

            <nav className="flex-1 space-y-1 overflow-y-auto p-3">

                {visibleItems.map(
                    (
                        item,
                        index
                    ) => {

                        const active =
                            isItemActive(item);

                        return (
                            <button
                                key={
                                    item?.key ??
                                    index
                                }
                                type="button"
                                onClick={() =>
                                    handleSelect(item)
                                }
                                title={
                                    collapsed
                                        ? item?.label
                                        : undefined
                                }
                                className={
                                    "group flex w-full items-center rounded-xl text-left text-sm font-medium transition " +

                                    (
                                        collapsed
                                            ? "justify-center px-2 py-2"
                                            : "gap-3 px-3 py-2.5"
                                    ) +

                                    " " +

                                    (
                                        active
                                            ? "bg-[#fff1e9] text-[#d94f12]"
                                            : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                                    )
                                }
                            >

                                <MenuIcon
                                    active={active}
                                >
                                    {item.icon}
                                </MenuIcon>


                                {!collapsed && (
                                    <span className="truncate">
                                        {item.label}
                                    </span>
                                )}

                            </button>
                        );
                    }
                )}

            </nav>


            {/* FOOTER */}

            <div className="mt-auto border-t border-slate-100 p-3">

                {!collapsed ? (

                    <div className="rounded-xl bg-[#fff8f4] px-3 py-3">

                        <div className="flex items-center gap-2">

                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                className="h-5 w-5 text-[#f25d19]"
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