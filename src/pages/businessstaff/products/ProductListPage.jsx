import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { changeProductVisibility, listProducts, productErrorMessage } from "../../../api/business/products";
import ProductStatusBadge, { formatProductPrice } from "../../../components/business/products/ProductStatusBadge";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Checkbox from "../../../components/ui/Checkbox";
import EmptyState from "../../../components/ui/EmptyState";
import Modal from "../../../components/ui/Modal";
import Pagination from "../../../components/ui/Pagination";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const ROOT = "/business/products";
const PAGE_SIZE = 10;
const STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
    { value: "ACTIVE", label: "Active" },
    { value: "INACTIVE", label: "Inactive" },
    { value: "DISCONTINUED", label: "Discontinued" },
];

export default function ProductListPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState(location.state?.notice || "");
    const [target, setTarget] = useState(null);
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listProducts({ status, page, size: PAGE_SIZE }, controller.signal)
            .then((result) => {
                if (!controller.signal.aborted) {
                    if (!Array.isArray(result?.content)) throw new Error("Invalid product list response.");
                    setData(result);
                }
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(productErrorMessage(requestError, "Unable to load products."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [status, page, reload]);

    const rows = data?.content || [];
    const openAction = (product, makeActive) => {
        setTarget({ product, makeActive });
        setConfirmed(false);
        setError("");
        setNotice("");
    };

    const submitVisibility = async () => {
        if (!target || !confirmed || saving) return;
        setSaving(true);
        setError("");
        try {
            await changeProductVisibility(target.product.id, target.makeActive);
            setNotice(`${target.makeActive ? "Restore" : "Inactive"} product(s) successfully.`);
            setTarget(null);
            setConfirmed(false);
            setReload((current) => current + 1);
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Unable to change product status."));
            setTarget(null);
        } finally {
            setSaving(false);
        }
    };

    const columns = [
        { key: "productCode", label: "Product Code", render: (v, p) => <Link className="font-semibold text-[#c94810] hover:underline" to={`${ROOT}/${p.id}`}>{v || "—"}</Link> },
        { key: "name", label: "Product Name", render: (v) => <span className="font-medium text-slate-900">{v || "—"}</span> },
        { key: "barcode", label: "Barcode" },
        { key: "categoryName", label: "Categories", render: (v) => v || "—" },
        { key: "sellingPrice", label: "Selling Price", render: formatProductPrice },
        { key: "status", label: "Status", render: (v) => <ProductStatusBadge status={v} /> },
        {
            key: "actions", label: "Actions", render: (_, product) => (
                <div className="flex flex-wrap items-center gap-2">
                    <Button size="sm" variant="ghost" onClick={() => navigate(`${ROOT}/${product.id}`)}>Details</Button>
                    {(product.status === "ACTIVE" || product.status === "INACTIVE") && (
                        <Button size="sm" variant="outline" onClick={() => navigate(`${ROOT}/${product.id}/edit`)}>Edit</Button>
                    )}
                    {product.status === "ACTIVE" && (
                        <Button size="sm" variant="danger" onClick={() => openAction(product, false)}>Inactive</Button>
                    )}
                    {product.status === "INACTIVE" && (
                        <Button size="sm" variant="success" onClick={() => openAction(product, true)}>Restore</Button>
                    )}
                </div>
            ),
        },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="Product Management" description="View, create, edit, hide, and restore products. All changes are recorded in the history."
                    actions={
                        <div className="flex flex-wrap gap-2">
                            <Button variant="outline" onClick={() => navigate(`${ROOT}/import`)}>Import from Excel</Button>
                            <Button onClick={() => navigate(`${ROOT}/new`)}>+ Add Product</Button>
                        </div>
                    }
                />
                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <div className="flex flex-wrap items-end justify-between gap-4">
                        <Select label="Filter by Status" className="min-w-56" value={status} options={STATUS_OPTIONS}
                            onChange={(event) => { setStatus(event.target.value); setPage(0); }} />
                        <Button variant="ghost" onClick={() => setReload((current) => current + 1)}>Refresh</Button>
                    </div>
                    <p className="mt-3 text-sm text-slate-500">Total: {data?.totalElements ?? "—"} products · Sorted by newest creation date first.</p>
                </div>
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm text-slate-600"><Spinner /> Loading product...</div>
                ) : rows.length === 0 && !error ? (
                    <EmptyState title="No Products" description="No products match the selected status." />
                ) : !error ? <Table columns={columns} data={rows} /> : null}
                {!loading && !error && data && data.totalPages > 0 && (
                    <Pagination page={page + 1} totalPages={data.totalPages} onChange={(selected) => setPage(selected - 1)} />
                )}
                <Modal open={Boolean(target)} title={target?.makeActive ? "Restore Product" : "Hide Product"}
                    onClose={() => { if (!saving) setTarget(null); }}
                    footer={<>
                        <Button variant="ghost" disabled={saving} onClick={() => setTarget(null)}>Cancel</Button>
                        <Button variant={target?.makeActive ? "success" : "danger"} disabled={!confirmed || saving} onClick={submitVisibility}>
                            {saving ? "Processing..." : "Confirm"}
                        </Button>
                    </>}
                >
                    <p className="mb-4 text-sm text-slate-700">{target?.makeActive ? "Restore" : "Inactive"} products <strong>{target?.product?.name}</strong> (ID {target?.product?.productCode})?</p>
                    <p className="mb-4 text-sm text-amber-700">This action changes the product status without deleting the product.</p>
                    <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} label="I confirm this change to shared team data." />
                </Modal>
            </div>
        </MainLayout>
    );
}
