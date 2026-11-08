import React from "react";

// SVG icons shared by the Business Operations workspace.
const paths = {
  overview: <><rect x="3" y="3" width="8" height="8" rx="1.5"/><rect x="13" y="3" width="8" height="8" rx="1.5"/><rect x="3" y="13" width="8" height="8" rx="1.5"/><rect x="13" y="13" width="8" height="8" rx="1.5"/></>,
  catalog: <><path d="m12 2 9 5-9 5-9-5 9-5Z"/><path d="M3 7v10l9 5 9-5V7M12 12v10"/></>,
  purchasing: <><path d="M4 3h13l3 3v15H4V3Z"/><path d="M17 3v4h3M8 11h8M8 15h8M8 19h5"/></>,
  distribution: <><path d="M3 6h12v12H3zM15 10h4l3 4v4h-7z"/><circle cx="7.5" cy="18" r="2"/><circle cx="18.5" cy="18" r="2"/></>,
  inventory: <><path d="M3 21V8l9-5 9 5v13M3 21h18M8 21v-9h8v9M7 8h10"/></>,
  approvals: <><rect x="5" y="4" width="14" height="18" rx="2"/><path d="M9 4V2h6v2M9 12l2 2 4-4M9 18h6"/></>,
  reports: <><path d="M4 20h16M6 17v-5M11 17V6M16 17V9M20 17V3"/></>,
  chevron: <path d="m6 9 6 6 6-6"/>,
  right: <path d="m9 18 6-6-6-6"/>,
  menu: <><path d="M3 6h18M3 12h18M3 18h18"/></>,
  close: <path d="M5 5 19 19M19 5 5 19"/>,
  logout: <><path d="M10 17l5-5-5-5M15 12H3M13 3h5a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3h-5"/></>,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></>,
  plus: <path d="M12 5v14M5 12h14"/>,
  products: <><path d="m12 2 9 5v10l-9 5-9-5V7l9-5ZM3 7l9 5 9-5M12 12v10"/></>,
  orders: <><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></>,
  receipts: <><path d="M4 7h16v14H4V7ZM8 3h8l2 4H6l2-4ZM8 13l3 3 5-5"/></>,
  alert: <><path d="m12 3 10 18H2L12 3Z"/><path d="M12 9v5M12 17h.01"/></>,
  check: <path d="m5 12 4 4L19 6"/>,
  clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/></>,
  search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></>,
  user: <><circle cx="12" cy="8" r="4"/><path d="M4 21c0-5 3-8 8-8s8 3 8 8"/></>,
  layers: <><path d="m12 2 9 5-9 5-9-5 9-5ZM3 12l9 5 9-5M3 17l9 5 9-5"/></>,
  arrow: <path d="M5 12h14m-6-6 6 6-6 6"/>,
  refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M6 9a7 7 0 0 1 12-2l2 5M4 12l2 5a7 7 0 0 0 12-2"/></>,
};

export default function BusinessIcon({ name, size = 19, className = "", ...props }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
      {...props}
    >
      {paths[name] ?? paths.overview}
    </svg>
  );
}

