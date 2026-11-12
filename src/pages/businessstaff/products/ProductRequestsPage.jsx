import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getMyProductRequests, productErrorMessage } from "../../../api/business/products";
import ProductStatusBadge, { formatProductDate } from "../../../components/business/products/ProductStatusBadge";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import EmptyState from "../../../components/ui/EmptyState";
import Pagination from "../../../components/ui/Pagination";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const ROOT = "/business/products";
const TYPES = { CREATE: "Create New", UPDATE: "Update Request", DEACTIVATE: "Inactive" };

export default function ProductRequestsPage() {
    const navigate = useNavigate();
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getMyProductRequests({ page, size: 10 }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.items)) throw new Error("Invalid request list data");
                setData(result);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(productErrorMessage(requestError, "Unable to load requests."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [page, reload]);

    const columns = [
        { key: "requestCode", label: "Request Code", render: (v) => <span className="font-medium text-slate-900">{v || "—"}</span> },
        { key: "requestType", label: "Type", render: (v) => TYPES[v] || v || "—" },
        { key: "productCode", label: "Product Code" },
        { key: "productName", label: "Product" },
        { key: "status", label: "Status", render: (v) => <ProductStatusBadge status={v} /> },
        { key: "rejectionReason", label: "Rejection Reason", render: (v) => v || "—" },
        { key: "createdAt", label: "Time", render: formatProductDate },
        { key: "productId", label: "Details", render: (v) => v ? <Link to={`${ROOT}/${v}`} className="text-[#c94810] hover:underline">View Product</Link> : "—" },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="My Product Requests" description="Track product creation and update requests, review results, and rejection reasons."
                    actions={<div className="flex gap-2"><Button variant="ghost" onClick={() => navigate(ROOT)}>← Back to List</Button><Button variant="outline" onClick={() => setReload((v) => v + 1)}>Refresh</Button></div>}
                />
                <Alert type="info">Product information is updated only after Business Manager approval. Pending requests will be reviewed later.</Alert>
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading ? <div role="status" className="flex items-center gap-3 rounded-2xl bg-white p-5"><Spinner /> Loading requests...</div>
                    : !error && (data?.items?.length || 0) === 0 ? <EmptyState title="No Requests" description="You have not submitted any requests in this list." />
                        : !error ? <Table columns={columns} data={data.items} /> : null}
                {!loading && !error && data && data.totalPages > 0 && (
                    <Pagination page={page + 1} totalPages={data.totalPages} onChange={(value) => setPage(value - 1)} />
                )}
            </div>
        </MainLayout>
    );
}

