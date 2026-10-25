import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { changeSupplierStatus, getSupplier, getSupplierProducts, supplierErrorMessage } from "../../../api/business/suppliers";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Modal from "../../../components/ui/Modal";
import Spinner from "../../../components/ui/Spinner";
import StatusBadge from "../../../components/ui/StatusBadge";
import Table from "../../../components/ui/Table";

const ROOT = "/business-staff/suppliers";
const show = (value) => value == null || value === "" ? "—" : value;
const formatDate = (value) => value ? new Date(value).toLocaleString("vi-VN") : "—";
const formatMoney = (value) => value == null ? "—" : `${Number(value).toLocaleString("vi-VN")} ₫`;

function InfoItem({ label, value }) {
    return (
        <div className="min-w-0">
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
            <dd className="mt-1 break-words text-sm font-semibold text-slate-900">{show(value)}</dd>
        </div>
    );
}

export default function SupplierDetailPage() {
    const { supplierId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [supplier, setSupplier] = useState(null);
    const [products, setProducts] = useState([]);
    const [productsLoading, setProductsLoading] = useState(true);
    const [loading, setLoading] = useState(true);
    const [productsError, setProductsError] = useState("");
    const [error, setError] = useState("");
    const [notice, setNotice] = useState(location.state?.notice || "");
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [savingStatus, setSavingStatus] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setError("");
        setSupplier(null);
        setProducts([]);
        setProductsError("");
        setProductsLoading(true);
        setLoading(true);
        getSupplier(supplierId, controller.signal)
            .then((data) => { if (!controller.signal.aborted) setSupplier(data); })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(supplierErrorMessage(requestError, "Không tải được thông tin nhà cung cấp."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        getSupplierProducts(supplierId, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data)) throw new Error("Invalid supplier product response");
                setProducts(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setProductsError(supplierErrorMessage(requestError, "Không tải được sản phẩm liên kết."));
            })
            .finally(() => { if (!controller.signal.aborted) setProductsLoading(false); });
        return () => controller.abort();
    }, [supplierId]);

    const handleStatusChange = async () => {
        if (!supplier || savingStatus) return;
        setSavingStatus(true);
        setError("");
        setNotice("");
        try {
            const updated = await changeSupplierStatus(supplierId, supplier.status === "INACTIVE");
            setSupplier(updated);
            setNotice(updated.status === "ACTIVE" ? "Khôi phục nhà cung cấp thành công." : "Đã ẩn nhà cung cấp.");
            setConfirmOpen(false);
        } catch (requestError) {
            setError(supplierErrorMessage(requestError, "Không thay đổi được trạng thái nhà cung cấp."));
            setConfirmOpen(false);
        } finally {
            setSavingStatus(false);
        }
    };

    const columns = [
        { key: "productsCode", label: "Mã sản phẩm", render: (value) => show(value) },
        { key: "productName", label: "Tên sản phẩm", render: (value) => <span className="font-medium text-slate-900">{show(value)}</span> },
        { key: "purchasePrice", label: "Giá mua", render: (value) => formatMoney(value) },
        { key: "minimumOrderQuantity", label: "MOQ", render: (value) => show(value) },
        { key: "leadTimeDays", label: "Lead time (ngày)", render: (value) => show(value) },
        { key: "productStatus", label: "Trạng thái SP", render: (value) => <StatusBadge status={value} /> },
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Chi tiết nhà cung cấp"
                    description="Thông tin hồ sơ, sản phẩm liên kết đang hoạt động và thời điểm cập nhật."
                    actions={<Button type="button" variant="ghost" onClick={() => navigate(ROOT)}>← Danh sách</Button>}
                />
                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading ? (
                    <div role="status" className="flex items-center gap-3 rounded-xl bg-white p-8 text-sm text-slate-600"><Spinner /> Đang tải nhà cung cấp...</div>
                ) : supplier && (
                    <>
                        <Card>
                            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 pb-5">
                                <div className="space-y-2">
                                    <p className="text-sm font-semibold text-[#c94810]">{supplier.supplierCode}</p>
                                    <h2 className="text-xl font-bold text-slate-900">{supplier.name}</h2>
                                    <StatusBadge status={supplier.status} />
                                </div>
                                <div className="flex flex-wrap gap-2">
                                    <Button type="button" variant="outline" onClick={() => navigate(`${ROOT}/${supplierId}/edit`)}>Chỉnh sửa</Button>
                                    <Button type="button" variant={supplier.status === "ACTIVE" ? "danger" : "secondary"} onClick={() => setConfirmOpen(true)}>
                                        {supplier.status === "ACTIVE" ? "Ẩn nhà cung cấp" : "Khôi phục"}
                                    </Button>
                                </div>
                            </div>
                            <dl className="mt-6 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
                                <InfoItem label="Mã số thuế" value={supplier.taxCode} />
                                <InfoItem label="Người liên hệ" value={supplier.contactPerson} />
                                <InfoItem label="Chức vụ" value={supplier.contactPosition} />
                                <InfoItem label="Số điện thoại" value={supplier.phone} />
                                <InfoItem label="Email" value={supplier.email} />
                                <InfoItem label="Địa chỉ" value={supplier.address} />
                                <InfoItem label="Ngày tạo" value={formatDate(supplier.createdAt)} />
                                <InfoItem label="Cập nhật lần cuối" value={formatDate(supplier.updatedAt)} />
                            </dl>
                            <div className="mt-6 border-t border-slate-100 pt-5">
                                <h3 className="font-semibold text-slate-900">Ghi chú</h3>
                                <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-600">{show(supplier.note)}</p>
                            </div>
                        </Card>

                        <section className="space-y-3">
                            <div>
                                <h2 className="text-lg font-bold text-slate-900">Sản phẩm liên kết đang hoạt động ({products.length})</h2>
                                <p className="text-sm text-slate-500">Nguồn: GET /api/business-staff/suppliers/{supplierId}/products</p>
                            </div>
                            {productsLoading ? (
                                <p className="text-sm text-slate-500" role="status">Đang tải sản phẩm liên kết...</p>
                            ) : productsError ? <div role="alert"><Alert type="warning">{productsError}</Alert></div> : products.length ? (
                                <Table columns={columns} data={products} rowKey="supplierProductId" />
                            ) : <EmptyState title="Chưa có sản phẩm liên kết hoạt động" description="Nhà cung cấp chưa có sản phẩm liên kết ở trạng thái ACTIVE." />}
                        </section>
                        <div className="rounded-xl bg-slate-100 p-4 text-xs leading-5 text-slate-600">
                            Backend ghi lịch sử tạo, sửa và thay đổi trạng thái trong activity_logs. Chưa có API đọc lịch sử nhà cung cấp nên trang này chỉ hiển thị ngày tạo và ngày cập nhật.
                        </div>
                    </>
                )}
            </div>
            <Modal
                open={confirmOpen}
                title={supplier?.status === "ACTIVE" ? "Xác nhận ẩn nhà cung cấp" : "Xác nhận khôi phục"}
                onClose={() => { if (!savingStatus) setConfirmOpen(false); }}
                footer={(
                    <>
                        <Button type="button" variant="ghost" disabled={savingStatus} onClick={() => setConfirmOpen(false)}>Hủy</Button>
                        <Button type="button" variant={supplier?.status === "ACTIVE" ? "danger" : "primary"} disabled={savingStatus} onClick={handleStatusChange}>
                            {savingStatus ? "Đang xử lý..." : "Xác nhận"}
                        </Button>
                    </>
                )}
            >
                <p className="text-sm text-slate-700">
                    {supplier?.status === "ACTIVE"
                        ? "Backend sẽ từ chối ẩn nếu nhà cung cấp còn đơn mua hàng chưa hoàn thành."
                        : "Bạn muốn khôi phục nhà cung cấp này về trạng thái hoạt động?"}
                </p>
                <p className="mt-3 text-xs text-amber-700">Đây là thay đổi thật trên database chung của nhóm.</p>
            </Modal>
        </MainLayout>
    );
}
