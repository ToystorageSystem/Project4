import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { importProductExcel, previewProductExcel, productErrorMessage } from "../../../api/business/products";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Checkbox from "../../../components/ui/Checkbox";
import Table from "../../../components/ui/Table";

const ROOT = "/business-staff/products";
const HEADERS = [
    "productCode", "barcode", "imageUrl", "name", "categoryId",
    "brandId", "baseUnit", "purchasePrice", "sellingPrice", "requestReason",
];

export default function ProductImportPage() {
    const navigate = useNavigate();
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);
    const [result, setResult] = useState(null);
    const [checking, setChecking] = useState(false);
    const [importing, setImporting] = useState(false);
    const [confirmed, setConfirmed] = useState(false);
    const [error, setError] = useState("");

    const chooseFile = (event) => {
        setFile(event.target.files?.[0] || null);
        setPreview(null);
        setResult(null);
        setConfirmed(false);
        setError("");
    };

    const checkExcel = async () => {
        if (!file || checking || importing) return;
        if (!/\.(xlsx|xls)$/i.test(file.name)) {
            setError("Chỉ hỗ trợ .xlsx hoặc .xls.");
            return;
        }
        setChecking(true);
        setError("");
        setPreview(null);
        setResult(null);
        try {
            const response = await previewProductExcel(file);
            if (!Array.isArray(response?.rows)) throw new Error("Invalid preview response");
            setPreview(response);
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Không thể xem trước file Excel."));
        } finally {
            setChecking(false);
        }
    };

    const confirmImport = async () => {
        if (!file || !confirmed || importing || !preview || preview.invalidRows !== 0 || preview.validRows < 1) return;
        setImporting(true);
        setError("");
        try {
            const response = await importProductExcel(file);
            setResult(response);
            setPreview(null);
            setConfirmed(false);
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Không thể nhập file Excel. Hãy xem trước lại trước khi thử tiếp."));
            setPreview(null); // The server revalidates; a fresh preview is required.
            setConfirmed(false);
        } finally {
            setImporting(false);
        }
    };

    const columns = [
        { key: "rowNumber", label: "Dòng" },
        { key: "productCode", label: "Mã SP", render: (value) => value || "—" },
        { key: "barcode", label: "Barcode", render: (value) => value || "—" },
        { key: "name", label: "Tên sản phẩm", render: (value) => value || "—" },
        { key: "categoryId", label: "Danh mục ID", render: (value) => value ?? "—" },
        { key: "brandId", label: "Thương hiệu ID", render: (value) => value ?? "—" },
        { key: "valid", label: "Kết quả", render: (value) => <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${value ? "bg-emerald-100 text-emerald-800" : "bg-red-100 text-red-800"}`}>{value ? "Hợp lệ" : "Có lỗi"}</span> },
        { key: "errors", label: "Lỗi", render: (value) => value?.length ? <ul className="list-disc space-y-1 pl-4 text-xs text-red-700">{value.map((item, i) => <li key={i}>{item}</li>)}</ul> : "—" },
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="Nhập sản phẩm bằng Excel" description="Xem trước toàn bộ các dòng hợp lệ/không hợp lệ trước khi xác nhận. Sản phẩm nhập sẽ chờ duyệt."
                    actions={<Button variant="ghost" onClick={() => navigate(ROOT)}>← Danh sách sản phẩm</Button>}
                />
                <Card>
                    <div className="space-y-4">
                        <Alert type="info">
                            Chọn Excel .xlsx hoặc .xls. <strong>Sheet đầu tiên</strong> phải có dòng tiêu đề gồm chính xác 10 cột theo thứ tự sau (phân biệt chữ hoa/thường):
                            <div className="mt-2 overflow-x-auto rounded-lg bg-white p-3 font-mono text-xs text-slate-800">{HEADERS.join(" | ")}</div>
                            <p className="mt-2 text-xs">brandId, imageUrl và requestReason có thể để trống. categoryId và các trường bắt buộc phải hợp lệ. Backend kiểm tra trùng mã/barcode trong file và database.</p>
                        </Alert>
                        <div className="flex flex-wrap items-center gap-3">
                            <input type="file" accept=".xlsx,.xls" aria-label="Chọn file Excel sản phẩm" onChange={chooseFile} className="max-w-full text-sm text-slate-600" />
                            <Button onClick={checkExcel} disabled={!file || checking || importing}>
                                {checking ? "Đang kiểm tra..." : "Xem trước / kiểm tra"}
                            </Button>
                        </div>
                        {file && <p className="text-xs text-slate-500">File: {file.name}</p>}
                    </div>
                </Card>
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {result && <div role="status"><Alert type="success">Đã gửi {result.importedRows} / {result.totalRows} sản phẩm từ Excel để Business Manager duyệt. Các sản phẩm chưa ở trạng thái ACTIVE.</Alert></div>}
                {preview && (
                    <>
                        <div className="grid gap-4 sm:grid-cols-3">
                            <Card><p className="text-sm text-slate-500">Tổng dòng</p><p className="mt-1 text-2xl font-bold text-slate-900">{preview.totalRows}</p></Card>
                            <Card><p className="text-sm text-slate-500">Hợp lệ</p><p className="mt-1 text-2xl font-bold text-emerald-700">{preview.validRows}</p></Card>
                            <Card><p className="text-sm text-slate-500">Không hợp lệ</p><p className="mt-1 text-2xl font-bold text-red-700">{preview.invalidRows}</p></Card>
                        </div>
                        <Card className="space-y-4">
                            <h2 className="text-lg font-semibold text-slate-900">Kết quả kiểm tra từng dòng</h2>
                            <p className="text-sm text-slate-500">Đây là preview từ Backend, chưa tạo sản phẩm. Có thể cuộn bảng để xem các dòng.</p>
                            <div className="max-h-[440px] overflow-auto"><Table columns={columns} data={preview.rows} rowKey="rowNumber" /></div>
                            {preview.invalidRows > 0 ? (
                                <Alert type="danger">File còn {preview.invalidRows} dòng không hợp lệ. Sửa trong Excel và xem trước lại. Backend không cho import một phần file có lỗi.</Alert>
                            ) : (
                                <Alert type="success">Toàn bộ {preview.validRows} dòng hợp lệ tại thời điểm preview. Backend sẽ kiểm tra lại khi import.</Alert>
                            )}
                            {preview.invalidRows === 0 && preview.validRows > 0 && (
                                <div className="space-y-4 border-t border-slate-100 pt-4">
                                    <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                        <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} label="Tôi xác nhận sẽ tạo yêu cầu sản phẩm thật trên MySQL chung của nhóm (cần Business Manager duyệt)." />
                                    </div>
                                    <div className="flex justify-end">
                                        <Button onClick={confirmImport} disabled={!confirmed || importing}>{importing ? "Đang nhập..." : `Xác nhận nhập ${preview.validRows} sản phẩm`}</Button>
                                    </div>
                                </div>
                            )}
                        </Card>
                    </>
                )}
            </div>
        </MainLayout>
    );
}
