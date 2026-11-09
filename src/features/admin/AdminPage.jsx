import { useNavigate } from "react-router-dom";
import MainLayout from "../../components/layout/MainLayout";
import WorkspaceWelcome from "../../components/layout/WorkspaceWelcome";
export const adminPaths = { dashboard:"/admin/dashboard", users:"/admin/users", roles:"/admin/roles", warehouses:"/admin/warehouses", logs:"/admin/activity-logs", alerts:"/admin/security-alerts" };
export default function AdminPage({ page: Page, overview = false }) {
  const navigate = useNavigate();
  return <MainLayout>{overview && <WorkspaceWelcome/>}<div className="admin-workspace"><Page navigate={id => navigate(adminPaths[id] || adminPaths.dashboard)} /></div></MainLayout>;
}
