import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getLinksBySupplier, supplierProductErrorMessage } from "../../../api/business/supplierProducts";
import Alert from "../../ui/Alert";
import Button from "../../ui/Button";
import EmptyState from "../../ui/EmptyState";
import StatusBadge from "../../ui/StatusBadge";
import Table from "../../ui/Table";

const show = (value) => value == null || value === "" ? "—" : value;
const formatMoney = (value) => value == null ? "—" : `${Number(value).toLocaleString("vi-VN")} ₫`;

export default function SupplierProductsPanel({ supplierId }) {
    const [links, setLinks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getLinksBySupplier(supplierId, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result)) throw new Error("Invalid supplier product links response");
                setLinks(result);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(supplierProductErrorMessage(requestError, "Không tải được liên kết sản phẩm."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [supplierId, reload]);

    const columns = [
        {
            key: "productCode", label: "Mã sản phẩm",
            render: (value, link) => <Link to={`/business-staff/products/${link.productId}`} className="font-semibold text-[#c94810] hover:underline">{show(value)}</Link>,
        },
        { key: "productName", label: "Tên sản phẩm", render: (value) => <span className="font-medium text-slate-900">{show(value)}</span> },
        { key: "supplierProductCode", label: "Mã SP phía NCC", render: show },
        { key: "purchasePrice", label: "Giá nhập", render: formatMoney },
        { key: "minimumOrderQuantity", label: "MOQ", render: show },
        { key: "leadTimeDays", label: "Giao (ngày)", render: show },
        { key: "status", label: "Liên kết", render: (value, link) => <div className="space-y-1"><StatusBadge status={value} />{link.defaultSupplier && <div className="text-xs font-semibold text-[#c94810]">★ NCC mặc định</div>}</div> },
        { key: "productStatus", label: "Trạng thái SP", render: (value) => <StatusBadge status={value} /> },
        { key: "id", label: "Thao tác", render: (_value, link) => <Link className="font-semibold text-[#c94810] hover:underline" to={`/business-staff/products/${link.productId}`}>Quản lý liên kết</Link> },
    ];

    return (
        <section className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h2 className="text-lg font-bold text-slate-900">Sản phẩm liên kết ({links.length})</h2>
                    <p className="mt-1 text-sm text-slate-500">Bao gồm liên kết ACTIVE và INACTIVE · Issue #8</p>
                </div>
                <Button type="button" variant="ghost" size="sm" onClick={() => setReload((value) => value + 1)}>Làm mới</Button>
            </div>
            {loading ? <p role="status" className="text-sm text-slate-500">Đang tải danh sách liên kết...</p>
                : error ? <div role="alert"><Alert type="warning">{error}</Alert></div>
                    : links.length ? <Table columns={columns} data={links} />
                        : <EmptyState title="Chưa có liên kết sản phẩm" description="Mở chi tiết một sản phẩm ACTIVE rồi chọn Liên kết nhà cung cấp để tạo mới." />}
            <p className="text-xs text-slate-500">Để tạo hoặc chỉnh sửa liên kết, mở trang chi tiết sản phẩm tương ứng. Không thay đổi sản phẩm bằng thao tác này.</p>
        </section>
    );
}
