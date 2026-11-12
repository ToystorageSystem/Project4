import { useEffect, useState } from "react";
import { brandErrorMessage, createBrand, updateBrand } from "../../../api/business/brands";
import Alert from "../../ui/Alert";
import Button from "../../ui/Button";
import Input from "../../ui/Input";
import Modal from "../../ui/Modal";

const EMPTY_FORM = { brandCode: "", name: "", country: "", description: "" };

// Popup them/sua thuong hieu. Dung chung cho trang Brand Management
// va nut "+ New" trong form Add Product.
export default function BrandFormModal({ open, brand = null, onClose, onSaved }) {
    const editing = Boolean(brand);
    const [form, setForm] = useState(EMPTY_FORM);
    const [formError, setFormError] = useState("");
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (!open) return;
        setForm(brand ? {
            brandCode: brand.brandCode || "",
            name: brand.name || "",
            country: brand.country || "",
            description: brand.description || "",
        } : { ...EMPTY_FORM });
        setFormError("");
    }, [open, brand]);

    const change = (field) => (event) => {
        setForm((current) => ({ ...current, [field]: event.target.value }));
        setFormError("");
    };

    const close = () => {
        if (!saving) onClose?.();
    };

    const save = async (event) => {
        event?.preventDefault();
        if (saving) return;

        const payload = {
            brandCode: form.brandCode.trim().toUpperCase(),
            name: form.name.trim(),
            country: form.country.trim() || null,
            description: form.description.trim() || null,
        };

        if (!payload.brandCode || !payload.name) {
            setFormError("Brand code and name are required.");
            return;
        }
        if (payload.brandCode.length > 50 || payload.name.length > 150
            || (payload.country || "").length > 100 || (payload.description || "").length > 1000) {
            setFormError("Information exceeds the maximum allowed length.");
            return;
        }

        setSaving(true);
        setFormError("");
        try {
            const saved = editing ? await updateBrand(brand.id, payload) : await createBrand(payload);
            onSaved?.(saved);
        } catch (requestError) {
            setFormError(brandErrorMessage(requestError, "Unable to save brand."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <Modal
            open={open}
            title={editing ? "Edit Brand" : "Add Brand"}
            onClose={close}
            footer={<>
                <Button variant="ghost" disabled={saving} onClick={close}>Cancel</Button>
                <Button disabled={saving} onClick={save}>{saving ? "Saving..." : "Save Brand"}</Button>
            </>}
        >
            <form onSubmit={save} noValidate className="space-y-4">
                {formError && <Alert type="danger">{formError}</Alert>}
                <Input label="Brand Code *" placeholder="Example: BR-LEGO" maxLength={50} required
                    value={form.brandCode} onChange={change("brandCode")} />
                <Input label="Brand Name *" placeholder="Enter brand name" maxLength={150} required
                    value={form.name} onChange={change("name")} />
                <Input label="Country" placeholder="Example: Vietnam" maxLength={100}
                    value={form.country} onChange={change("country")} />
                <label className="block space-y-1.5">
                    <span className="text-sm font-medium text-slate-700">Description</span>
                    <textarea rows={3} maxLength={1000} value={form.description} onChange={change("description")}
                        placeholder="Enter a description (optional)"
                        className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/20" />
                    <span className="block text-xs text-slate-500">{form.description.length}/1000 characters</span>
                </label>
                {/* Cho phep Enter de submit */}
                <button type="submit" className="hidden" aria-hidden="true" tabIndex={-1} />
            </form>
        </Modal>
    );
}
