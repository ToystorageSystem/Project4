import { useEffect, useState } from "react";
import { createSupplier, listSuppliers, supplierErrorMessage } from "../../../api/business/suppliers";
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
import EmptyState from "../../ui/EmptyState";
import Input from "../../ui/Input";
import Modal from "../../ui/Modal";
import Select from "../../ui/Select";
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
    return errors;
}

export default function SupplierProductLinksPanel({ productId, productStatus }) {
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
    const [showSupplierCreate, setShowSupplierCreate] = useState(false);
    const [supplierCreateName, setSupplierCreateName] = useState("");
    const [supplierCreateContact, setSupplierCreateContact] = useState("");
    const [supplierCreateEmail, setSupplierCreateEmail] = useState("");
    const [supplierCreateError, setSupplierCreateError] = useState("");
    const [supplierCreateBusy, setSupplierCreateBusy] = useState(false);
    const [supplierCreateNotice, setSupplierCreateNotice] = useState("");
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
            setShowSupplierCreate(false);
            setSupplierCreateName("");
            setSupplierCreateContact("");
            setSupplierCreateEmail("");
            setSupplierCreateError("");
            setSupplierCreateNotice("");
            // Gia nhap chi nhap sau khi chon nha cung cap, khong lay gia cu mac dinh.
            setForm({ ...EMPTY_FORM });
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
                    defaultSupplier: links.length === 0,
                });
            } else {
                await updateSupplierProductLink(action.link.id, { ...payload, status: action.link.status ?? "ACTIVE" });
            }
            setNotice(creating ? "Supplier added and purchase price synchronized." : "Supplier and product purchase prices updated together.");
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

    // Tao Supplier ngay trong trang Edit Product; khong tu dong gan Supplier vao Product.
    // Nguoi dung xac nhan Create Association o buoc tiep theo de luu gia/MOQ.
    const handleCreateSupplierInline = async () => {
        if (supplierCreateBusy) return;
        const name = supplierCreateName.trim();
        const email = supplierCreateEmail.trim();
        if (!name || name.length > 200) {
            setSupplierCreateError("Supplier name is required (maximum 200 characters).");
            return;
        }
        if (email && (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 150)) {
            setSupplierCreateError("Enter a valid supplier email address.");
            return;
        }
        setSupplierCreateBusy(true);
        setSupplierCreateError("");
        setSupplierCreateNotice("");
        try {
            const created = await createSupplier({
                name,
                contactPerson: supplierCreateContact.trim() || null,
                email: email || null,
                taxCode: null,
                contactPosition: null,
                phone: null,
                address: null,
                note: "Created from Product Supplier Setup",
            });
            if (!created?.id) throw new Error("Supplier was saved but no ID was returned. Check the supplier list before retrying.");
            setSupplierOptions((previous) => [created, ...previous.filter((x) => Number(x.id) !== Number(created.id))]);
            setForm((previous) => ({ ...previous, supplierId: String(created.id) }));
            setSupplierCreateNotice("Supplier created. Review the purchase price and minimum quantity, then save the association.");
            setShowSupplierCreate(false);
            setSupplierCreateName("");
            setSupplierCreateContact("");
            setSupplierCreateEmail("");
        } catch (requestError) {
            setSupplierCreateError(supplierErrorMessage(requestError, "Unable to create supplier."));
        } finally {
            setSupplierCreateBusy(false);
        }
    };

    const columns = [
        {
            key: "supplierName",
            label: "Supplier",
            render: (_value, link) => (
                <div>
                    <p className="font-semibold text-slate-900">{link.supplierName || link.supplierCode || "—"}</p>
                    <p className="mt-1 text-xs text-slate-500">{display(link.supplierCode)}</p>
                </div>
            ),
        },
        { key: "purchasePrice", label: "Purchase Price (VND)", render: formatMoney },
        { key: "minimumOrderQuantity", label: "Minimum Order Quantity", render: display },
        {
            key: "id",
            label: "Actions",
            render: (_value, link) => (
                <div className="flex flex-wrap gap-2">
                    <Button type="button" variant="outline" size="sm" disabled={!canManage} onClick={() => openAction("edit", link)}>Edit</Button>
                    {links.length > 1 && !link.defaultSupplier && (
                        <Button type="button" variant="secondary" size="sm" disabled={!canManage || link.status !== "ACTIVE" || link.supplierStatus !== "ACTIVE"} onClick={() => openAction("default", link)}>Set as Default</Button>
                    )}
                    {!link.defaultSupplier && (
                        <Button type="button" variant="danger" size="sm" disabled={!canManage} onClick={() => openAction("delete", link)}>Delete</Button>
                    )}
                </div>
            ),
        },
    ];

    const editingForm = action?.type === "create" || action?.type === "edit";
    const creating = action?.type === "create";
    return (
        <section className="space-y-4">
            <Card>
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <h2 className="text-lg font-bold text-slate-900">Supplier Information</h2>
                    {!loading && links.length === 0 && (
                        <Button type="button" disabled={!canManage} onClick={() => openAction("create")}>+ Add Supplier</Button>
                    )}
                </div>

                {notice && <div role="status" className="mb-4"><Alert type="success">{notice}</Alert></div>}
                {loadError && <div role="alert" className="mb-4"><Alert type="danger">{loadError}</Alert></div>}
                {!canManage && <div className="mb-4"><Alert type="warning">Supplier information is read-only for this product.</Alert></div>}

                {loading ? (
                    <p role="status" className="text-sm text-slate-600">Loading supplier information...</p>
                ) : !loadError && (
                    links.length > 0
                        ? <Table columns={columns} data={links} />
                        : <EmptyState title="No Supplier" description="This product does not have a supplier yet." />
                )}
            </Card>
            <Modal
                open={Boolean(action)}
                title={creating ? "Add Supplier" : action?.type === "edit" ? "Edit Supplier Information" : action?.type === "default" ? "Set Default Supplier" : "Remove Supplier"}
                onClose={() => { if (!saving) { setAction(null); setError(""); } }}
                footer={<>
                    <Button type="button" variant="ghost" disabled={saving} onClick={() => { setAction(null); setError(""); }}>Cancel</Button>
                    <Button
                        type={editingForm ? "submit" : "button"}
                        form={editingForm ? "supplier-product-link-form" : undefined}
                        variant={action?.type === "delete" ? "danger" : "primary"}
                        disabled={saving || (action?.type === "delete" && deleteText !== "XOA")}
                        onClick={editingForm ? undefined : confirmAction}
                    >{saving ? "Processing..." : creating ? "Save Supplier" : action?.type === "edit" ? "Save Changes" : action?.type === "default" ? "Confirm Default Supplier" : "Delete Permanently"}</Button>
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
                                        onChange={(event) => setForm((current) => ({ ...current, supplierId: event.target.value, purchasePrice: "" }))}
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
                                    <div className="rounded-lg border border-slate-200 bg-white p-3">
                                        <Button type="button" variant="outline" size="sm" onClick={() => { setShowSupplierCreate((old) => !old); setSupplierCreateError(""); }}>
                                            {showSupplierCreate ? "Close New Supplier" : "+ Create New Supplier"}
                                        </Button>
                                        {supplierCreateNotice && <div className="mt-2"><Alert type="success">{supplierCreateNotice}</Alert></div>}
                                        {showSupplierCreate && <div className="mt-3 grid gap-3">
                                            <Input label="New Supplier Name *" value={supplierCreateName} onChange={(event) => setSupplierCreateName(event.target.value)} maxLength={200} onKeyDown={(event) => { if (event.key === "Enter") event.preventDefault(); }} />
                                            <Input label="Contact Person (optional)" value={supplierCreateContact} onChange={(event) => setSupplierCreateContact(event.target.value)} maxLength={150} onKeyDown={(event) => { if (event.key === "Enter") event.preventDefault(); }} />
                                            <Input label="Email (optional)" type="email" value={supplierCreateEmail} onChange={(event) => setSupplierCreateEmail(event.target.value)} maxLength={150} onKeyDown={(event) => { if (event.key === "Enter") event.preventDefault(); }} />
                                            {supplierCreateError && <Alert type="danger">{supplierCreateError}</Alert>}
                                            <div className="flex justify-end"><Button type="button" disabled={supplierCreateBusy} onClick={handleCreateSupplierInline}>{supplierCreateBusy ? "Creating Supplier..." : "Create Supplier"}</Button></div>
                                        </div>}
                                    </div>
                                    <div className="flex items-center justify-between text-xs text-slate-500">
                                        <span>Trang {supplierPage + 1}/{Math.max(1, supplierTotalPages)}</span>
                                        <div className="flex gap-2">
                                            <Button type="button" size="sm" variant="ghost" disabled={supplierPage <= 0 || supplierLoading} onClick={() => { setSupplierPage((page) => page - 1); setForm((current) => ({ ...current, supplierId: "" })); }}>Previous</Button>
                                            <Button type="button" size="sm" variant="ghost" disabled={supplierPage + 1 >= supplierTotalPages || supplierLoading} onClick={() => { setSupplierPage((page) => page + 1); setForm((current) => ({ ...current, supplierId: "" })); }}>Sau</Button>
                                        </div>
                                    </div>
                                </div>
                            ) : <p className="text-sm text-slate-700">Linked to <strong>{action.link?.supplierName}</strong> (association ID {action.link?.id}). To change the supplier, create a new association.</p>}
                            {(!creating || Boolean(form.supplierId)) && (
                                <Input label="Purchase Price (VND) *" type="number" min="0.01" step="0.01" value={form.purchasePrice} onChange={change("purchasePrice")} error={errors.purchasePrice} />
                            )}
                            <Input label="Minimum Order Quantity *" type="number" min="1" step="1" value={form.minimumOrderQuantity} onChange={change("minimumOrderQuantity")} error={errors.minimumOrderQuantity} />
                            <p className="text-xs text-slate-500">When this is the product's only supplier, updating the purchase price also updates the Product purchase price.</p>
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

