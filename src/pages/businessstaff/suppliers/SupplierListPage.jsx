import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { changeSupplierStatus, listSuppliers, supplierErrorMessage } from "../../../api/business/suppliers";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Modal from "../../../components/ui/Modal";
import Pagination from "../../../components/ui/Pagination";
import SearchInput from "../../../components/ui/SearchInput";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import StatusBadge from "../../../components/ui/StatusBadge";
import Table from "../../../components/ui/Table";

const ROOT = "/business/suppliers";
const PAGE_SIZE = 10;
const STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
    { value: "ACTIVE", label: "Active" },
    { value: "INACTIVE", label: "Inactive" },
];
const SORT_OPTIONS = [
    { value: "createdAt", label: "Created At" },
    { value: "name", label: "Supplier Name" },
    { value: "suppliersCode", label: "Supplier Code" },
    { value: "updatedAt", label: "Updated At" },
];

export default function SupplierListPage() {
    const navigate = useNavigate();
    const [searchDraft, setSearchDraft] = useState("");
    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState("");
    const [sortBy, setSortBy] = useState("createdAt");
    const [direction, setDirection] = useState("DESC");
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [action, setAction] = useState(null);
    const [savingStatus, setSavingStatus] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listSuppliers({
            keyword: keyword || undefined,
            status: status || undefined,
            page,
            size: PAGE_SIZE,
            sortBy,
            direction,
        }, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data?.content)) {
                    throw new Error("Invalid supplier list data");
                }
                setResult(data);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;
                setResult(null);
                setError(supplierErrorMessage(requestError, "Unable to load suppliers."));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [keyword, status, page, sortBy, direction, reload]);

    const handleSearch = (event) => {
        event.preventDefault();
        setPage(0);
        setKeyword(searchDraft.trim());
    };

    const confirmStatusChange = async () => {
        if (!action || savingStatus) return;
        setSavingStatus(true);
        setError("");
        setNotice("");
        try {
            await changeSupplierStatus(action.id, action.status === "INACTIVE");
            setNotice(action.status === "ACTIVE"
                ? `Supplier deactivated ${action.name}.`
                : `Supplier restored ${action.name}.`);
            setAction(null);
            setReload((value) => value + 1);
        } catch (requestError) {
            // BE is authoritative; e.g. it rejects hiding suppliers with unfinished POs.
            setError(supplierErrorMessage(requestError, "Unable to change supplier status."));
            setAction(null);
        } finally {
            setSavingStatus(false);
        }
    };

    const columns = [
        {
            key: "supplierCode", label: "Supplier Code",
            render: (_value, supplier) => (
                <Link className="font-semibold text-[#c94810] hover:underline" to={`${ROOT}/${supplier.id}`}>
                    {supplier.supplierCode || "—"}
                </Link>
            ),
        },
        { key: "name", label: "Supplier Name", render: (value) => <span className="font-medium text-slate-900">{value || "—"}</span> },
        { key: "contactPerson", label: "Contact Person", render: (value) => value || "—" },
        { key: "phone", label: "Phone Number", render: (value) => value || "—" },
        { key: "email", label: "Email", render: (value) => value || "—" },
        { key: "status", label: "Status", render: (value) => <StatusBadge status={value} /> },
        {
            key: "id", label: "Actions",
            render: (_value, supplier) => (
                <div className="flex flex-wrap items-center gap-2">
                    <Button type="button" variant="ghost" size="sm" onClick={() => navigate(`${ROOT}/${supplier.id}`)}>Details</Button>
                    <Button type="button" variant="outline" size="sm" onClick={() => navigate(`${ROOT}/${supplier.id}/edit`)}>Edit</Button>
                    <Button
                        type="button"
                        variant={supplier.status === "ACTIVE" ? "danger" : "secondary"}
                        size="sm"
                        onClick={() => { setError(""); setNotice(""); setAction(supplier); }}
                    >
                        {supplier.status === "ACTIVE" ? "Inactive" : "Restore"}
                    </Button>
                </div>
            ),
        },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Supplier Management"
                    description="Supplier list, search, contact details, and status. Supplier codes are generated automatically."
                    actions={<Button type="button" onClick={() => navigate(`${ROOT}/new`)}>+ Add Supplier</Button>}
                />

                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}

                <Card>
                    <form onSubmit={handleSearch} className="grid items-end gap-3 lg:grid-cols-[minmax(200px,1fr)_180px_170px_140px_auto]">
                        <SearchInput
                            label="Search by code, name, or phone number"
                            value={searchDraft}
                            onChange={(event) => setSearchDraft(event.target.value)}
                            placeholder="Enter code, name, or phone number..."
                        />
                        <Select label="Status" options={STATUS_OPTIONS} value={status} onChange={(event) => { setPage(0); setStatus(event.target.value); }} />
                        <Select label="Sort By" options={SORT_OPTIONS} value={sortBy} onChange={(event) => { setPage(0); setSortBy(event.target.value); }} />
                        <Select
                            label="Order"
                            options={[{ value: "DESC", label: "Descending" }, { value: "ASC", label: "Ascending" }]}
                            value={direction}
                            onChange={(event) => { setPage(0); setDirection(event.target.value); }}
                        />
                        <Button type="submit">Search</Button>
                    </form>
                    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">
                        <span>{loading ? "Loading..." : `Found ${result?.totalElements ?? 0} suppliers`}</span>
                        <Button type="button" variant="ghost" size="sm" onClick={() => setReload((value) => value + 1)}>Refresh</Button>
                    </div>
                </Card>

                {loading ? (
                    <div className="flex items-center justify-center gap-3 rounded-2xl bg-white p-12 text-sm text-slate-600" role="status">
                        <Spinner /> Loading suppliers...
                    </div>
                ) : result && result.content.length ? (
                    <div className="space-y-4">
                        <Table columns={columns} data={result.content} />
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <p className="text-sm text-slate-500">Trang {page + 1} / {Math.max(1, result.totalPages)}</p>
                            <Pagination
                                page={page + 1}
                                totalPages={Math.max(1, result.totalPages)}
                                onChange={(nextPage) => setPage(nextPage - 1)}
                            />
                        </div>
                    </div>
                ) : !error ? (
                    <EmptyState title="No Suppliers Found" description="Try a different search term or status filter." />
                ) : null}
            </div>

            <Modal
                open={Boolean(action)}
                title={action?.status === "ACTIVE" ? "Confirm Supplier Deactivation" : "Confirm Restoration"}
                onClose={() => { if (!savingStatus) setAction(null); }}
                footer={(
                    <>
                        <Button type="button" variant="ghost" disabled={savingStatus} onClick={() => setAction(null)}>Cancel</Button>
                        <Button
                            type="button"
                            variant={action?.status === "ACTIVE" ? "danger" : "primary"}
                            disabled={savingStatus}
                            onClick={confirmStatusChange}
                        >
                            {savingStatus ? "Processing..." : "Confirm"}
                        </Button>
                    </>
                )}
            >
                <p className="text-sm leading-6 text-slate-600">
                    {action?.status === "ACTIVE"
                        ? `Deactivate “${action?.name}”? Suppliers with outstanding purchase orders cannot be deactivated.`
                        : `Restore “${action?.name}” to Active status?`}
                </p>
                <p className="mt-3 text-xs text-amber-700">This change will be saved to the shared database.</p>
            </Modal>
        </MainLayout>
    );
}

