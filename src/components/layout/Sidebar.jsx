import { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";

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
  title = "ToyStorage",
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();
  const order = [
    "Overview",
    "Inbound",
    "Outbound",
    "Exceptions",
    "Warehouse",
    "History & reports",
  ];
  const groups = [
    ...new Set(
      items
        .filter((item) => item?.path)
        .map((item) => item.group || "Workspace"),
    ),
  ].sort((a, b) => order.indexOf(a) - order.indexOf(b));
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

  const activePath = items
    .filter(
      (item) =>
        item?.path &&
        (location.pathname === item.path ||
          location.pathname.startsWith(`${item.path}/`)),
    )
    .sort((a, b) => b.path.length - a.path.length)[0]?.path;
  return (
    <aside
      className={`workspace-sidebar border-b border-slate-200 bg-white md:sticky md:top-0 md:h-screen md:shrink-0 md:border-r md:border-b-0 print:hidden ${collapsed ? "md:w-20" : "md:w-60"}`}
    >
      {/* HEADER */}
      <div className="flex h-16 items-center justify-between gap-2 border-b border-slate-100 px-4">
        <NavLink
          to="/warehouse/dashboard"
          aria-label="ToyStorage dashboard"
          className="flex min-w-0 items-center gap-2.5"
        >
          <span
            aria-hidden="true"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[#f25d19] text-sm font-extrabold text-white"
          >
            T
          </span>
          <span
            className={`text-base font-bold tracking-tight ${collapsed ? "md:hidden" : ""}`}
          >
            {title}
          </span>
        </NavLink>
        <button
          type="button"
          onClick={() => setCollapsed(!collapsed)}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          className="hidden rounded-md p-1 text-slate-400 hover:bg-slate-100 md:block"
        >
          {collapsed ? "›" : "‹"}
        </button>
        <button
          type="button"
          aria-expanded={mobileOpen}
          aria-controls="warehouse-navigation"
          onClick={() => setMobileOpen(!mobileOpen)}
          className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm md:hidden"
        >
          {mobileOpen ? "Close menu" : "Menu"}
        </button>
      </div>
      {/* MENU */}
      <nav
        id="warehouse-navigation"
        aria-label="Warehouse navigation"
        className={`${mobileOpen ? "block" : "hidden"} max-h-[70vh] overflow-y-auto p-3 md:block md:max-h-[calc(100vh-110px)]`}
      >
        {groups.map((group) => (
          <div key={group} className="mb-4 last:mb-0">
            <p
              className={`mb-1.5 px-3 text-[10px] font-bold uppercase tracking-[0.12em] text-slate-400 ${collapsed ? "md:sr-only" : ""}`}
            >
              {group}
            </p>
            {items
              .filter(
                (item) => item?.path && (item.group || "Workspace") === group,
              )
              .map((item) => {
                const active = activeKey
                  ? item.key === activeKey
                  : item.path === activePath;
                return (
                  <NavLink
                    key={item.key || item.path}
                    to={item.path}
                    end={item.path !== activePath}
                    title={item.label}
                    onClick={() => {
                      onSelect?.(item);
                      setMobileOpen(false);
                    }}
                    className={`mb-0.5 flex items-center gap-3 rounded-lg px-3 py-2 text-[13px] font-medium transition-colors ${active ? "bg-orange-50 text-orange-700" : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"} ${collapsed ? "md:justify-center md:px-1" : ""}`}
                  >
                    <span aria-hidden="true" className="shrink-0">
                      {item.icon}
                    </span>
                    <span className={collapsed ? "md:sr-only" : "truncate"}>
                      {item.label}
                    </span>
                  </NavLink>
                );
              })}
          </div>
        ))}
      </nav>
      {/* FOOTER */}
      <div
        className={`hidden border-t border-slate-100 px-5 py-3 text-[10px] font-medium uppercase tracking-widest text-slate-400 md:block ${collapsed ? "text-center" : ""}`}
      >
        {collapsed ? "TSS" : "Warehouse operations"}
      </div>
    </aside>
  );
}
