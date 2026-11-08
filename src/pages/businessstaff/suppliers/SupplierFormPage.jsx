import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { createSupplier, getSupplier, supplierErrorMessage, updateSupplier } from "../../../api/business/suppliers";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Checkbox from "../../../components/ui/Checkbox";
import Input from "../../../components/ui/Input";
import Spinner from "../../../components/ui/Spinner";
import TextArea from "../../../components/ui/TextArea";

const ROOT = "/business/suppliers";
const EMPTY_FORM = {
    name: "",
    taxCode: "",
    contactPerson: "",
    contactPosition: "",
    phone: "",
    email: "",
    address: "",
    note: "",
};
const MAX_LENGTHS = {
    name: 200,
    taxCode: 50,
    contactPerson: 150,
    contactPosition: 100,
    email: 150,
    address: 255,
};

function validate(form) {
    const errors = {};
    for (const [field, limit] of Object.entries(MAX_LENGTHS)) {
        if (form[field].trim().length > limit) {
            errors[field] = `Must not exceed ${limit} characters.`;
        }
    }
    if (!form.name.trim()) errors.name = "Please enter a supplier name.";

    const phone = form.phone.trim();
    if (phone && !/^\+?[0-9]{9,15}$/.test(phone)) {
        errors.phone = "Phone number must contain 9–15 digits and may start with +.";
    }
    const email = form.email.trim();
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errors.email = "Invalid email address format.";
    }
    return errors;
}

function toPayload(form) {
    const optional = (value) => value.trim() || null;
    return {
        name: form.name.trim(),
        taxCode: optional(form.taxCode)?.toUpperCase() ?? null,
        contactPerson: optional(form.contactPerson),
        contactPosition: optional(form.contactPosition),
        phone: optional(form.phone),
        email: optional(form.email)?.toLowerCase() ?? null,
        address: optional(form.address),
        note: optional(form.note),
    };
}

export default function SupplierFormPage() {
    const { supplierId } = useParams();
    const editing = Boolean(supplierId);
    const navigate = useNavigate();
    const [form, setForm] = useState(EMPTY_FORM);
    const [supplierCode, setSupplierCode] = useState("");
    const [loading, setLoading] = useState(editing);
    const [loadFailed, setLoadFailed] = useState(false);
    const [saving, setSaving] = useState(false);
    const [confirmedSharedData, setConfirmedSharedData] = useState(false);
    const [errors, setErrors] = useState({});
    const [error, setError] = useState("");

    useEffect(() => {
        if (!editing) {
            setLoadFailed(false);
            setForm(EMPTY_FORM);
            setSupplierCode("");
            setLoading(false);
            return;
        }
        const controller = new AbortController();
        setLoading(true);
        setLoadFailed(false);
        setError("");
        getSupplier(supplierId, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                setSupplierCode(data.supplierCode || "");
                setForm(Object.fromEntries(
                    Object.keys(EMPTY_FORM).map((field) => [field, data[field] ?? ""])
                ));
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) {
                    setLoadFailed(true);
                    setError(supplierErrorMessage(requestError, "Unable to load supplier details."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [editing, supplierId]);

    const change = (field) => (event) => {
        setForm((current) => ({ ...current, [field]: event.target.value }));
        setErrors((current) => ({ ...current, [field]: undefined }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (saving || loadFailed || !confirmedSharedData) return;
        const nextErrors = validate(form);
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length) return;

        setSaving(true);
        setError("");
        try {
            const saved = editing
                ? await updateSupplier(supplierId, toPayload(form))
                : await createSupplier(toPayload(form));
            navigate(`${ROOT}/${saved.id}`, {
                replace: true,
                state: { notice: editing ? "Supplier updated successfully." : "Supplier added successfully." },
            });
        } catch (requestError) {
            setError(supplierErrorMessage(requestError, "Unable to save supplier details."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title={editing ? "Edit Supplier" : "Add Supplier"}
                    description="Enter supplier details. Fields marked * are required."
                    actions={<Button type="button" variant="ghost" onClick={() => navigate(editing ? `${ROOT}/${supplierId}` : ROOT)}>← Back</Button>}
                />
                {loading ? (
                    <div role="status" className="flex items-center gap-3 rounded-2xl bg-white p-8 text-sm text-slate-600"><Spinner /> Loading information...</div>
                ) : loadFailed ? (
                    <Card className="max-w-5xl">
                        <div role="alert"><Alert type="danger">{error}</Alert></div>
                        <Button type="button" variant="ghost" className="mt-4" onClick={() => navigate(ROOT)}>← Back to List</Button>
                    </Card>
                ) : (
                    <Card className="max-w-5xl">
                        {error && <div role="alert" className="mb-5"><Alert type="danger">{error}</Alert></div>}
                        {editing && supplierCode && (
                            <div className="mb-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
                                Supplier Code (read-only): <span className="font-semibold text-slate-900">{supplierCode}</span>
                            </div>
                        )}
                        {!editing && (
                            <div className="mb-5 rounded-xl border border-orange-200 bg-orange-50 p-4 text-sm text-orange-800">
                                Supplier code will be generated automatically after saving.
                            </div>
                        )}
                        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
                            <div className="grid gap-5 md:grid-cols-2">
                                <Input label="Supplier Name *" required maxLength={200} value={form.name} onChange={change("name")} error={errors.name} placeholder="Enter supplier name" />
                                <Input label="Tax ID" maxLength={50} value={form.taxCode} onChange={change("taxCode")} error={errors.taxCode} placeholder="Tax ID (optional)" />
                                <Input label="Contact Person" maxLength={150} value={form.contactPerson} onChange={change("contactPerson")} error={errors.contactPerson} placeholder="Full Name" />
                                <Input label="Contact Position" maxLength={100} value={form.contactPosition} onChange={change("contactPosition")} error={errors.contactPosition} placeholder="Position" />
                                <Input label="Phone Number" type="tel" maxLength={16} value={form.phone} onChange={change("phone")} error={errors.phone} placeholder="09xxxxxxxx" />
                                <Input label="Email" type="email" maxLength={150} value={form.email} onChange={change("email")} error={errors.email} placeholder="supplier@example.com" />
                                <div className="md:col-span-2">
                                    <Input label="Address" maxLength={255} value={form.address} onChange={change("address")} error={errors.address} placeholder="Supplier Address" />
                                </div>
                                <div className="md:col-span-2">
                                    <TextArea label="Notes" rows={4} value={form.note} onChange={change("note")} placeholder="Additional Information (optional)" />
                                </div>
                            </div>

                            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                <Checkbox
                                    checked={confirmedSharedData}
                                    onChange={(event) => setConfirmedSharedData(event.target.checked)}
                                    label="I have reviewed and agree to save the supplier details."
                                />
                            </div>
                            <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 pt-5">
                                <Button type="button" variant="ghost" onClick={() => navigate(editing ? `${ROOT}/${supplierId}` : ROOT)}>Cancel</Button>
                                <Button type="submit" disabled={saving || !confirmedSharedData}>
                                    {saving ? "Saving..." : editing ? "Save Changes" : "Create Supplier"}
                                </Button>
                            </div>
                        </form>
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}

