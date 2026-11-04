import Badge from "./Badge";

const map = {
    CREATED: "orange",
    ASSIGNED: "orange",
    ACCEPTED: "blue",
    READY_TO_SHIP: "yellow",
    IN_TRANSIT: "blue",
    ARRIVED: "yellow",
    DELIVERED: "green",
    COMPLETED: "green",
    CONFIRMED: "green",
    FAILED: "red",
    CANCELLED: "red",
    REJECTED: "red",
    PACKING: "orange",
    PACKED: "yellow",
    SHIPPED: "blue",
    RECEIVING: "orange",
    INSPECTING: "orange",
    INSPECTED: "yellow",
    PENDING_CONFIRMATION: "yellow",
    RECEIVED: "green",
    OPEN: "red",
    INVESTIGATING: "yellow",
    RESOLVED: "green",
    ACTIVE: "green",
    INACTIVE: "default",
};

export default function StatusBadge({ status }) {
    return <Badge variant={map[status] || "default"}>{status || "-"}</Badge>;
}
