import {
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import Sidebar from "./Sidebar";

import {
    warehouseMenuItems,
} from "../icons/warehouseMenuItems.jsx";

import {
    logout,
} from "../../api/auth/auth.js";

import {
    useAuth,
} from "../../auth/AuthContext.jsx";


function formatRole(role) {
    if (!role) {
        return "Warehouse";
    }

    if (typeof role === "object") {
        role =
            role.roleName ??
            role.name ??
            role.roleCode ??
            "";
    }

    return String(role)
        .replaceAll("_", " ")
        .trim()
        .toLowerCase()
        .replace(
            /\b\w/g,
            (char) =>
                char.toUpperCase()
        );
}


function getGreeting(date) {
    const hour =
        date.getHours();

    if (
        hour >= 5 &&
        hour < 12
    ) {
        return {
            title: "Good morning",
            message:
                "Hope you have a smooth and productive shift today.",
            type: "morning",
        };
    }

    if (hour === 12) {
        return {
            title: "Break time",
            message:
                "Time to recharge. Enjoy your lunch and take a short rest.",
            type: "break",
        };
    }

    if (
        hour >= 13 &&
        hour < 17
    ) {
        return {
            title: "Good afternoon",
            message:
                "Keep going — you're doing great today.",
            type: "afternoon",
        };
    }

    if (
        hour >= 17 &&
        hour < 19
    ) {
        return {
            title: "Goodbye",
            message:
                "Great work today. Have a safe trip home.",
            type: "goodbye",
        };
    }

    if (
        hour >= 19 &&
        hour < 23
    ) {
        return {
            title: "Overtime mode",
            message:
                "Thanks for the extra effort. Remember to take a short break.",
            type: "overtime",
        };
    }

    return {
        title: "Good night",
        message:
            "It's getting late. Take care of yourself and rest when you can.",
        type: "night",
    };
}


function GreetingIcon({
                          type,
                      }) {
    if (type === "morning") {
        return (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-6 w-6"
            >
                <circle
                    cx="12"
                    cy="12"
                    r="4"
                    strokeWidth="2"
                />

                <path
                    strokeLinecap="round"
                    strokeWidth="2"
                    d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.65 17.65l1.42 1.42M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.65 6.35l1.42-1.42"
                />
            </svg>
        );
    }

    if (type === "break") {
        return (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-6 w-6"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M4 5h12v7a6 6 0 0 1-12 0V5Zm12 2h2a3 3 0 0 1 0 6h-2M3 21h16"
                />
            </svg>
        );
    }

    if (type === "afternoon") {
        return (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-6 w-6"
            >
                <circle
                    cx="12"
                    cy="10"
                    r="4"
                    strokeWidth="2"
                />

                <path
                    strokeLinecap="round"
                    strokeWidth="2"
                    d="M12 2v2M4 10H2M22 10h-2M5 4l1.5 1.5M19 4l-1.5 1.5M3 18h18"
                />
            </svg>
        );
    }

    if (type === "goodbye") {
        return (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-6 w-6"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M8 11V5a2 2 0 1 1 4 0v5-3a2 2 0 1 1 4 0v4-2a2 2 0 1 1 4 0v6c0 4-3 7-7 7h-1c-3 0-5-1-7-4l-3-4a2 2 0 0 1 3-3l3 3"
                />
            </svg>
        );
    }

    if (type === "overtime") {
        return (
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                className="h-6 w-6"
            >
                <circle
                    cx="12"
                    cy="12"
                    r="9"
                    strokeWidth="2"
                />

                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M12 7v5l3 2"
                />
            </svg>
        );
    }

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-6 w-6"
        >
            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M20 15.5A8.5 8.5 0 0 1 8.5 4 8.5 8.5 0 1 0 20 15.5Z"
            />
        </svg>
    );
}


function UserIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >
            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M20 21a8 8 0 0 0-16 0M12 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z"
            />
        </svg>
    );
}


function LogoutIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >
            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M10 17l5-5-5-5M15 12H3M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5"
            />
        </svg>
    );
}


function WorkspaceHeader() {
    const navigate =
        useNavigate();

    const {
        user,
        clearUser,
    } = useAuth();

    const [now, setNow] =
        useState(
            () => new Date()
        );

    const [
        loggingOut,
        setLoggingOut,
    ] = useState(false);


    useEffect(() => {
        const updateClock = () => {
            setNow(new Date());
        };

        updateClock();

        const timer = window.setInterval(
            updateClock,
            1000
        );

        return () => {
            window.clearInterval(timer);
        };
    }, []);


    const handleLogout =
        async () => {
            if (loggingOut) {
                return;
            }

            try {
                setLoggingOut(true);

                await logout();
            } catch (error) {
                console.error(
                    "Logout error:",
                    error
                );
            } finally {
                clearUser();

                navigate(
                    "/login",
                    {
                        replace: true,
                    }
                );
            }
        };


    const greeting =
        getGreeting(now);


    const displayName =
        user?.name ??
        user?.fullName ??
        user?.email ??
        "User";


    const role =
        formatRole(
            user?.roleName ??
            user?.role ??
            user?.roleCode ??
            user?.roles?.[0]?.roleName ??
            user?.roles?.[0]?.name ??
            user?.userRoles?.[0]?.role?.roleName ??
            user?.userRoles?.[0]?.role?.name
        );


    const time =
        now.toLocaleTimeString(
            "en-GB",
            {
                hour: "2-digit",
                minute: "2-digit",
            }
        );


    const date =
        now.toLocaleDateString(
            "en-GB",
            {
                weekday: "short",
                day: "2-digit",
                month: "short",
                year: "numeric",
            }
        );


    return (
        <header className="border-b border-slate-200 bg-white">

            <div className="flex min-h-[96px] flex-col justify-center gap-4 px-6 py-4 lg:flex-row lg:items-center lg:justify-between">

                <div className="flex min-w-0 items-center gap-4">

                    <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl bg-[#fff1e9] text-[#f25d19]">

                        <GreetingIcon
                            type={
                                greeting.type
                            }
                        />

                    </div>


                    <div className="min-w-0">

                        <div className="flex flex-wrap items-center gap-2">

                            <p className="text-sm font-semibold text-[#d94f12]">
                                {greeting.title},
                            </p>


                            {greeting.type === "goodbye" && (
                                <span>
                                    👋
                                </span>
                            )}


                            {greeting.type === "overtime" && (
                                <span>
                                    🌙
                                </span>
                            )}

                        </div>


                        <h2 className="mt-0.5 truncate text-xl font-bold text-slate-900">
                            {displayName}
                        </h2>


                        <p className="mt-1 text-xs text-slate-500">
                            {greeting.message}
                        </p>

                    </div>

                </div>


                <div className="flex flex-wrap items-center gap-4">

                    <div className="hidden text-right xl:block">

                        <p className="text-xs text-slate-400">
                            {date}
                        </p>

                        <p className="mt-1 text-sm font-semibold text-slate-700">
                            {time}
                        </p>

                    </div>


                    <div className="hidden h-10 w-px bg-slate-200 xl:block" />


                    <div className="flex items-center gap-3">

                        <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1e9] text-[#f25d19]">
                            <UserIcon />
                        </div>


                        <div>

                            <p className="text-xs text-slate-400">
                                Position
                            </p>

                            <p className="mt-0.5 whitespace-nowrap text-sm font-semibold text-slate-800">
                                {role}
                            </p>

                        </div>

                    </div>


                    <button
                        type="button"
                        onClick={
                            handleLogout
                        }
                        disabled={
                            loggingOut
                        }
                        className="flex h-11 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-600 transition hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-50"
                    >

                        <LogoutIcon />


                        <span className="hidden sm:inline">

                            {loggingOut
                                ? "Logging out..."
                                : "Logout"}

                        </span>

                    </button>

                </div>

            </div>

        </header>
    );
}


export default function MainLayout({
                                       children,
                                   }) {
    return (
        <div className="min-h-screen bg-slate-50">

            <div className="flex min-h-screen">

                <Sidebar
                    title="Warehouse"
                    items={
                        warehouseMenuItems
                    }
                />


                <div className="flex min-w-0 flex-1 flex-col">

                    <div className="print:hidden">
                        <WorkspaceHeader/>
                    </div>


                    <main className="flex min-h-0 flex-1 flex-col p-6">

                        {children}

                    </main>

                </div>

            </div>

        </div>
    );
}