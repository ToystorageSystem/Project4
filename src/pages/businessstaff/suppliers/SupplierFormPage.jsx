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

const ROOT = "/business-staff/suppliers";
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
            errors[field] = `Không được vượt quá ${limit} ký tự.`;
        }
    }
    if (!form.name.trim()) errors.name = "Vui lòng nhập tên nhà cung cấp.";

    const phone = form.phone.trim();
    if (phone && !/^\+?[0-9]{9,15}$/.test(phone)) {
        errors.phone = "Số điện thoại gồm 9–15 chữ số, có thể bắt đầu bằng dấu +.";
    }
    const email = form.email.trim();
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errors.email = "Email không đúng định dạng.";
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
                    setError(supplierErrorMessage(requestError, "Không tải được thông tin nhà cung cấp."));
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
                state: { notice: editing ? "Cập nhật nhà cung cấp thành công." : "Thêm nhà cung cấp thành công." },
            });
        } catch (requestError) {
            setError(supplierErrorMessage(requestError, "Không thể lưu thông tin nhà cung cấp."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title={editing ? "Chỉnh sửa nhà cung cấp" : "Thêm nhà cung cấp"}
                    description="Thông tin quản lý nhà cung cấp theo Issue #7. Dấu * là trường bắt buộc."
                    actions={<Button type="button" variant="ghost" onClick={() => navigate(editing ? `${ROOT}/${supplierId}` : ROOT)}>← Quay lại</Button>}
                />
                {loading ? (
                    <div role="status" className="flex items-center gap-3 rounded-2xl bg-white p-8 text-sm text-slate-600"><Spinner /> Đang tải thông tin...</div>
                ) : loadFailed ? (
                    <Card className="max-w-5xl">
                        <div role="alert"><Alert type="danger">{error}</Alert></div>
                        <Button type="button" variant="ghost" className="mt-4" onClick={() => navigate(ROOT)}>← Về danh sách</Button>
                    </Card>
                ) : (
                    <Card className="max-w-5xl">
                        {error && <div role="alert" className="mb-5"><Alert type="danger">{error}</Alert></div>}
                        {editing && supplierCode && (
                            <div className="mb-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
                                Mã nhà cung cấp (không thể sửa): <span className="font-semibold text-slate-900">{supplierCode}</span>
                            </div>
                        )}
                        {!editing && (
                            <div className="mb-5 rounded-xl border border-orange-200 bg-orange-50 p-4 text-sm text-orange-800">
                                Mã NCC sẽ được Backend tự sinh sau khi lưu, không cần nhập thủ công.
                            </div>
                        )}
                        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
                            <div className="grid gap-5 md:grid-cols-2">
                                <Input label="Tên nhà cung cấp *" required maxLength={200} value={form.name} onChange={change("name")} error={errors.name} placeholder="Nhập tên nhà cung cấp" />
                                <Input label="Mã số thuế" maxLength={50} value={form.taxCode} onChange={change("taxCode")} error={errors.taxCode} placeholder="Mã số thuế (nếu có)" />
                                <Input label="Người liên hệ" maxLength={150} value={form.contactPerson} onChange={change("contactPerson")} error={errors.contactPerson} placeholder="Họ và tên" />
                                <Input label="Chức vụ người liên hệ" maxLength={100} value={form.contactPosition} onChange={change("contactPosition")} error={errors.contactPosition} placeholder="Chức vụ" />
                                <Input label="Số điện thoại" type="tel" maxLength={16} value={form.phone} onChange={change("phone")} error={errors.phone} placeholder="09xxxxxxxx" />
                                <Input label="Email" type="email" maxLength={150} value={form.email} onChange={change("email")} error={errors.email} placeholder="supplier@example.com" />
                                <div className="md:col-span-2">
                                    <Input label="Địa chỉ" maxLength={255} value={form.address} onChange={change("address")} error={errors.address} placeholder="Địa chỉ nhà cung cấp" />
                                </div>
                                <div className="md:col-span-2">
                                    <TextArea label="Ghi chú" rows={4} value={form.note} onChange={change("note")} placeholder="Thông tin bổ sung (nếu có)" />
                                </div>
                            </div>

                            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                <Checkbox
                                    checked={confirmedSharedData}
                                    onChange={(event) => setConfirmedSharedData(event.target.checked)}
                                    label="Tôi xác nhận thao tác này sẽ ghi dữ liệu thật vào MySQL dùng chung của nhóm."
                                />
                            </div>
                            <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 pt-5">
                                <Button type="button" variant="ghost" onClick={() => navigate(editing ? `${ROOT}/${supplierId}` : ROOT)}>Hủy</Button>
                                <Button type="submit" disabled={saving || !confirmedSharedData}>
                                    {saving ? "Đang lưu..." : editing ? "Lưu thay đổi" : "Tạo nhà cung cấp"}
                                </Button>
                            </div>
                        </form>
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}
