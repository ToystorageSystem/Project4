import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getProduct, productErrorMessage } from "../../../api/business/products";
import ProductStatusBadge, { formatProductDate, formatProductPrice } from "../../../components/business/products/ProductStatusBadge";
import SupplierProductLinksPanel from "../../../components/business/supplierProducts/SupplierProductLinksPanel";
import ProductActivityHistory from "../../../components/business/products/ProductActivityHistory";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Spinner from "../../../components/ui/Spinner";

const ROOT = "/business/products";
const DetailField = ({ label, children }) => (
    <div className="space-y-1 rounded-xl border border-slate-100 bg-slate-50 p-4">
        <div className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</div>
        <div className="break-words text-sm font-medium text-slate-900">{children ?? "—"}</div>
    </div>
);

export default function ProductDetailPage() {
    const { productId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getProduct(productId, controller.signal)
            .then((result) => { if (!controller.signal.aborted) setProduct(result); })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(productErrorMessage(requestError, "Product not found."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [productId]);

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="Product Details" description="View product details, edit directly, and review its change history."
                    actions={
                        <div className="flex flex-wrap gap-2">
                            <Button variant="ghost" onClick={() => navigate(ROOT)}>← Back to List</Button>
                            {(product?.status === "ACTIVE" || product?.status === "INACTIVE") && (
                                <Button onClick={() => navigate(`${ROOT}/${productId}/edit`)}>Edit Product</Button>
                            )}
                        </div>
                    }
                />
                {location.state?.notice && <div role="status"><Alert type="success">{location.state.notice}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading && <div role="status" className="flex items-center gap-2 rounded-2xl bg-white p-5 text-sm"><Spinner /> Loading product...</div>}
                {!loading && product && !error && (
                    <>
                        {(product.status === "PENDING_CREATE" || product.status === "PENDING_UPDATE" || product.status === "PENDING") && (
                            <Alert type="warning">This product belongs to the legacy approval workflow. To avoid modifying existing records incorrectly, resolve the legacy request before editing.</Alert>
                        )}
                        {product.status === "REJECTED" && (
                            <Alert type="danger">This product was rejected. Reason: {product.rejectionReason || "No reason provided."} This record belongs to the legacy workflow and is retained for reference.</Alert>
                        )}
                        <Card>
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                                <div>
                                    <h2 className="text-xl font-bold text-slate-900">{product.name}</h2>
                                    <p className="mt-1 text-sm text-slate-500">{product.productCode} · {product.barcode}</p>
                                </div>
                                <ProductStatusBadge status={product.status} />
                            </div>
                            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                                <DetailField label="Product Code">{product.productCode}</DetailField>
                                <DetailField label="Barcode">{product.barcode}</DetailField>
                                <DetailField label="Base Unit">{product.baseUnit}</DetailField>
                                <DetailField label="Categories">{product.categoryName ? `${product.categoryName} (ID ${product.categoryId})` : `Code ${product.categoryId}`}</DetailField>
                                <DetailField label="Brand">{product.brandId ? `${product.brandName || "Brand"} (ID ${product.brandId})` : "None"}</DetailField>
                                <DetailField label="Purchase Price">{formatProductPrice(product.purchasePrice)}</DetailField>
                                <DetailField label="Selling Price">{formatProductPrice(product.sellingPrice)}</DetailField>
                                <DetailField label="Created By">{product.createdByName || "—"}</DetailField>
                                <DetailField label="Created At">{formatProductDate(product.createdAt)}</DetailField>
                                <DetailField label="Updated At">{formatProductDate(product.updatedAt)}</DetailField>
                            </div>
                            {product.imageUrl && (
                                <div className="mt-5 border-t border-slate-100 pt-5">
                                    <p className="mb-3 text-sm font-medium text-slate-700">Product Image</p>
                                    <img src={product.imageUrl} alt={product.name} className="h-48 max-w-full rounded-xl border border-slate-200 object-contain" />
                                </div>
                            )}
                        </Card>
                        <SupplierProductLinksPanel productId={productId} productStatus={product.status} />
                        <ProductActivityHistory productId={productId} />
                    </>
                )}
            </div>
        </MainLayout>
    );
}
