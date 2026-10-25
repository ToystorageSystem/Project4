import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { createProduct, getCategoryOptions, getProduct, listProducts, productErrorMessage, requestProductUpdate, uploadProductImage } from "../../../api/business/products";
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

const ROOT = "/business-staff/products";
const EMPTY_FORM = {
    productCode: "", barcode: "", imageUrl: "", name: "", categoryId: "",
    brandId: "", baseUnit: "", purchasePrice: "", sellingPrice: "", requestReason: "",
};

function validate(form) {
    const errors = {};
    const limits = { productCode: 50, barcode: 100, imageUrl: 500, name: 200, baseUnit: 30, requestReason: 500 };
    for (const [field, maximum] of Object.entries(limits)) {
        if (form[field].trim().length > maximum) errors[field] = `Tối đa ${maximum} ký tự.`;
    }
    for (const field of ["productCode", "barcode", "name", "baseUnit"]) {
        if (!form[field].trim()) errors[field] = "Không được để trống.";
    }
    if (!/^[1-9]\d*$/.test(form.categoryId.trim())) errors.categoryId = "Chọn ID danh mục hợp lệ (lớn hơn 0).";
    if (form.brandId.trim() && !/^[1-9]\d*$/.test(form.brandId.trim())) errors.brandId = "ID thương hiệu phải lớn hơn 0.";
    for (const field of ["purchasePrice", "sellingPrice"]) {
        if (!form[field].trim() || !Number.isFinite(Number(form[field])) || Number(form[field]) < 0.01) {
            errors[field] = "Giá phải là số lớn hơn hoặc bằng 0,01.";
        }
    }
    if (form.imageUrl.trim() && !/^https?:\/\//i.test(form.imageUrl.trim())) errors.imageUrl = "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://.";
    return errors;
}

function toPayload(form) {
    return {
        productCode: form.productCode.trim().toUpperCase(),
        barcode: form.barcode.trim(),
        imageUrl: form.imageUrl.trim() || null,
        name: form.name.trim(),
        categoryId: Number(form.categoryId),
        brandId: form.brandId.trim() ? Number(form.brandId) : null,
        baseUnit: form.baseUnit.trim(),
        purchasePrice: Number(form.purchasePrice),
        sellingPrice: Number(form.sellingPrice),
        requestReason: form.requestReason.trim() || null,
    };
}

function uniqueOptions(rows, idField, nameField) {
    const options = new Map();
    for (const row of rows) {
        if (row?.[idField] > 0) options.set(String(row[idField]), row[nameField] || `ID ${row[idField]}`);
    }
    return [...options].map(([value, label]) => ({ value, label })).sort((a, b) => Number(a.value) - Number(b.value));
}

export default function ProductFormPage() {
    const { productId } = useParams();
    const editing = Boolean(productId);
    const navigate = useNavigate();
    const [form, setForm] = useState(EMPTY_FORM);
    const [categorySuggestions, setCategorySuggestions] = useState([]);
    const [brandSuggestions, setBrandSuggestions] = useState([]);
    const [categoryApiWarning, setCategoryApiWarning] = useState("");
    const [loading, setLoading] = useState(true);
    const [loadFailed, setLoadFailed] = useState(false);
    const [errors, setErrors] = useState({});
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [imageFile, setImageFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [confirmed, setConfirmed] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        setLoadFailed(false);
        setConfirmed(false);
        setImageFile(null);
        const productPromise = editing ? getProduct(productId, controller.signal) : Promise.resolve(null);
        const categoryPromise = getCategoryOptions(controller.signal)
            .catch((requestError) => {
                if (!controller.signal.aborted) setCategoryApiWarning(productErrorMessage(requestError, "Không tải được danh mục."));
                return [];
            });
        // No /api/brands endpoint exists in this BE snapshot. Suggestions
        // from products are non-exhaustive; users can enter another valid ID.
        const samplePromise = listProducts({ page: 0, size: 100 }, controller.signal).catch(() => ({ content: [] }));
        Promise.all([productPromise, categoryPromise, samplePromise])
            .then(([product, categories, sample]) => {
                if (controller.signal.aborted) return;
                const sampleRows = Array.isArray(sample?.content) ? sample.content : [];
                const categoryRows = [...sampleRows, ...(product ? [product] : [])];
                const fromProducts = uniqueOptions(categoryRows, "categoryId", "categoryName");
                const fromApi = Array.isArray(categories) ? categories
                    .filter((item) => item.status === "ACTIVE")
                    .map((item) => ({ value: String(item.id), label: item.name })) : [];
                setCategorySuggestions([...new Map([...fromProducts, ...fromApi].map((item) => [item.value, item])).values()]);
                setBrandSuggestions(uniqueOptions([...sampleRows, ...(product ? [product] : [])], "brandId", "brandName"));
                if (editing) {
                    if (product.status !== "ACTIVE" && product.status !== "INACTIVE") {
                        setLoadFailed(true);
                        setError("Chỉ có thể gửi đề nghị sửa sản phẩm đang ACTIVE hoặc INACTIVE.");
                    } else {
                        setForm(Object.fromEntries(Object.keys(EMPTY_FORM).map((key) => [key, product[key] == null ? "" : String(product[key])])));
                    }
                } else {
                    setForm({ ...EMPTY_FORM });
                }
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) {
                    setLoadFailed(true);
                    setError(productErrorMessage(requestError, "Không tải được dữ liệu sản phẩm."));
                }
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [editing, productId]);

    const change = (field) => (event) => {
        setForm((old) => ({ ...old, [field]: event.target.value }));
        setErrors((old) => ({ ...old, [field]: undefined }));
        setConfirmed(false);
    };

    const upload = async () => {
        if (!imageFile || uploading) return;
        if (!imageFile.type.startsWith("image/")) { setError("File được chọn phải là hình ảnh."); return; }
        setUploading(true);
        setError("");
        try {
            const result = await uploadProductImage(imageFile);
            if (!result?.imageUrl) throw new Error("No image URL returned");
            setForm((old) => ({ ...old, imageUrl: result.imageUrl }));
            setConfirmed(false);
            setImageFile(null);
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Không thể tải ảnh lên."));
        } finally {
            setUploading(false);
        }
    };

    const submit = async (event) => {
        event.preventDefault();
        if (saving || uploading || !confirmed || loadFailed) return;
        const nextErrors = validate(form);
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length) return;
        setSaving(true);
        setError("");
        try {
            const request = editing
                ? await requestProductUpdate(productId, toPayload(form))
                : await createProduct(toPayload(form));
            const destinationId = editing ? productId : request?.product?.id;
            const notice = editing
                ? "Đã gửi yêu cầu sửa để Business Manager duyệt. Thông tin sản phẩm chưa thay đổi."
                : "Đã tạo sản phẩm ở trạng thái PENDING_CREATE và gửi yêu cầu chờ Business Manager duyệt.";
            navigate(destinationId ? `${ROOT}/${destinationId}` : `${ROOT}/requests`, {
                replace: true,
                state: { notice },
            });
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Không thể gửi yêu cầu sản phẩm."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title={editing ? "Đề nghị sửa sản phẩm" : "Thêm sản phẩm"}
                    description={editing ? "Thông tin mới chỉ có hiệu lực sau khi Business Manager duyệt." : "Tạo mới sẽ chuyển sang trạng thái PENDING_CREATE, chưa thể sử dụng ngay."}
                    actions={<Button variant="ghost" onClick={() => navigate(editing ? `${ROOT}/${productId}` : ROOT)}>← Quay lại</Button>}
                />
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm"><Spinner /> Đang tải dữ liệu...</div>
                ) : loadFailed ? (
                    <Card><Button variant="outline" onClick={() => navigate(ROOT)}>Về danh sách</Button></Card>
                ) : (
                    <Card className="max-w-6xl">
                        <Alert type="info">Mã sản phẩm và barcode phải duy nhất. Backend kiểm tra trùng trên toàn bộ dữ liệu khi bạn gửi yêu cầu.</Alert>
                        {categoryApiWarning && (
                            <div className="mt-4"><Alert type="warning">Không tải được danh mục từ API: {categoryApiWarning} Có thể nhập ID danh mục đang hoạt động ở bên dưới (Backend sẽ xác minh).</Alert></div>
                        )}
                        <form onSubmit={submit} noValidate className="mt-6 space-y-6">
                            <div className="grid gap-5 md:grid-cols-2">
                                <Input label="Mã sản phẩm *" required maxLength={50} value={form.productCode} onChange={change("productCode")} error={errors.productCode} placeholder="VD: SP-TOY-001" />
                                <Input label="Barcode *" required maxLength={100} value={form.barcode} onChange={change("barcode")} error={errors.barcode} placeholder="Mã vạch duy nhất" />
                                <div className="md:col-span-2"><Input label="Tên sản phẩm *" required maxLength={200} value={form.name} onChange={change("name")} error={errors.name} placeholder="Tên sản phẩm" /></div>
                                <div>
                                    <Input label="Chọn danh mục (ID) *" type="number" min="1" step="1" required list="product-category-options" value={form.categoryId} onChange={change("categoryId")} error={errors.categoryId} placeholder="Nhập/chọn ID danh mục" />
                                    <datalist id="product-category-options">{categorySuggestions.map((item) => <option key={item.value} value={item.value} label={item.label} />)}</datalist>
                                    <p className="mt-1 text-xs text-slate-500">{categorySuggestions.length ? `Gợi ý: ${categorySuggestions.map((x) => `${x.label} (#${x.value})`).join(" · ")}` : "Chưa có gợi ý. Cần nhập ID danh mục đang hoạt động."}</p>
                                </div>
                                <div>
                                    <Input label="Chọn thương hiệu (ID, tùy chọn)" type="number" min="1" step="1" list="product-brand-options" value={form.brandId} onChange={change("brandId")} error={errors.brandId} placeholder="Để trống hoặc nhập ID" />
                                    <datalist id="product-brand-options">{brandSuggestions.map((item) => <option key={item.value} value={item.value} label={item.label} />)}</datalist>
                                    <p className="mt-1 text-xs text-slate-500">{brandSuggestions.length ? `Gợi ý từ sản phẩm hiện có: ${brandSuggestions.map((x) => `${x.label} (#${x.value})`).join(" · ")}` : "Backend chưa có API danh sách thương hiệu; có thể nhập ID đang hoạt động."}</p>
                                </div>
                                <Input label="Đơn vị cơ sở *" maxLength={30} required value={form.baseUnit} onChange={change("baseUnit")} error={errors.baseUnit} placeholder="VD: PCS, BAG, BOX" />
                                <Input label="Giá nhập (VND) *" type="number" min="0.01" step="0.01" required value={form.purchasePrice} onChange={change("purchasePrice")} error={errors.purchasePrice} placeholder="120000" />
                                <Input label="Giá bán (VND) *" type="number" min="0.01" step="0.01" required value={form.sellingPrice} onChange={change("sellingPrice")} error={errors.sellingPrice} placeholder="150000" />
                                <div className="md:col-span-2"><Input label="URL hình ảnh (nếu có)" maxLength={500} type="url" value={form.imageUrl} onChange={change("imageUrl")} error={errors.imageUrl} placeholder="https://..." /></div>
                            </div>
                            <div className="rounded-xl border border-slate-200 p-4">
                                <p className="text-sm font-semibold text-slate-800">Hoặc tải hình ảnh lên Cloudinary</p>
                                <p className="mb-3 mt-1 text-xs text-slate-500">Chỉ khi bạn bấm “Tải ảnh lên”, ảnh mới được gửi đến dịch vụ lưu trữ. Việc tải ảnh không tạo sản phẩm.</p>
                                <div className="flex flex-wrap items-center gap-3">
                                    <input type="file" accept="image/*" aria-label="Chọn ảnh sản phẩm" onChange={(event) => setImageFile(event.target.files?.[0] || null)} className="max-w-full text-sm text-slate-600" />
                                    <Button type="button" variant="outline" onClick={upload} disabled={!imageFile || uploading || saving}>{uploading ? "Đang tải ảnh..." : "Tải ảnh lên"}</Button>
                                </div>
                                {form.imageUrl && <img src={form.imageUrl} alt="Ảnh xem trước" className="mt-3 h-28 max-w-full rounded-lg border object-contain" />}
                            </div>
                            <TextArea label="Lý do đề nghị (nếu có)" rows={3} maxLength={500} value={form.requestReason} onChange={change("requestReason")} error={errors.requestReason} placeholder="Lý do tạo hoặc sửa sản phẩm" />
                            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} label="Tôi xác nhận gửi yêu cầu thật vào MySQL chung. Sản phẩm/việc chỉnh sửa cần Business Manager phê duyệt." />
                            </div>
                            <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 pt-5">
                                <Button type="button" variant="ghost" onClick={() => navigate(editing ? `${ROOT}/${productId}` : ROOT)}>Hủy</Button>
                                <Button type="submit" disabled={!confirmed || uploading || saving}>{saving ? "Đang gửi..." : editing ? "Gửi đề nghị sửa" : "Tạo và gửi duyệt"}</Button>
                            </div>
                        </form>
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}
