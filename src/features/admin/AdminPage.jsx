import { useNavigate } from "react-router-dom";
import MainLayout from "../../components/layout/MainLayout";
import WorkspaceWelcome from "../../components/layout/WorkspaceWelcome";
// Shared admin layout styles: load for every admin page, not just the dashboard,
// so a direct reload of /admin/users etc. keeps its layout.
import "./css/App.css";
export const adminPaths = { dashboard:"/admin/dashboard", users:"/admin/users", roles:"/admin/roles", warehouses:"/admin/warehouses", logs:"/admin/activity-logs", alerts:"/admin/security-alerts" };
export default function AdminPage({ page: Page, overview = false }) {
  const navigate = useNavigate();
  return <MainLayout>{overview && <WorkspaceWelcome/>}<div className="admin-workspace"><Page navigate={id => navigate(adminPaths[id] || adminPaths.dashboard)} /></div></MainLayout>;
}
