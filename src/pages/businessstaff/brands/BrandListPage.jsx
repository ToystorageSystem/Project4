import { useEffect, useState } from "react";
import { activateBrand, brandErrorMessage, deactivateBrand, listBrands } from "../../../api/business/brands";
import BrandFormModal from "../../../components/business/brands/BrandFormModal";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Checkbox from "../../../components/ui/Checkbox";
import EmptyState from "../../../components/ui/EmptyState";
import Input from "../../../components/ui/Input";
import Modal from "../../../components/ui/Modal";
import Pagination from "../../../components/ui/Pagination";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
    { value: "ACTIVE", label: "Active" },
    { value: "INACTIVE", label: "Inactive" },
];

// Quan ly thuong hieu - tuong tu trang Product Categories
export default function BrandListPage() {
    const [searchInput, setSearchInput] = useState("");
    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    // Popup them/sua: { brand: null } = them moi, { brand } = sua
    const [formTarget, setFormTarget] = useState(null);

    // Popup an/khoi phuc
    const [visibilityTarget, setVisibilityTarget] = useState(null);
    const [visibilityError, setVisibilityError] = useState("");
    const [visibilityConfirmed, setVisibilityConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listBrands({ keyword, status, page, size: PAGE_SIZE }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.content)) throw new Error("Invalid brand list data");
                setData(result);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(brandErrorMessage(requestError, "Unable to load brands."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [keyword, status, page, reload]);

    const rows = data?.content || [];

    const applySearch = () => {
        setKeyword(searchInput.trim());
        setPage(0);
        setReload((current) => current + 1);
    };

    const openForm = (brand) => {
        setFormTarget({ brand });
        setNotice("");
    };

    const handleSaved = () => {
        setNotice(formTarget?.brand ? "Brand updated successfully." : "Brand added successfully.");
        setFormTarget(null);
        setReload((current) => current + 1);
    };

    const openVisibility = (brand, makeActive) => {
        setVisibilityTarget({ brand, makeActive });
        setVisibilityError("");
        setVisibilityConfirmed(false);
        setNotice("");
    };

    const closeVisibility = () => {
        if (saving) return;
        setVisibilityTarget(null);
        setVisibilityError("");
        setVisibilityConfirmed(false);
    };

    const submitVisibility = async () => {
        if (!visibilityTarget || !visibilityConfirmed || saving) return;
        const { brand, makeActive } = visibilityTarget;
        setSaving(true);
        setVisibilityError("");
        try {
            if (makeActive) await activateBrand(brand.id);
            else await deactivateBrand(brand.id);
            setNotice(makeActive ? "Brand restored successfully." : "Brand hidden successfully.");
            setVisibilityTarget(null);
            setVisibilityConfirmed(false);
            setReload((current) => current + 1);
        } catch (requestError) {
            setVisibilityError(brandErrorMessage(requestError, "Unable to change brand status."));
        } finally {
            setSaving(false);
        }
    };

    const columns = [
        { key: "brandCode", label: "Brand Code", render: (value) => <span className="font-semibold text-[#c94810]">{value || "—"}</span> },
        { key: "name", label: "Brand Name", render: (value) => <span className="font-medium text-slate-900">{value || "—"}</span> },
        { key: "country", label: "Country", render: (value) => value || "—" },
        { key: "description", label: "Description", render: (value) => value || "—" },
        {
            key: "status",
            label: "Status",
            render: (value) => (
                <span className={value === "ACTIVE"
                    ? "inline-flex rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-700"
                    : "inline-flex rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600"}>
                    {value === "ACTIVE" ? "Active" : "Inactive"}
                </span>
            ),
        },
        {
            key: "actions",
            label: "Actions",
            render: (_, brand) => (
                <div className="flex flex-wrap gap-2">
                    <Button size="sm" variant="outline" onClick={() => openForm(brand)}>Edit</Button>
                    {brand.status === "ACTIVE" && <Button size="sm" variant="danger" onClick={() => openVisibility(brand, false)}>Inactive</Button>}
                    {brand.status === "INACTIVE" && <Button size="sm" variant="success" onClick={() => openVisibility(brand, true)}>Restore</Button>}
                </div>
            ),
        },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Brand Management"
                    description="Manage brand codes, names, countries, descriptions, and status."
                    actions={<Button onClick={() => openForm(null)}>+ Add Brand</Button>}
                />

                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}

                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <div className="flex flex-wrap items-end gap-3">
                        <div className="min-w-60 flex-1">
                            <Input
                                label="Search by brand code, name or country"
                                placeholder="Enter brand code, name or country..."
                                value={searchInput}
                                onChange={(event) => setSearchInput(event.target.value)}
                                onKeyDown={(event) => {
                                    if (event.key === "Enter") {
                                        event.preventDefault();
                                        applySearch();
                                    }
                                }}
                            />
                        </div>
                        <Button variant="outline" onClick={applySearch}>Search</Button>
                        <div className="min-w-48">
                            <Select label="Status Filter" value={status} options={STATUS_OPTIONS}
                                onChange={(event) => { setStatus(event.target.value); setPage(0); }} />
                        </div>
                        <Button variant="ghost" onClick={() => setReload((current) => current + 1)}>Refresh</Button>
                    </div>
                    <p className="mt-4 text-sm text-slate-500">
                        Total: {data?.totalElements ?? "—"} brands. Sorted alphabetically by name.
                    </p>
                </div>

                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm text-slate-600"><Spinner /> Loading brands...</div>
                ) : error ? null : rows.length === 0 ? (
                    <EmptyState title="No Brands" description="No brands match the selected filters." />
                ) : (
                    <Table columns={columns} data={rows} />
                )}

                {!loading && !error && data && data.totalPages > 0 && (
                    <Pagination page={page + 1} totalPages={data.totalPages} onChange={(selected) => setPage(selected - 1)} />
                )}

                <BrandFormModal
                    open={Boolean(formTarget)}
                    brand={formTarget?.brand || null}
                    onClose={() => setFormTarget(null)}
                    onSaved={handleSaved}
                />

                <Modal
                    open={Boolean(visibilityTarget)}
                    title={visibilityTarget?.makeActive ? "Restore Brand" : "Hide Brand"}
                    onClose={closeVisibility}
                    footer={<>
                        <Button variant="ghost" disabled={saving} onClick={closeVisibility}>Cancel</Button>
                        <Button variant={visibilityTarget?.makeActive ? "success" : "danger"}
                            disabled={!visibilityConfirmed || saving} onClick={submitVisibility}>
                            {saving ? "Processing..." : "Confirm"}
                        </Button>
                    </>}
                >
                    <div className="space-y-4">
                        {visibilityError && <Alert type="danger">{visibilityError}</Alert>}
                        <p className="text-sm text-slate-700">
                            Are you sure you want to <strong>{visibilityTarget?.makeActive ? "restore" : "hide"}</strong> brand{" "}
                            <strong>{visibilityTarget?.brand?.name}</strong>?
                        </p>
                        <p className="text-sm text-slate-500">Brand Code: {visibilityTarget?.brand?.brandCode}</p>
                        <p className="text-sm text-amber-700">
                            Brands are not deleted from the database. Hidden brands are excluded from brand selections when creating products. Hiding a brand is blocked while related products are pending approval.
                        </p>
                        <Checkbox checked={visibilityConfirmed}
                            onChange={(event) => setVisibilityConfirmed(event.target.checked)}
                            label="I confirm this brand status change." />
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
}
