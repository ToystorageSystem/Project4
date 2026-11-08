const paths = {
  users: "M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M2 21v-3a6 6 0 0 1 12 0v3 M17 4a4 4 0 0 1 0 8 M18 15a5 5 0 0 1 4 5v1",
  shield: "m12 3 8 3v5c0 5-3 8-8 10-5-2-8-5-8-10V6z m-4 9 3 3 5-6",
  dashboard: "M3 3h7v7H3z M14 3h7v4h-7z M14 11h7v10h-7z M3 14h7v7H3z",
  inbound: "M12 3v12 m-4-4 4 4 4-4 M4 16v5h16v-5",
  outbound: "M12 16V4 m-4 4 4-4 4 4 M4 16v5h16v-5",
  box: "m3 7 9-4 9 4v10l-9 4-9-4z M3 7l9 4 9-4 M12 11v10 M7 5l10 4",
  inventory: "M3 3h18v18H3z M3 9h18 M3 15h18 M9 3v18 M15 3v18",
  check: "m5 12 4 4L19 6",
  returns: "M8 4 3 9l5 5 M3 9h12a6 6 0 0 1 0 12h-3",
  list: "M9 5h12 M9 12h12 M9 19h12 M3 5h1 M3 12h1 M3 19h1",
  store: "M3 10V5h18v5 M3 10a3 3 0 0 0 6 0 3 3 0 0 0 6 0 3 3 0 0 0 6 0 M5 13v8h14v-8 M10 21v-6h4v6",
  alert: "m12 3 10 18H2z M12 9v5 M12 17h.01",
  arrow: "M5 12h14 m-5-5 5 5-5 5",
  refresh: "M20 7v5h-5 M4 17v-5h5 M6 6a8 8 0 0 1 13 2 M5 16a8 8 0 0 0 13 2",
  search: "M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16 M17 17l5 5",
  pin: "m9 3 6 0-1 6 4 4v2H6v-2l4-4z M12 15v7",
  sunrise: "M3 17h18 M6 13a6 6 0 0 1 12 0 M12 2v3 M3 7l2 2 M21 7l-2 2 M7 21h10",
  sun: "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10 M12 2v2 M12 20v2 M2 12h2 M20 12h2 M5 5l1.5 1.5 M17.5 17.5l1.5 1.5 M5 19l1.5-1.5 M17.5 6.5l1.5-1.5",
  moon: "M20.5 14A9 9 0 0 1 10 3.5 9 9 0 1 0 20.5 14Z",
  logout: "M9 3H4v18h5 M10 12h11 m-5-5 5 5-5 5",
  menu: "M4 6h16 M4 12h16 M4 18h16",
  close: "m6 6 12 12 M6 18 18 6",
};
export function navigationIcon(key) {
  if (key.endsWith("dashboard")) return "dashboard";
  if (key.includes("users") || key.includes("supplier")) return "users";
  if (key.includes("roles") || key.includes("alerts")) return "shield";
  if (key.includes("logs") || key.includes("reports")) return "list";
  if (key.includes("return")) return "returns";
  if (key.includes("damage") || key === "discrepancies" || key.includes("incident")) return "alert";
  if (key.includes("count")) return "list";
  if (key === "stores") return "store";
  if (key === "inventory" || key.includes("location") || key.includes("putaway")) return "inventory";
  if (key.includes("receiv")) return "inbound";
  if (key.includes("pack")) return "box";
  return "outbound";
}
export default function WorkspaceIcon({ name = "box", size = 20, ...props }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name] || paths.box} /></svg>;
}
