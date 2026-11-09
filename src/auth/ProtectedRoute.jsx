import { Navigate, Outlet, useLocation, Link } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { authPath, canAccess, homePath } from "./access";
import { errorMessage } from "../api/api";
export default function ProtectedRoute({ access }) {
  const { user, loading, error, refresh } = useAuth();
  const location = useLocation();
  if (loading)
    return (
      <div className="session-screen" role="status">
        Checking your session…
      </div>
    );
  if (error)
    return (
      <div className="session-screen">
        <p role="alert">{errorMessage(error)}</p>
        <button className="ops-button" onClick={refresh}>
          Try again
        </button>
      </div>
    );
  if (!user || user.stage !== "AUTHENTICATED")
    return (
      <Navigate
        to={authPath(user)}
        state={{ from: location.pathname }}
        replace
      />
    );
  if (access && !canAccess(user, access))
    return (
      <div className="session-screen">
        <h1>Access denied</h1>
        <Link to={homePath(user)}>Back to my workspace</Link>
      </div>
    );
  return <Outlet />;
}
