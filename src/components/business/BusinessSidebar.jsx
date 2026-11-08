import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { getBusinessNavigation } from "./businessOperationsNav";
import BusinessIcon from "./BusinessIcon";

function currentGroup(pathname, sections) {
  return sections.find((group) => group.links.some((link) => pathname === link.path || pathname.startsWith(`${link.path}/`)))?.title ?? "Overview";
}

export default function BusinessSidebar({ user, open, onClose }) {
  const location = useLocation();
  const sections = getBusinessNavigation(user);
  const active = currentGroup(location.pathname, sections);
  const activeLinkPath = sections.flatMap((group) => group.links)
    .filter((link) => location.pathname === link.path || location.pathname.startsWith(`${link.path}/`))
    .sort((a, b) => b.path.length - a.path.length)[0]?.path;
  const [expanded, setExpanded] = useState(active);

  useEffect(() => { setExpanded(active); }, [active]);

  return (
    <>
      {open && <button className="bo-backdrop" type="button" aria-label="Close navigation menu" onClick={onClose} />}
      <aside className={`bo-sidebar${open ? " bo-sidebar--open" : ""}`} aria-label="Business Operations navigation">
        <Link className="bo-brand" to="/business/dashboard" onClick={onClose}>
          <span className="bo-brand__mark"><img src="/logo.png" alt="Logo ToyStorage" /></span>
          <span>
            <strong className="bo-brand__name">ToyStorage</strong>
            <small className="bo-brand__subtitle">Business Operations</small>
          </span>
        </Link>
        <nav className="bo-sidebar__scroll" aria-label="Main menu">
          <p className="bo-nav__caption">Workspace</p>
          {sections.map((group) => {
            const isOpen = expanded === group.title;
            const isSelected = active === group.title;
            return (
              <div className="bo-nav__group" key={group.title}>
                {group.title === "Overview" ? (
                  <Link
                    to="/business/dashboard"
                    onClick={onClose}
                    aria-current={activeLinkPath === "/business/dashboard" ? "page" : undefined}
                    className={`bo-nav__link bo-nav__primary${activeLinkPath === "/business/dashboard" ? " bo-nav__selected" : ""}`}
                  >
                    <span className="bo-nav__icon"><BusinessIcon name={group.icon} /></span>
                    <span className="bo-nav__label">Overview</span>
                  </Link>
                ) : (
                  <>
                    <button type="button" className={`bo-nav__group-button${isSelected ? " bo-nav__group-active" : ""}`}
                      aria-expanded={isOpen} aria-controls={`bo-group-${group.icon}`}
                      onClick={() => setExpanded((current) => current === group.title ? "" : group.title)}>
                      <span className="bo-nav__icon"><BusinessIcon name={group.icon} /></span>
                      <span className="bo-nav__label">{group.title}</span>
                      <BusinessIcon name="chevron" size={15} className="bo-nav__chevron" />
                    </button>
                    {isOpen && (
                      <div className="bo-nav__children" id={`bo-group-${group.icon}`}>
                        {group.links.map((link) => (
                          <Link key={link.path} to={link.path} onClick={onClose}
                            aria-current={activeLinkPath === link.path ? "page" : undefined}
                            className={`bo-nav__link${activeLinkPath === link.path ? " bo-nav__selected" : ""}`}>

                            <span className="bo-nav__label">{link.label}</span>
                          </Link>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>
            );
          })}
        </nav>
        <div className="bo-sidebar__footer">
          <span className="bo-sidebar__footer-label">Operations Workspace</span>
          <span className="bo-sidebar__footer-hint">Operations · Internal System</span>
        </div>
      </aside>
    </>
  );
}

