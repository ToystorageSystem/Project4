import SupplierProductLinksPanel from "../../../components/business/supplierProducts/SupplierProductLinksPanel";
import { listSuppliers, supplierErrorMessage } from "../../../api/business/suppliers";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { createProductWithSupplier, getCategoryOptions, getProduct, productErrorMessage, updateProduct, uploadProductImage } from "../../../api/business/products";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Input from "../../../components/ui/Input";
import ComboboxSelect from "../../../components/ui/ComboboxSelect";
import BrandFormModal from "../../../components/business/brands/BrandFormModal";
import { getActiveBrands } from "../../../api/business/brands";
import Spinner from "../../../components/ui/Spinner";
import TextArea from "../../../components/ui/TextArea";

const ROOT = "/business/products";
const EMPTY_FORM = {
    productCode: "", barcode: "", imageUrl: "", name: "", categoryId: "",
    brandId: "", baseUnit: "", purchasePrice: "", sellingPrice: "", requestReason: "",
};

function validate(form, activeCategoryIds, activeBrandIds) {
    const errors = {};
    const limits = { productCode: 50, barcode: 100, imageUrl: 500, name: 200, baseUnit: 30, requestReason: 500 };
    for (const [field, maximum] of Object.entries(limits)) {
        if (form[field].trim().length > maximum) errors[field] = `Maximum ${maximum} characters.`;
    }
    for (const field of ["barcode", "name", "baseUnit"]) {
        if (!form[field].trim()) errors[field] = "Required field.";
    }
    // Chi chap nhan category ACTIVE da duoc them trong Category Management.
    if (!activeCategoryIds.has(form.categoryId.trim())) errors.categoryId = "Please select an active category from the list.";
    // Brand khong bat buoc, nhung neu chon thi phai la brand ACTIVE.
    if (form.brandId.trim() && !activeBrandIds.has(form.brandId.trim())) errors.brandId = "Please select an active brand from the list.";
    for (const field of ["sellingPrice"]) {
        if (!form[field].trim() || !Number.isFinite(Number(form[field])) || Number(form[field]) < 0.01) {
            errors[field] = "Price must be at least 0.01.";
        }
    }
    if (form.imageUrl.trim() && !/^https?:\/\//i.test(form.imageUrl.trim())) errors.imageUrl = "Image URL must start with http:// or https://.";
    return errors;
}

function toPayload(form) {
    return {
        productCode: form.productCode.trim().toUpperCase() || null,
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

function validateSupplierSetup(value) {
    const errors = {};
    if (!value.name.trim() || value.name.trim().length > 200) {
        errors.name = "Enter a supplier name (maximum 200 characters).";
    }
    if (value.phone.trim() && !/^\+?[0-9]{9,15}$/.test(value.phone.trim())) {
        errors.phone = "Phone number must contain 9 to 15 digits (optional + prefix).";
    }
    if (!/^\d+(\.\d{1,2})?$/.test(value.purchasePrice)
            || Number(value.purchasePrice) < 0.01) {
        errors.purchasePrice = "Enter a valid purchase price greater than zero.";
    }
    if (!/^[1-9]\d*$/.test(value.minimumOrderQuantity)) {
        errors.minimumOrderQuantity = "Minimum order quantity must be at least 1.";
    }
    return errors;
}

function SupplierNameCombobox({ value, onChange, suppliers, loadError, disabled, errors }) {
    const [open, setOpen] = useState(false);
    const suggestions = useMemo(() => {
        const term = value.name.trim().toLocaleLowerCase();
        return suppliers.filter((item) => !term
            || item.name.toLocaleLowerCase().includes(term)
            || (item.supplierCode || "").toLocaleLowerCase().includes(term)).slice(0, 50);
    }, [suppliers, value.name]);
    const selected = suppliers.find((item) => String(item.id) === String(value.supplierId));

    return <div className="md:col-span-2 space-y-2">
        <label className="block space-y-1.5">
            <span className="text-sm font-medium text-slate-700">Supplier Name *</span>
            <input type="text" role="combobox" aria-expanded={open} aria-controls="supplier-name-options"
                aria-autocomplete="list" autoComplete="off" maxLength={200}
                className={`w-full rounded-xl border bg-white px-3 py-2.5 text-sm focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-100 ${errors.name ? "border-red-500" : "border-slate-300"}`}
                value={value.name} disabled={disabled} placeholder="Select a supplier or type a new supplier name"
                onFocus={() => setOpen(true)} onChange={(event) => {
                    const typed = event.target.value;
                    const exact = suppliers.filter((s) => s.name.toLocaleLowerCase() === typed.trim().toLocaleLowerCase());
                    const match = exact.length === 1 ? exact[0] : null;
                    onChange((current) => ({ ...current, name: typed,
                        supplierId: match ? String(match.id) : "", phone: match?.phone || "" }));
                    setOpen(true);
                }}
                onKeyDown={(event) => { if (event.key === "Escape") setOpen(false); }}
                onBlur={() => setTimeout(() => setOpen(false), 120)} />
            {errors.name && <span className="text-xs text-red-600">{errors.name}</span>}
        </label>
        {open && !disabled && <div id="supplier-name-options" role="listbox"
            className="max-h-52 space-y-1 overflow-y-auto rounded-xl border border-slate-200 bg-white p-2 shadow-sm">
            {suggestions.map((supplier) => <button type="button" role="option" key={supplier.id}
                aria-selected={String(value.supplierId) === String(supplier.id)}
                className="block w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-orange-50"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => { onChange((current) => ({ ...current,
                    name: supplier.name, supplierId: String(supplier.id), phone: supplier.phone || "" })); setOpen(false); }}>
                {supplier.name} <span className="text-xs text-slate-500">({supplier.supplierCode || `ID ${supplier.id}`})</span>
            </button>)}
            {!suggestions.length && <p className="px-3 py-2 text-sm text-slate-500">No matching supplier. Your typed name will create a new supplier.</p>}
        </div>}
        {loadError && <p role="alert" className="text-xs text-red-600">{loadError}</p>}
        <p className="text-xs text-slate-500">{selected
            ? "Existing supplier selected. Its phone number is shown below."
            : "If the name does not exist, a new supplier will be created when you save the product."}</p>
    </div>;
}

// Chuyen danh sach Category/Brand ACTIVE thanh option cho dropdown
function toActiveOptions(rows, codeField) {
    if (!Array.isArray(rows)) return [];
    return rows
        .filter((item) => item?.id > 0 && item.status === "ACTIVE")
        .map((item) => ({ value: String(item.id), label: item[codeField] ? `${item.name} (${item[codeField]})` : item.name }))
        .sort((a, b) => a.label.localeCompare(b.label));
}

export default function ProductFormPage() {
    const { productId } = useParams();
    const editing = Boolean(productId);
    const navigate = useNavigate();
    const [form, setForm] = useState(EMPTY_FORM);
    const [categorySuggestions, setCategorySuggestions] = useState([]);
    const [brandSuggestions, setBrandSuggestions] = useState([]);
    const [categoryApiWarning, setCategoryApiWarning] = useState("");
    const [brandApiWarning, setBrandApiWarning] = useState("");
    const [brandModalOpen, setBrandModalOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const [loadFailed, setLoadFailed] = useState(false);
    const [errors, setErrors] = useState({});
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [existingProductStatus, setExistingProductStatus] = useState("ACTIVE");
    const [supplierSetup, setSupplierSetup] = useState({
        supplierId: "", name: "", phone: "", purchasePrice: "", minimumOrderQuantity: "1",
    });
    const [supplierErrors, setSupplierErrors] = useState({});
    const [availableSuppliers, setAvailableSuppliers] = useState([]);
    const [supplierLoadError, setSupplierLoadError] = useState("");
    const [imageFile, setImageFile] = useState(null);
    const [uploading, setUploading] = useState(false);

    useEffect(() => {
        if (editing) return undefined;
        const controller = new AbortController();
        const load = async () => {
            try {
                const all = [];
                let page = 0;
                let pages = 1;
                while (page < pages && !controller.signal.aborted) {
                    const result = await listSuppliers({ status: "ACTIVE", page, size: 100,
                        sortBy: "name", direction: "ASC" }, controller.signal);
                    if (!Array.isArray(result.content)) throw new Error("Invalid supplier list");
                    all.push(...result.content);
                    pages = result.totalPages || 1;
                    page++;
                }
                if (!controller.signal.aborted) setAvailableSuppliers(all);
            } catch (error) {
                if (!controller.signal.aborted) setSupplierLoadError(supplierErrorMessage(error, "Unable to load suppliers."));
            }
        };
        load();
        return () => controller.abort();
    }, [editing]);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        setLoadFailed(false);
        setSupplierErrors({});
        setImageFile(null);
        const productPromise = editing ? getProduct(productId, controller.signal) : Promise.resolve(null);
        const categoryPromise = getCategoryOptions(controller.signal)
            .catch((requestError) => {
                if (!controller.signal.aborted) setCategoryApiWarning(productErrorMessage(requestError, "Unable to load categories."));
                return [];
            });
        const brandPromise = getActiveBrands(controller.signal)
            .catch((requestError) => {
                if (!controller.signal.aborted) setBrandApiWarning(productErrorMessage(requestError, "Unable to load brands."));
                return [];
            });
        Promise.all([productPromise, categoryPromise, brandPromise])
            .then(([product, categories, brands]) => {
                if (controller.signal.aborted) return;
                // Chi lay Category/Brand ACTIVE tu API /options
                // (khong lay tu product mau vi co the dang dung muc da bi an).
                setCategorySuggestions(toActiveOptions(categories, "categoryCode"));
                setBrandSuggestions(toActiveOptions(brands, "brandCode"));
                if (editing) {
                    setExistingProductStatus(product.status);
                    if (product.status !== "ACTIVE" && product.status !== "INACTIVE") {
                        setLoadFailed(true);
                        setError("Only active or hidden products can be edited.");
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
                    setError(productErrorMessage(requestError, "Unable to load product data."));
                }
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [editing, productId]);

    // Dung cho ComboboxSelect (tra ve value truc tiep, khong phai event)
    const pick = (field) => (value) => change(field)({ target: { value } });

    const change = (field) => (event) => {
        setForm((old) => ({ ...old, [field]: event.target.value }));
        setErrors((old) => ({ ...old, [field]: undefined }));
    };

    const upload = async () => {
        if (!imageFile || uploading) return;
        if (!imageFile.type.startsWith("image/")) { setError("The selected file must be an image."); return; }
        setUploading(true);
        setError("");
        try {
            const result = await uploadProductImage(imageFile);
            if (!result?.imageUrl) throw new Error("No image URL was returned");
            setForm((old) => ({ ...old, imageUrl: result.imageUrl }));
                setImageFile(null);
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Unable to upload image."));
        } finally {
            setUploading(false);
        }
    };

    const submit = async (event) => {
        event.preventDefault();
        if (saving || uploading || loadFailed) return;
        const nextErrors = validate(form,
            new Set(categorySuggestions.map((item) => item.value)),
            new Set(brandSuggestions.map((item) => item.value)));
        const nextSupplierErrors = editing ? {} : validateSupplierSetup(supplierSetup);
        setErrors(nextErrors);
        setSupplierErrors(nextSupplierErrors);
        if (Object.keys(nextErrors).length || Object.keys(nextSupplierErrors).length) return;
        setSaving(true);
        setError("");
        try {
            // BE tao Product va lien ket Supplier trong cung mot transaction.
            const product = editing
                ? await updateProduct(productId, toPayload(form))
                : await createProductWithSupplier({
                    product: { ...toPayload({ ...form, purchasePrice: supplierSetup.purchasePrice }),
                        productCode: null },
                    supplierId: supplierSetup.supplierId ? Number(supplierSetup.supplierId) : null,
                    supplierName: supplierSetup.name.trim(),
                    supplierPhone: supplierSetup.supplierId ? null : supplierSetup.phone.trim() || null,
                    minimumOrderQuantity: Number(supplierSetup.minimumOrderQuantity),
                    leadTimeDays: 0,
                });
            if (!product?.id && !editing) {
                setError("The product may have been saved, but no ID was returned. Check the product list before retrying.");
                return;
            }
            navigate(`${ROOT}/${product?.id ?? productId}`, {
                replace: true,
                state: { notice: editing ? "Product updated successfully." : "Product and supplier saved successfully." },
            });
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Unable to save product and supplier."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title={editing ? "Edit Product" : "Add Product"}
                    description={editing
                        ? "Edit product details below. Edit the purchase price in Product Supplier."
                        : "Product Code is generated automatically. Choose a supplier or enter a new supplier name."}
                    actions={<Button variant="ghost" onClick={() => navigate(ROOT)}>← Back</Button>}
                />
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm"><Spinner /> Loading data...</div>
                ) : loadFailed ? (
                    <Card><Button variant="outline" onClick={() => navigate(ROOT)}>Back to List</Button></Card>
                ) : (
                    <Card className="max-w-6xl">
                        {categoryApiWarning && (
                            <div className="mt-4"><Alert type="warning">Unable to load product categories: {categoryApiWarning} Please reload the page before saving.</Alert></div>
                        )}
                        <form onSubmit={submit} noValidate className="mt-6 space-y-6">
                            <div className="grid gap-5 md:grid-cols-2">
                                <div className="md:col-span-2">
                                    <Input label="Barcode *" required maxLength={100} value={form.barcode} onChange={change("barcode")} error={errors.barcode} placeholder="May be shared across different suppliers" />
                                </div>
                                <div className="md:col-span-2"><Input label="Product Name *" required maxLength={200} value={form.name} onChange={change("name")} error={errors.name} placeholder="Product Name" /></div>
                                <div>
                                    <ComboboxSelect label="Product Category *" value={form.categoryId} onChange={pick("categoryId")} error={errors.categoryId}
                                        disabled={!categorySuggestions.length} options={categorySuggestions}
                                        placeholder={categorySuggestions.length ? "Type to search category..." : "No active category available"}
                                        noMatchText="No matching category." />
                                    {editing && form.categoryId && !categorySuggestions.some((item) => item.value === form.categoryId) && (
                                        <p className="mt-1 text-xs text-amber-600">The current category is inactive or no longer exists. Please choose an active category.</p>
                                    )}
                                </div>
                                <div>
                                    <div className="flex items-end gap-2">
                                        <div className="min-w-0 flex-1">
                                            <ComboboxSelect label="Brand (optional)" value={form.brandId} onChange={pick("brandId")} error={errors.brandId}
                                                options={brandSuggestions} emptyLabel="No brand"
                                                placeholder="Type to search brand or leave blank"
                                                noMatchText="No matching brand. Use + New to add one." />
                                        </div>
                                        {/* Mo popup them brand moi */}
                                        <Button type="button" variant="outline" disabled={saving || uploading}
                                            className={errors.brandId ? "mb-5" : ""}
                                            onClick={() => setBrandModalOpen(true)}>+ New</Button>
                                    </div>
                                    {brandApiWarning && <p role="alert" className="mt-1 text-xs text-red-600">Unable to load brands: {brandApiWarning}</p>}
                                    {editing && form.brandId && !brandSuggestions.some((item) => item.value === form.brandId) && (
                                        <p className="mt-1 text-xs text-amber-600">The current brand is inactive or no longer exists. Choose an active brand or "No brand".</p>
                                    )}
                                </div>
                                <Input label="Base Unit *" maxLength={30} required value={form.baseUnit} onChange={change("baseUnit")} error={errors.baseUnit} placeholder="Example: piece, bag, box" />
                                <Input label="Selling Price (VND) *" type="number" min="0.01" step="0.01" required value={form.sellingPrice} onChange={change("sellingPrice")} error={errors.sellingPrice} placeholder="150000" />
                                {!editing && <>
                                    <SupplierNameCombobox value={supplierSetup} suppliers={availableSuppliers}
                                        loadError={supplierLoadError} disabled={saving || uploading} errors={supplierErrors}
                                        onChange={(updater) => { setSupplierSetup(updater); setSupplierErrors({}); }} />
                                    <div>
                                        <Input label="Supplier Phone Number (optional)" type="tel" maxLength={16}
                                            placeholder="0901234567" value={supplierSetup.phone}
                                            disabled={saving || uploading || Boolean(supplierSetup.supplierId)}
                                            onChange={(event) => setSupplierSetup((s) => ({ ...s, phone: event.target.value }))}
                                            error={supplierErrors.phone} />
                                        {supplierSetup.supplierId && <p className="mt-1 text-xs text-slate-500">Existing supplier phone is read-only.</p>}
                                    </div>
                                    <Input label="Purchase Price (VND) *" type="number" min="0.01" step="0.01"
                                        value={supplierSetup.purchasePrice} disabled={saving || uploading}
                                        onChange={(event) => setSupplierSetup((s) => ({ ...s, purchasePrice: event.target.value }))}
                                        error={supplierErrors.purchasePrice} placeholder="45000" />
                                    <Input label="Minimum Order Quantity *" type="number" min="1" step="1"
                                        value={supplierSetup.minimumOrderQuantity} disabled={saving || uploading}
                                        onChange={(event) => setSupplierSetup((s) => ({ ...s, minimumOrderQuantity: event.target.value }))}
                                        error={supplierErrors.minimumOrderQuantity} />
                                </>}

                            </div>
                            <div className="rounded-xl border border-slate-200 p-4">
                                <p className="text-sm font-semibold text-slate-800">Or upload a product image</p>
                                <p className="mb-3 mt-1 text-xs text-slate-500">Your image is uploaded to storage only when you select Upload Image. Uploading an image does not create a product.</p>
                                <div className="flex flex-wrap items-center gap-3">

                                    {/* Chon anh san pham */}
                                    <label className="inline-flex cursor-pointer items-center rounded-lg border border-orange-500 px-4 py-2 text-sm font-medium text-orange-600 hover:bg-orange-50 focus-within:ring-2 focus-within:ring-orange-300">
                                        <input
                                            type="file"
                                            accept="image/*"
                                            aria-label="Choose Product Image"
                                            className="sr-only"
                                            onChange={(event) =>
                                                setImageFile(event.target.files?.[0] || null)
                                            }
                                        />
                                        Choose Image
                                    </label>

                                    {/* Ten file da chon */}
                                    <span className="text-sm text-slate-600 break-all">
                                        {imageFile ? imageFile.name : "No file selected"}
                                    </span>

                                    {/* Tai anh len he thong */}
                                    <Button
                                        type="button"
                                        variant="outline"
                                        onClick={upload}
                                        disabled={!imageFile || uploading || saving}
                                    >
                                        {uploading ? "Uploading image..." : "Upload Image"}
                                    </Button>
                                </div>
                                {form.imageUrl && <img src={form.imageUrl} alt="Image Preview" className="mt-3 h-28 max-w-full rounded-lg border object-contain" />}
                            </div>
                            <TextArea label="Change Notes (optional)" rows={3} maxLength={500} value={form.requestReason} onChange={change("requestReason")} error={errors.requestReason} placeholder="Describe the reason for adding or updating the product" />
                            <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 pt-5">
                                <Button type="button" variant="ghost" onClick={() => navigate(ROOT)}>Cancel</Button>
                                <Button type="submit" disabled={uploading || saving}>{saving ? "Saving..." : editing ? "Save Changes" : "Create Product"}</Button>
                            </div>
                        </form>
                    </Card>
                )}
                {/* Popup them brand moi: luu xong thi tu dong chon brand do */}
                <BrandFormModal
                    open={brandModalOpen}
                    onClose={() => setBrandModalOpen(false)}
                    onSaved={(brand) => {
                        setBrandModalOpen(false);
                        if (!brand?.id) return;
                        const option = toActiveOptions([{ ...brand, status: brand.status || "ACTIVE" }], "brandCode")[0];
                        setBrandSuggestions((current) => [...current.filter((item) => item.value !== option.value), option]
                            .sort((a, b) => a.label.localeCompare(b.label)));
                        setForm((old) => ({ ...old, brandId: option.value }));
                        setErrors((old) => ({ ...old, brandId: undefined }));
                        setBrandApiWarning("");
                    }}
                />
                {editing && !loading && !loadFailed && (
                    <SupplierProductLinksPanel productId={productId} productStatus={existingProductStatus} />
                )}
            </div>
        </MainLayout>
    );
}

