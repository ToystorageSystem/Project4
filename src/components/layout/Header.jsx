import { useNavigate } from "react-router-dom";

import Button from "../ui/Button";
import { logout } from "../../api/auth/auth";
import { useAuth } from "../../auth/AuthContext";

export default function Header({
    logoSrc = "/logo.png",
    title = "Toy Storage System",
}) {
    const navigate = useNavigate();
    const { user, setUser } = useAuth();

    const displayName =
        user?.name ??
        user?.fullName ??
        user?.username ??
        user?.email ??
        "User";

    const roleName =
        user?.role?.name ??
        user?.roleName ??
        user?.role ??
        "";

    const handleLogout = async () => {
        try {
            await logout();
        } finally {
            setUser(null);
            navigate("/login");
        }
    };

    return (
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200 bg-white/95 px-4 backdrop-blur md:px-6">
            <div className="flex min-w-0 items-center gap-3">
                <div className="app-logo-frame">
                    <img
                        src={logoSrc}
                        alt="Toy Storage System logo"
                        className="app-logo"
                    />
                </div>

                <div className="min-w-0">
                    <div className="truncate font-semibold text-slate-900">
                        {title}
                    </div>

                    <div className="hidden text-xs text-slate-400 sm:block">
                        Warehouse Management System
                    </div>
                </div>
            </div>

            <div className="ml-4 flex items-center gap-3">
                <div className="hidden text-right sm:block">
                    <p className="max-w-48 truncate text-sm font-semibold text-slate-800">
                        {displayName}
                    </p>

                    {roleName && (
                        <p className="max-w-48 truncate text-xs text-slate-400">
                            {roleName}
                        </p>
                    )}
                </div>

                <div
                    className="flex h-9 w-9 items-center justify-center rounded-full bg-[#fff1e9] font-semibold uppercase text-[#d94f12]"
                    title={displayName}
                >
                    {displayName.charAt(0)}
                </div>

                <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleLogout}
                >
                    Sign out
                </Button>
            </div>
        </header>
    );
}
