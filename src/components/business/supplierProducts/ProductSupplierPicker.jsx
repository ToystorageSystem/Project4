import { useEffect, useMemo, useState } from "react";
import { createSupplier, listSuppliers, supplierErrorMessage } from "../../../api/business/suppliers";
import Alert from "../../ui/Alert";
import Button from "../../ui/Button";
import Input from "../../ui/Input";
import Select from "../../ui/Select";

// Chon mot nha cung cap cho mot Product moi; gia nhap chi xuat hien sau khi chon.
export default function ProductSupplierPicker({ value, onChange, disabled = false, errors = {} }) {
    const [keywordDraft, setKeywordDraft] = useState("");
    const [keyword, setKeyword] = useState("");
    const [page, setPage] = useState(0);
    const [options, setOptions] = useState([]);
    const [createdOption, setCreatedOption] = useState(null);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(false);
    const [loadError, setLoadError] = useState("");
    const [showCreate, setShowCreate] = useState(false);
    const [supplierName, setSupplierName] = useState("");
    const [contactPerson, setContactPerson] = useState("");
    const [phone, setPhone] = useState("");
    const [email, setEmail] = useState("");
    const [createBusy, setCreateBusy] = useState(false);
    const [createError, setCreateError] = useState("");
    const [createdNotice, setCreatedNotice] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        listSuppliers({ keyword: keyword || undefined, status: "ACTIVE", page, size: 20, sortBy: "name", direction: "ASC" }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.content)) throw new Error("Invalid supplier options");
                setOptions(result.content);
                setTotalPages(result.totalPages || 0);
            })
            .catch((error) => {
                if (!controller.signal.aborted) setLoadError(supplierErrorMessage(error, "Unable to load suppliers."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [keyword, page]);

    const selectOptions = useMemo(() => {
        const found = new Map();
        for (const supplier of [createdOption, ...options]) {
            if (supplier?.id) found.set(String(supplier.id), {
                value: String(supplier.id),
                label: `${supplier.name} (${supplier.supplierCode || supplier.code || `ID ${supplier.id}`})`,
            });
        }
        return [{ value: "", label: "-- Select Supplier --" }, ...found.values()];
    }, [options, createdOption]);

    const change = (field, next) => onChange((current) => ({ ...current, [field]: next }));
    const findSupplier = () => { setPage(0); setKeyword(keywordDraft.trim()); };

    const createNewSupplier = async () => {
        const name = supplierName.trim();
        const normalizedEmail = email.trim();
        const normalizedPhone = phone.trim();
        if (!name || name.length > 200) {
            setCreateError("Supplier name is required (maximum 200 characters).");
            return;
        }
        if (normalizedEmail && (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail) || normalizedEmail.length > 150)) {
            setCreateError("Enter a valid supplier email address.");
            return;
        }
        if (normalizedPhone && !/^\+?[0-9]{9,15}$/.test(normalizedPhone)) {
            setCreateError("Phone number must contain 9 to 15 digits (optional + prefix).");
            return;
        }
        setCreateBusy(true);
        setCreateError("");
        try {
            const created = await createSupplier({
                name,
                contactPerson: contactPerson.trim() || null,
                email: normalizedEmail || null,
                contactPosition: null,
                taxCode: null,
                phone: normalizedPhone || null,
                address: null,
                note: "Created from Add Product",
            });
            if (!created?.id) throw new Error("Supplier was saved but no ID was returned. Check existing suppliers before retrying.");
            setCreatedOption(created);
            onChange((current) => ({ ...current, supplierId: String(created.id) }));
            setShowCreate(false);
            setSupplierName("");
            setContactPerson("");
            setPhone("");
            setEmail("");
            setCreatedNotice("Supplier created and selected. The product has not been saved yet.");
        } catch (error) {
            setCreateError(supplierErrorMessage(error, "Unable to create supplier."));
        } finally {
            setCreateBusy(false);
        }
    };

    return (
        <section className="space-y-4 rounded-xl border border-slate-200 p-4">
            <div>
                <h2 className="text-base font-semibold text-slate-900">Product Supplier</h2>
                <p className="mt-1 text-xs text-slate-500">One supplier per product code. To purchase the same item from another supplier, create a separate product code.</p>
            </div>
            <div className="grid gap-2 sm:grid-cols-[1fr_auto] sm:items-end">
                <Input label="Find an active supplier" placeholder="Search by supplier name or code" value={keywordDraft}
                    disabled={disabled || createBusy} onChange={(event) => setKeywordDraft(event.target.value)}
                    onKeyDown={(event) => { if (event.key === "Enter") { event.preventDefault(); findSupplier(); } }} />
                <Button type="button" variant="outline" disabled={disabled || createBusy} onClick={findSupplier}>Find Supplier</Button>
            </div>
            {loadError && <Alert type="danger">{loadError}</Alert>}
            <Select label="Supplier *" value={value.supplierId} disabled={disabled || createBusy || loading}
                options={selectOptions} error={errors.supplierId}
                onChange={(event) => onChange((current) => ({ ...current, supplierId: event.target.value, purchasePrice: "" }))} />
            <div className="flex items-center justify-between gap-2 text-xs text-slate-500">
                <span>Page {page + 1} / {Math.max(1, totalPages)}</span>
                <div className="flex gap-2">
                    <Button type="button" size="sm" variant="ghost" disabled={disabled || loading || page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
                    <Button type="button" size="sm" variant="ghost" disabled={disabled || loading || page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Next</Button>
                </div>
            </div>
            <Button type="button" size="sm" variant="outline" disabled={disabled || createBusy} onClick={() => { setShowCreate((old) => !old); setCreateError(""); }}>
                {showCreate ? "Close New Supplier" : "+ Create New Supplier"}
            </Button>
            {createdNotice && <Alert type="success">{createdNotice}</Alert>}
            {showCreate && <div className="space-y-3 rounded-xl border border-orange-100 bg-orange-50 p-4">
                <Input label="New Supplier Name *" maxLength={200} value={supplierName} disabled={createBusy}
                    onChange={(event) => setSupplierName(event.target.value)} />
                <Input label="Contact Person (optional)" maxLength={150} value={contactPerson} disabled={createBusy}
                    onChange={(event) => setContactPerson(event.target.value)} />
                <Input label="Phone Number (optional)" type="tel" maxLength={16} value={phone} disabled={createBusy}
                    onChange={(event) => setPhone(event.target.value)}
                    placeholder="Example: 0901234567 or +84901234567" />
                <Input label="Email (optional)" type="email" maxLength={150} value={email} disabled={createBusy}
                    onChange={(event) => setEmail(event.target.value)} />
                {createError && <Alert type="danger">{createError}</Alert>}
                <Button type="button" disabled={disabled || createBusy} onClick={createNewSupplier}>
                    {createBusy ? "Creating Supplier..." : "Create Supplier"}
                </Button>
            </div>}
            {value.supplierId && <div className="grid gap-4 border-t border-slate-200 pt-4 md:grid-cols-2">
                <Input label="Purchase Price (VND) *" type="number" min="0.01" step="0.01" required
                    value={value.purchasePrice} disabled={disabled} error={errors.purchasePrice}
                    onChange={(event) => change("purchasePrice", event.target.value)} placeholder="120000" />
                <Input label="Supplier Product Code (optional)" maxLength={100} value={value.supplierProductCode}
                    disabled={disabled} error={errors.supplierProductCode}
                    onChange={(event) => change("supplierProductCode", event.target.value)} placeholder="Example: SUP-TOY-001" />
                <Input label="Minimum Order Quantity *" type="number" min="1" step="1" required
                    value={value.minimumOrderQuantity} disabled={disabled} error={errors.minimumOrderQuantity}
                    onChange={(event) => change("minimumOrderQuantity", event.target.value)} />
            </div>}
        </section>
    );
}
