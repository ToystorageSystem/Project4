import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getLinksBySupplier, supplierProductErrorMessage } from "../../../api/business/supplierProducts";
import Alert from "../../ui/Alert";
import Button from "../../ui/Button";
import EmptyState from "../../ui/EmptyState";
import StatusBadge from "../../ui/StatusBadge";
const businessStatusLabel = (value) => ({ ACTIVE: "Active", INACTIVE: "Inactive", REJECTED: "Rejected", PENDING: "Pending Approval" }[value] || value);
import Table from "../../ui/Table";

const show = (value) => value == null || value === "" ? "—" : value;
const formatMoney = (value) => value == null ? "—" : `${Number(value).toLocaleString("en-GB")} ₫`;

export default function SupplierProductsPanel({ supplierId }) {
    const [links, setLinks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getLinksBySupplier(supplierId, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result)) throw new Error("Invalid product–supplier association data");
                setLinks(result);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(supplierProductErrorMessage(requestError, "Unable to load product associations."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [supplierId, reload]);

    const columns = [
        {
            key: "productCode", label: "Product Code",
            render: (value, link) => <Link to={`/business/products/${link.productId}`} className="font-semibold text-[#c94810] hover:underline">{show(value)}</Link>,
        },
        { key: "productName", label: "Product Name", render: (value) => <span className="font-medium text-slate-900">{show(value)}</span> },
        { key: "supplierProductCode", label: "Supplier Product Code", render: show },
        { key: "purchasePrice", label: "Purchase Price", render: formatMoney },
        { key: "minimumOrderQuantity", label: "Minimum Order Quantity", render: show },
        { key: "leadTimeDays", label: "Lead Time (days)", render: show },
        { key: "status", label: "Association", render: (value, link) => <div className="space-y-1"><StatusBadge status={value} label={businessStatusLabel(value)} />{link.defaultSupplier && <div className="text-xs font-semibold text-[#c94810]">★ Preferred Supplier</div>}</div> },
        { key: "productStatus", label: "Product Status", render: (value) => <StatusBadge status={value} label={businessStatusLabel(value)} /> },
        { key: "id", label: "Actions", render: (_value, link) => <Link className="font-semibold text-[#c94810] hover:underline" to={`/business/products/${link.productId}`}>Manage Associations</Link> },
    ];

    return (
        <section className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h2 className="text-lg font-bold text-slate-900">Linked Products ({links.length})</h2>
                    <p className="mt-1 text-sm text-slate-500">Includes both active and inactive associations</p>
                </div>
                <Button type="button" variant="ghost" size="sm" onClick={() => setReload((value) => value + 1)}>Refresh</Button>
            </div>
            {loading ? <p role="status" className="text-sm text-slate-500">Loading associations...</p>
                : error ? <div role="alert"><Alert type="warning">{error}</Alert></div>
                    : links.length ? <Table columns={columns} data={links} />
                        : <EmptyState title="No Linked Products" description="Open an active product's details and select Link Supplier." />}
        </section>
    );
}

