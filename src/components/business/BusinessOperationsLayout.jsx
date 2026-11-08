import MainLayout from "../layout/MainLayout";
import WorkspaceWelcome from "../layout/WorkspaceWelcome";
import { useLocation } from "react-router-dom";
import "../../css/business-operations-layout.css";
import "../../css/business-operations-pages.css";
export default function BusinessOperationsLayout({children}) {
 const {pathname}=useLocation();
 return <MainLayout>{pathname === "/business/dashboard" && <WorkspaceWelcome/>}<div className="business-workspace bo-content">{children}</div></MainLayout>;
}
