const Icon = ({ children }) => (
    <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        className="h-5 w-5"
    >
        {children}
    </svg>
);

const DashboardIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4h6v6H4V4Zm10 0h6v6h-6V4ZM4 14h6v6H4v-6Zm10 0h6v6h-6v-6Z" />
    </Icon>
);

const ReceivingIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7h13v10H3V7Zm13 3h3l2 3v4h-5v-7ZM7 17a2 2 0 1 0 0 4 2 2 0 0 0 0-4Zm10 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z" />
    </Icon>
);

const ReviewIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
    </Icon>
);

const CompletedIcon = () => (
    <Icon>
        <circle cx="12" cy="12" r="9" strokeWidth="2" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m8 12 3 3 5-6" />
    </Icon>
);

const IncidentIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 3 2 21h20L12 3Z" />
        <path strokeLinecap="round" strokeWidth="2" d="M12 9v5" />
        <circle cx="12" cy="17" r="1" fill="currentColor" stroke="none" />
    </Icon>
);

const PutawayIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 21h16M5 21V8h14v13M8 12h8M8 16h8M9 8V4h6v4" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m12 14 2-2m-2 2-2-2m2 2V9" />
    </Icon>
);

const LocationIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 21h18M5 21V5h14v16" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 9h3v3H8V9Zm5 0h3v3h-3V9ZM8 15h3v3H8v-3Zm5 0h3v3h-3v-3Z" />
    </Icon>
);

const PackingConfirmIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 7l-8-4-8 4m16 0-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m8 12 2 2 4-4" />
    </Icon>
);

const ShipmentHistoryIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 3h9l3 3v15H6V3Zm9 0v4h4M9 11h6M9 15h6" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m8 7-2 2 2 2" />
    </Icon>
);

const DispatchHandoverIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 6h11v10H3V6Zm11 4h4l3 3v3h-7v-6Z" />
        <circle cx="7" cy="18" r="2" strokeWidth="2" />
        <circle cx="18" cy="18" r="2" strokeWidth="2" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m8 10 2 2 4-4" />
    </Icon>
);

const DamagedGoodsIcon = () => (
    <Icon>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 3 3 7v6c0 5 3.8 7.7 9 8 5.2-.3 9-3 9-8V7l-9-4Z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v5" />
        <circle cx="12" cy="16" r="1" fill="currentColor" stroke="none" />
    </Icon>
);

export const warehouseMenuItems = [
    {
        key: "dashboard",
        group: "Overview",
        label: "Dashboard",
        path: "/warehouse/dashboard",
        icon: <DashboardIcon />,
    },
    {
        key: "receiving-process",
        group: "Inbound",
        label: "Receive goods",
        path: "/warehouse/receiving/process",
        icon: <ReceivingIcon />,
    },
    {
        key: "store-returns",
        group: "Inbound",
        label: "Store Returns",
        path: "/warehouse/store-returns",
        icon: <ReceivingIcon />,
    },
    {
        key: "receiving-in-progress",
        group: "Inbound",
        label: "Receiving monitor",
        path: "/warehouse/receiving/in-progress",
        icon: <ReceivingIcon />,
    },
    {
        key: "receiving-waiting-review",
        group: "Inbound",
        label: "Review receipts",
        path: "/warehouse/receiving/waiting-review",
        icon: <ReviewIcon />,
    },
    {
        key: "receiving-completed",
        group: "History & reports",
        label: "Receiving history",
        path: "/warehouse/receiving/completed",
        icon: <CompletedIcon />,
    },
    {
        key: "discrepancies",
        group: "Exceptions",
        label: "Discrepancies",
        path: "/warehouse/discrepancies",
        icon: <IncidentIcon />,
    },
    {
        key: "receiving-incidents",
        group: "History & reports",
        label: "Incident reports",
        path: "/warehouse/receiving/incident-reports",
        icon: <IncidentIcon />,
    },
    {
        key: "damaged-goods",
        group: "Exceptions",
        label: "Damaged Goods",
        path: "/warehouse/damaged-goods",
        icon: <DamagedGoodsIcon />,
    },
    {
        key: "putaway",
        group: "Inbound",
        label: "Putaway",
        path: "/warehouse/putaway",
        icon: <PutawayIcon />,
    },
    {
        key: "packing-confirmation",
        group: "Outbound",
        label: "Packing",
        path: "/warehouse/packing",
        icon: <PackingConfirmIcon />,
    },
    {
        key: "shipment-history",
        group: "History & reports",
        label: "Shipment History",
        path: "/warehouse/shipment-history",
        icon: <ShipmentHistoryIcon />,
    },
    {
        key: "dispatch-handover",
        group: "Outbound",
        label: "Dispatch",
        path: "/warehouse/dispatch",
        icon: <DispatchHandoverIcon />,
    },
    {
        key: "warehouse-locations",
        group: "Warehouse",
        label: "Locations",
        path: "/warehouse/locations",
        icon: <LocationIcon />,
    },
];
