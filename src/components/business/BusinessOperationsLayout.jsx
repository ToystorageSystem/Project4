import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { logout } from "../../api/auth/auth";
import { useAuth } from "../../auth/AuthContext";
import { resolveBusinessLocation } from "./businessOperationsNav";
import BusinessSidebar from "./BusinessSidebar";
import BusinessIcon from "./BusinessIcon";
import "../../css/business-operations-layout.css";
import "../../css/business-operations-pages.css";

function initials(name) {
  return String(name || "BO").trim().split(/\s+/).slice(-2).map((part) => part[0] || "").join("").toUpperCase();
}

export default function BusinessOperationsLayout({ children }) {
  const { user, clearUser } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [navOpen, setNavOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const { section, page } = resolveBusinessLocation(location.pathname, user);
  const displayName = user?.name || user?.fullName || user?.email || "Business Operations User";

  // Close the mobile drawer on navigation and support Escape.
  useEffect(() => { setNavOpen(false); }, [location.pathname]);
  useEffect(() => {
    const onEscape = (event) => { if (event.key === "Escape") setNavOpen(false); };
    window.addEventListener("keydown", onEscape);
    return () => window.removeEventListener("keydown", onEscape);
  }, []);

  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    try { await logout(); }
    catch (error) { console.error("Sign-out failed:", error); }
    finally { clearUser(); navigate("/login", { replace: true }); }
  };

  return (
    <div className="bo-layout">
      <BusinessSidebar user={user} open={navOpen} onClose={() => setNavOpen(false)} />
      <div className="bo-main">
        <header className="bo-topbar">
          <div className="bo-topbar__left">
            <button type="button" className="bo-mobile-toggle" aria-label="Open navigation menu" aria-expanded={navOpen} onClick={() => setNavOpen(true)}>
              <BusinessIcon name="menu" size={21} />
            </button>
            <div className="bo-breadcrumb" aria-label="Breadcrumb navigation">
              <span>Operations</span><BusinessIcon name="right" size={13} />
              <span>{section}</span><BusinessIcon name="right" size={13} />
              <strong className="bo-breadcrumb__current">{page}</strong>
            </div>
          </div>
          <div className="bo-topbar__right">
            <div className="bo-profile">
              <div className="bo-profile__text">
                <span className="bo-profile__name">{displayName}</span>
                <span className="bo-profile__role">Business Operations</span>
              </div>
              <span className="bo-profile__avatar" aria-hidden="true">{initials(displayName)}</span>
            </div>
            <button type="button" className="bo-sign-out" onClick={handleLogout} disabled={loggingOut}>
              <BusinessIcon name="logout" size={16} /> <span>{loggingOut ? "Signing out..." : "Sign out"}</span>
            </button>
          </div>
        </header>
        <main className="bo-content" id="business-main">{children}</main>
        <footer className="bo-footer">ToyStorageSystem · Business Operations</footer>
      </div>
    </div>
  );
}
