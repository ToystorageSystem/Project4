import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listSuppliers } from "../../../api/business/suppliers";
import {
    createSupplierProductLink,
    deleteSupplierProductLink,
    getLinksByProduct,
    setDefaultSupplierLink,
    supplierProductErrorMessage,
    updateSupplierProductLink,
} from "../../../api/business/supplierProducts";
import Alert from "../../ui/Alert";
import Button from "../../ui/Button";
import Card from "../../ui/Card";
import Checkbox from "../../ui/Checkbox";
import EmptyState from "../../ui/EmptyState";
import Input from "../../ui/Input";
import Modal from "../../ui/Modal";
import Select from "../../ui/Select";
import StatusBadge from "../../ui/StatusBadge";
const businessStatusLabel = (value) => ({ ACTIVE: "Active", INACTIVE: "Inactive", REJECTED: "Rejected", PENDING: "Pending Approval" }[value] || value);
import Table from "../../ui/Table";

const EMPTY_FORM = {
    supplierId: "",
    supplierProductCode: "",
    purchasePrice: "",
    leadTimeDays: "0",
    minimumOrderQuantity: "1",
    status: "ACTIVE",
    defaultSupplier: false,
};
const formatMoney = (value) => value == null ? "—" : `${Number(value).toLocaleString("en-GB")} ₫`;
const display = (value) => value == null || value === "" ? "—" : value;

function validationErrors(form, creating, linkedSupplierIds) {
    const errors = {};
    if (creating && (!Number.isSafeInteger(Number(form.supplierId)) || Number(form.supplierId) <= 0)) {
        errors.supplierId = "Select a valid supplier.";
    } else if (creating && linkedSupplierIds.has(Number(form.supplierId))) {
        errors.supplierId = "This supplier is already linked to the product, including inactive links. Edit the existing link instead.";
    }
    if (form.supplierProductCode.trim().length > 100) {
        errors.supplierProductCode = "Maximum 100 characters.";
    }
    const rawPrice = form.purchasePrice.trim();
    if (!/^\d+(\.\d{1,2})?$/.test(rawPrice) || Number(rawPrice) < 0.01 || !Number.isFinite(Number(rawPrice))) {
        errors.purchasePrice = "Supplier purchase price must be greater than zero, with at most two decimal places.";
    }
    if (!/^\d+$/.test(form.leadTimeDays) || !Number.isSafeInteger(Number(form.leadTimeDays))) {
        errors.leadTimeDays = "Lead time must be a non-negative integer.";
    }
    if (!/^\d+$/.test(form.minimumOrderQuantity) || !Number.isSafeInteger(Number(form.minimumOrderQuantity)) || Number(form.minimumOrderQuantity) < 1) {
        errors.minimumOrderQuantity = "Minimum order quantity must be an integer of at least 1.";
    }
    if (!creating && !["ACTIVE", "INACTIVE"].includes(form.status)) {
        errors.status = "Invalid status.";
    }
    return errors;
}

export default function SupplierProductLinksPanel({ productId, productStatus, referencePurchasePrice }) {
    const [links, setLinks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [notice, setNotice] = useState("");
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);
    const [action, setAction] = useState(null); // create | edit | default | delete
    const [form, setForm] = useState(EMPTY_FORM);
    const [errors, setErrors] = useState({});
    const [deleteText, setDeleteText] = useState("");
    const [saving, setSaving] = useState(false);
    const [supplierOptions, setSupplierOptions] = useState([]);
    const [supplierPage, setSupplierPage] = useState(0);
    const [supplierTotalPages, setSupplierTotalPages] = useState(0);
    const [supplierKeywordDraft, setSupplierKeywordDraft] = useState("");
    const [supplierKeyword, setSupplierKeyword] = useState("");
    const [supplierLoading, setSupplierLoading] = useState(false);
    const [supplierError, setSupplierError] = useState("");
    const canManage = productStatus === "ACTIVE";

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        getLinksByProduct(productId, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result)) throw new Error("Invalid product–supplier association data");
                setLinks(result);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setLoadError(supplierProductErrorMessage(requestError, "Unable to load linked suppliers."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [productId, reload]);

    useEffect(() => {
        if (action?.type !== "create") return;
        const controller = new AbortController();
        setSupplierLoading(true);
        setSupplierError("");
        listSuppliers({ keyword: supplierKeyword || undefined, status: "ACTIVE", page: supplierPage, size: 20, sortBy: "name", direction: "ASC" }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.content)) throw new Error("Invalid supplier list data");
                setSupplierOptions(result.content);
                setSupplierTotalPages(result.totalPages ?? 0);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setSupplierError(supplierProductErrorMessage(requestError, "Unable to load suppliers."));
            })
            .finally(() => { if (!controller.signal.aborted) setSupplierLoading(false); });
        return () => controller.abort();
    }, [action?.type, supplierKeyword, supplierPage]);

    const openAction = (type, link = null) => {
        if (!canManage) return;
        setNotice("");
        setError("");
        setErrors({});
        setDeleteText("");
        if (type === "create") {
            setSupplierPage(0);
            setSupplierKeyword("");
            setSupplierKeywordDraft("");
            setSupplierOptions([]);
            // Chi goi y gia tham khao khi tao lien ket NCC moi.
            // Khi sua lien ket cu, su dung gia da luu cua NCC.
            const price = Number(referencePurchasePrice);
            setForm({
                ...EMPTY_FORM,
                purchasePrice: Number.isFinite(price) && price > 0 ? String(price) : "",
            });
        } else if (type === "edit" && link) {
            setForm({
                supplierId: String(link.supplierId ?? ""),
                supplierProductCode: link.supplierProductCode ?? "",
                purchasePrice: String(link.purchasePrice ?? ""),
                leadTimeDays: String(link.leadTimeDays ?? 0),
                minimumOrderQuantity: String(link.minimumOrderQuantity ?? 1),
                status: link.status ?? "ACTIVE",
                defaultSupplier: Boolean(link.defaultSupplier),
            });
        }
        setAction({ type, link });
    };

    const change = (field) => (event) => {
        const value = event.target.type === "checkbox" ? event.target.checked : event.target.value;
        setForm((current) => ({ ...current, [field]: value }));
        setErrors((current) => ({ ...current, [field]: undefined }));
    };

    const saveForm = async (event) => {
        event.preventDefault();
        if (saving || !canManage || !action || !["create", "edit"].includes(action.type)) return;
        const creating = action.type === "create";
        const nextErrors = validationErrors(form, creating, new Set(links.map((link) => Number(link.supplierId))));
        if (!creating && action.link?.defaultSupplier && form.status === "INACTIVE") {
            nextErrors.status = "Cannot deactivate the default supplier. Select another default supplier first.";
        }
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length) return;
        const payload = {
            supplierProductCode: form.supplierProductCode.trim() || null,
            purchasePrice: Number(form.purchasePrice),
            leadTimeDays: Number(form.leadTimeDays),
            minimumOrderQuantity: Number(form.minimumOrderQuantity),
        };
        setSaving(true);
        setError("");
        try {
            if (creating) {
                await createSupplierProductLink({
                    ...payload,
                    productId: Number(productId),
                    supplierId: Number(form.supplierId),
                    defaultSupplier: form.defaultSupplier,
                });
            } else {
                await updateSupplierProductLink(action.link.id, { ...payload, status: form.status });
            }
            setNotice(creating ? "Supplier association created." : "Supplier association updated.");
            setAction(null);
            setReload((current) => current + 1);
        } catch (requestError) {
            setError(supplierProductErrorMessage(requestError, "Unable to save supplier association."));
        } finally {
            setSaving(false);
        }
    };

    const confirmAction = async () => {
        if (saving || !canManage || !action?.link) return;
        if (action.type === "default" && (action.link.status !== "ACTIVE" || action.link.supplierStatus !== "ACTIVE")) return;
        if (action.type === "delete" && (deleteText !== "XOA" || action.link.defaultSupplier)) return;
        setSaving(true);
        setError("");
        try {
            if (action.type === "default") {
                await setDefaultSupplierLink(action.link.id);
                setNotice(`Selected ${action.link.supplierName} as the default supplier.`);
            } else if (action.type === "delete") {
                await deleteSupplierProductLink(action.link.id);
                setNotice("Supplier association permanently deleted.");
            } else return;
            setAction(null);
            setReload((current) => current + 1);
        } catch (requestError) {
            setError(supplierProductErrorMessage(requestError, "Unable to complete the operation."));
        } finally {
            setSaving(false);
        }
    };

    const columns = [
        {
            key: "supplierName", label: "Supplier",
            render: (_value, link) => (
                <div className="space-y-1">
                    <Link to={`/business/suppliers/${link.supplierId}`} className="font-semibold text-[#c94810] hover:underline">{link.supplierName || link.supplierCode || "—"}</Link>
                    <div className="text-xs text-slate-500">{display(link.supplierCode)} · {display(link.linkCode)}</div>
                    <StatusBadge status={link.supplierStatus} label={businessStatusLabel(link.supplierStatus)} />
                </div>
            ),
        },
        { key: "supplierProductCode", label: "Supplier Product Code", render: display },
        { key: "purchasePrice", label: "Supplier Purchase Price", render: formatMoney },
        { key: "minimumOrderQuantity", label: "Minimum Order Quantity", render: display },
        { key: "status", label: "Association", render: (value, link) => <div className="space-y-1"><StatusBadge status={value} label={businessStatusLabel(value)} />{link.defaultSupplier && <div className="text-xs font-semibold text-[#c94810]">★ Preferred Supplier</div>}</div> },
        {
            key: "id", label: "Actions",
            render: (_value, link) => (
                <div className="flex flex-wrap gap-2">
                    <Button type="button" variant="outline" size="sm" disabled={!canManage} onClick={() => openAction("edit", link)}>Edit</Button>
                    {!link.defaultSupplier && <Button type="button" variant="secondary" size="sm" disabled={!canManage || link.status !== "ACTIVE" || link.supplierStatus !== "ACTIVE"} onClick={() => openAction("default", link)}>Set as Default</Button>}
                    <Button type="button" variant="danger" size="sm" disabled={!canManage || Boolean(link.defaultSupplier)} onClick={() => openAction("delete", link)}>Delete</Button>
                </div>
            ),
        },
    ];

    const editingForm = action?.type === "create" || action?.type === "edit";
    const creating = action?.type === "create";
    return (
        <section className="space-y-4">
            <Card>
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <h2 className="text-lg font-bold text-slate-900">Linked Suppliers ({links.length})</h2>
                        <p className="mt-1 text-sm text-slate-500">
                            Manage purchase prices, minimum order quantities, and preferred suppliers
                        </p>
                    </div>
                    <Button type="button" disabled={!canManage} onClick={() => openAction("create")}>+ Link Supplier</Button>
                </div>
                {!canManage && <div className="mt-4"><Alert type="warning">Supplier associations can be changed only for active products. Existing associations remain visible for reference.</Alert></div>}
                {notice && <div role="status" className="mt-4"><Alert type="success">{notice}</Alert></div>}
                {loadError && <div role="alert" className="mt-4"><Alert type="danger">{loadError}</Alert></div>}
                {!loading && !loadError && <p className="mt-4 text-xs text-slate-500">Yes {links.filter((link) => link.status === "ACTIVE").length} active associations. Inactive associations are retained and can be restored if needed.</p>}
            </Card>
            {loading ? <p role="status" className="text-sm text-slate-600">Loading linked suppliers...</p>
                : !loadError && (links.length ? <Table columns={columns} data={links} />
                    : <EmptyState title="No Linked Suppliers" description="This product has no linked suppliers. Select Link Supplier to add one." />)}
            <Modal
                open={Boolean(action)}
                title={creating ? "Add Supplier Association" : action?.type === "edit" ? "Update Association" : action?.type === "default" ? "Set Default Supplier" : "Permanently Delete Association"}
                onClose={() => { if (!saving) { setAction(null); setError(""); } }}
                footer={<>
                    <Button type="button" variant="ghost" disabled={saving} onClick={() => { setAction(null); setError(""); }}>Cancel</Button>
                    <Button
                        type={editingForm ? "submit" : "button"}
                        form={editingForm ? "supplier-product-link-form" : undefined}
                        variant={action?.type === "delete" ? "danger" : "primary"}
                        disabled={saving || (action?.type === "delete" && deleteText !== "XOA")}
                        onClick={editingForm ? undefined : confirmAction}
                    >{saving ? "Processing..." : creating ? "Create Association" : action?.type === "edit" ? "Save Changes" : action?.type === "default" ? "Confirm Default Supplier" : "Delete Permanently"}</Button>
                </>}
            >
                <div className="max-h-[65vh] space-y-4 overflow-y-auto pr-1">
                    {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                    {editingForm ? (
                        <form id="supplier-product-link-form" onSubmit={saveForm} className="space-y-4" noValidate>
                            {creating ? (
                                <div className="space-y-3 rounded-xl border border-slate-200 p-3">
                                    <div>
                                        <Input
                                            label="Find an active supplier"
                                            placeholder="Enter supplier name or code"
                                            value={supplierKeywordDraft}
                                            onChange={(event) => setSupplierKeywordDraft(event.target.value)}
                                            onKeyDown={(event) => {
                                                // Enter in the search field must never submit the link-creation form.
                                                if (event.key === "Enter") {
                                                    event.preventDefault();
                                                    setSupplierPage(0);
                                                    setSupplierKeyword(supplierKeywordDraft.trim());
                                                    setForm((current) => ({ ...current, supplierId: "" }));
                                                }
                                            }}
                                        />
                                        <Button type="button" size="sm" variant="outline" className="mt-2" onClick={() => { setSupplierPage(0); setSupplierKeyword(supplierKeywordDraft.trim()); setForm((current) => ({ ...current, supplierId: "" })); }}>Find Supplier</Button>
                                    </div>
                                    {supplierError && <Alert type="danger">{supplierError}</Alert>}
                                    {supplierLoading ? <p className="text-sm text-slate-500">Loading suppliers...</p> : <Select
                                        label="Supplier *" value={form.supplierId} error={errors.supplierId}
                                        onChange={change("supplierId")}
                                        options={[
    {
        value: "",
        label: "-- Select Supplier --",
    },
    ...supplierOptions
        .filter(
            (supplier) =>
                !links.some(
                    (link) =>
                        Number(link.supplierId) === Number(supplier.id)
                )
        )
        .map((supplier) => ({
            value: String(supplier.id),
            label: `${supplier.name} (${supplier.supplierCode})`,
        })),
]}
                                    />}
                                    <div className="flex items-center justify-between text-xs text-slate-500">
                                        <span>Trang {supplierPage + 1}/{Math.max(1, supplierTotalPages)}</span>
                                        <div className="flex gap-2">
                                            <Button type="button" size="sm" variant="ghost" disabled={supplierPage <= 0 || supplierLoading} onClick={() => { setSupplierPage((page) => page - 1); setForm((current) => ({ ...current, supplierId: "" })); }}>Previous</Button>
                                            <Button type="button" size="sm" variant="ghost" disabled={supplierPage + 1 >= supplierTotalPages || supplierLoading} onClick={() => { setSupplierPage((page) => page + 1); setForm((current) => ({ ...current, supplierId: "" })); }}>Sau</Button>
                                        </div>
                                    </div>
                                </div>
                            ) : <p className="text-sm text-slate-700">Linked to <strong>{action.link?.supplierName}</strong> (association ID {action.link?.id}). To change the supplier, create a new association.</p>}
                            <Input label="Product code used by the supplier (maximum 100 characters)" value={form.supplierProductCode} maxLength={100} onChange={change("supplierProductCode")} error={errors.supplierProductCode} />
                            <Input label="Supplier Purchase Price (VND) *" type="number" min="0.01" step="0.01" value={form.purchasePrice} onChange={change("purchasePrice")} error={errors.purchasePrice} />
                            {creating && Number(referencePurchasePrice) > 0 && (
                                <p className="text-xs text-slate-500">Pre-filled from the product reference price. Adjust it to match the supplier's quote.</p>
                            )}
                            <Input label="Minimum Order Quantity *" type="number" min="1" step="1" value={form.minimumOrderQuantity} onChange={change("minimumOrderQuantity")} error={errors.minimumOrderQuantity} />
                            {creating ? <Checkbox checked={form.defaultSupplier} onChange={change("defaultSupplier")} label="Set as the default supplier when creating this association (replaces the current default)." />
                                : <Select label="Association Status" value={form.status} onChange={change("status")} error={errors.status} options={[{ value: "ACTIVE", label: "Active" }, { value: "INACTIVE", label: "Inactive" }]} />}
                            <p className="text-xs text-slate-500">Only purchasing terms and association status can be edited. The product and supplier records remain unchanged.</p>
                        </form>
                    ) : action?.type === "default" ? (
                        <p className="text-sm text-slate-700">Set <strong>{action.link?.supplierName}</strong> as the default supplier for this product? The current default supplier will be replaced.</p>
                    ) : action?.type === "delete" ? (
                        <div className="space-y-3 text-sm text-slate-700">
                            <Alert type="danger">Warning: This association will be deleted permanently and cannot currently be restored from the activity history. Proceed only if authorized.</Alert>
                            <p>Delete association with <strong>{action.link?.supplierName}</strong> (ID {action.link?.linkCode})?</p>
                            <Input label='Type XOA to confirm' value={deleteText} onChange={(event) => setDeleteText(event.target.value)} placeholder="XOA" />
                        </div>
                    ) : null}
                </div>
            </Modal>
        </section>
    );
}

