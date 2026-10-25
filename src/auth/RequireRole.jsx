import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { getHomePath, hasAuthority } from "./roleRouting";

/**
 * Navigation guard only. Backend @PreAuthorize is the actual security boundary.
 * AuthContext is hydrated from sessionStorage and does NOT verify server session.
 * An expired session will be rejected (401) on the first API request.
 */
export default function RequireRole({ authority }) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (!hasAuthority(user, authority)) {
        return <Navigate to={getHomePath(user)} replace />;
    }

    return <Outlet />;
}
