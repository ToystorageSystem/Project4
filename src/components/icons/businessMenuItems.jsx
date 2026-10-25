function DashboardIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-5 w-5" aria-hidden="true">
            <path strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" d="M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z" />
        </svg>
    );
}

// More menu items will be added only after their routes and pages exist.
export const businessStaffMenuItems = [
    {
        key: "business-staff-dashboard",
        label: "Dashboard",
        path: "/business-staff/dashboard",
        icon: <DashboardIcon />,
    },
];

export const businessManagerMenuItems = [
    {
        key: "business-manager-dashboard",
        label: "Dashboard",
        path: "/business-manager/dashboard",
        icon: <DashboardIcon />,
    },
];
