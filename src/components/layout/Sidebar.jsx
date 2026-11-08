import { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import WorkspaceIcon, { navigationIcon } from "../ui/WorkspaceIcon";
import { roleLabel, homePath } from "../../auth/access";
export default function Sidebar({ items = [], user }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const { pathname } = useLocation();
  const filtered = items.filter(i => i.label.toLowerCase().includes(query.trim().toLowerCase()));
  const groups = [...new Set(filtered.map((i) => i.group))];
  const active = items
    .filter((i) => pathname === i.path || pathname.startsWith(i.path + "/"))
    .sort((a, b) => b.path.length - a.path.length)[0]?.path;
  return (
    <aside className="workspace-sidebar">
      <div className="sidebar-brand">
        <NavLink className="brand-lockup" to={homePath(user)}>
          <span className="brand-logo">
            <img src="/logo.png" alt="ToyStorage logo" width="46" height="46" />
          </span>
          <span>
            ToyStorage<span className="brand-caption">A little more organised.</span>
          </span>
        </NavLink>
        <button
          className="mobile-menu"
          onClick={() => setOpen(!open)}
          aria-expanded={open}
          aria-controls="workspace-nav"
        >
          <WorkspaceIcon name={open ? "close" : "menu"} />{open ? "Close" : "Menu"}
        </button>
      </div>
      <nav
        id="workspace-nav"
        className={open ? "sidebar-nav is-open" : "sidebar-nav"}
        aria-label="Main navigation"
      >
        <div className="workspace-identity"><span className="identity-avatar">{(user?.name || user?.email || "T").charAt(0).toUpperCase()}</span><div><strong>{user?.name || "My workspace"}</strong><span>{roleLabel(user)}</span></div></div>
        <label className="navigation-search"><WorkspaceIcon name="search" size={17} /><input type="search" aria-label="Find a page" placeholder="Find a page…" value={query} onChange={e => setQuery(e.target.value)} /></label>
        {!filtered.length && <p className="nav-empty" role="status">No pages match “{query}”.</p>}
        {groups.map((group) => (
          <div className="nav-group" key={group}>
            <h2>{group.replace(/^\d+ \/ /, "").toLowerCase()}</h2>
            {filtered
              .filter((i) => i.group === group)
              .map((item) => (
                <NavLink
                  key={item.key}
                  to={item.path}
                  onClick={() => setOpen(false)}
                  className={`nav-item ${active === item.path ? "is-active" : ""}`}
                >
                  <WorkspaceIcon name={navigationIcon(item.key)} size={18} />
                  {item.label}
                  <span className="nav-arrow" aria-hidden="true">
                    <WorkspaceIcon name="arrow" size={15} />
                  </span>
                </NavLink>
              ))}
          </div>
        ))}
      </nav>
      <footer className="sidebar-foot">
        <WorkspaceIcon name="box" size={25} /><span>Small details.<br /><strong>Everything accounted for.</strong></span>
      </footer>
    </aside>
  );
}
