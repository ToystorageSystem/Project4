import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getProduct, productErrorMessage } from "../../../api/business/products";
import ProductStatusBadge, { formatProductDate, formatProductPrice } from "../../../components/business/products/ProductStatusBadge";
import SupplierProductLinksPanel from "../../../components/business/supplierProducts/SupplierProductLinksPanel";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Spinner from "../../../components/ui/Spinner";

const ROOT = "/business-staff/products";
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
                if (!controller.signal.aborted) setError(productErrorMessage(requestError, "Không tìm thấy sản phẩm."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [productId]);

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="Chi tiết sản phẩm" description="Sản phẩm chỉ được đưa vào sử dụng sau khi Business Manager phê duyệt."
                    actions={
                        <div className="flex flex-wrap gap-2">
                            <Button variant="ghost" onClick={() => navigate(ROOT)}>← Danh sách</Button>
                            {(product?.status === "ACTIVE" || product?.status === "INACTIVE") && (
                                <Button onClick={() => navigate(`${ROOT}/${productId}/edit`)}>Đề nghị sửa</Button>
                            )}
                        </div>
                    }
                />
                {location.state?.notice && <div role="status"><Alert type="success">{location.state.notice}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading && <div role="status" className="flex items-center gap-2 rounded-2xl bg-white p-5 text-sm"><Spinner /> Đang tải sản phẩm...</div>}
                {!loading && product && !error && (
                    <>
                        {(product.status === "PENDING_CREATE" || product.status === "PENDING_UPDATE" || product.status === "PENDING") && (
                            <Alert type="warning">Sản phẩm hoặc yêu cầu thay đổi đang chờ Business Manager duyệt, chưa được dùng như sản phẩm đã duyệt.</Alert>
                        )}
                        {product.status === "REJECTED" && (
                            <Alert type="danger">Sản phẩm bị từ chối. Lý do: {product.rejectionReason || "Chưa có thông tin lý do."} Bạn có thể theo dõi yêu cầu tại “Yêu cầu của tôi”.</Alert>
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
                                <DetailField label="Mã sản phẩm">{product.productCode}</DetailField>
                                <DetailField label="Barcode">{product.barcode}</DetailField>
                                <DetailField label="Đơn vị cơ sở">{product.baseUnit}</DetailField>
                                <DetailField label="Danh mục">{product.categoryName ? `${product.categoryName} (ID ${product.categoryId})` : `ID ${product.categoryId}`}</DetailField>
                                <DetailField label="Thương hiệu">{product.brandId ? `${product.brandName || "Thương hiệu"} (ID ${product.brandId})` : "Không có"}</DetailField>
                                <DetailField label="Giá nhập">{formatProductPrice(product.purchasePrice)}</DetailField>
                                <DetailField label="Giá bán">{formatProductPrice(product.sellingPrice)}</DetailField>
                                <DetailField label="Người tạo">{product.createdByName || "—"}</DetailField>
                                <DetailField label="Người phê duyệt">{product.approvedByName || "Chưa duyệt"}</DetailField>
                                <DetailField label="Ngày tạo">{formatProductDate(product.createdAt)}</DetailField>
                                <DetailField label="Ngày cập nhật">{formatProductDate(product.updatedAt)}</DetailField>
                            </div>
                            {product.imageUrl && (
                                <div className="mt-5 border-t border-slate-100 pt-5">
                                    <p className="mb-3 text-sm font-medium text-slate-700">Ảnh sản phẩm</p>
                                    <img src={product.imageUrl} alt={product.name} className="h-48 max-w-full rounded-xl border border-slate-200 object-contain" />
                                </div>
                            )}
                        </Card>
                        <SupplierProductLinksPanel productId={productId} productStatus={product.status} />
                        <Card>
                            <p className="text-sm text-slate-600">Đề nghị sửa sản phẩm được lưu riêng và chỉ áp dụng lên sản phẩm khi được Business Manager phê duyệt. Để kiểm tra trạng thái yêu cầu, mở <button type="button" onClick={() => navigate(`${ROOT}/requests`)} className="font-semibold text-[#c94810] hover:underline">Yêu cầu của tôi</button>.</p>
                        </Card>
                    </>
                )}
            </div>
        </MainLayout>
    );
}
