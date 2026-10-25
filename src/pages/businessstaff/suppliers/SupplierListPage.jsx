import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { changeSupplierStatus, listSuppliers, supplierErrorMessage } from "../../../api/business/suppliers";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Modal from "../../../components/ui/Modal";
import Pagination from "../../../components/ui/Pagination";
import SearchInput from "../../../components/ui/SearchInput";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import StatusBadge from "../../../components/ui/StatusBadge";
import Table from "../../../components/ui/Table";

const ROOT = "/business-staff/suppliers";
const PAGE_SIZE = 10;
const STATUS_OPTIONS = [
    { value: "", label: "Tất cả trạng thái" },
    { value: "ACTIVE", label: "Đang hoạt động" },
    { value: "INACTIVE", label: "Đã ẩn" },
];
const SORT_OPTIONS = [
    { value: "createdAt", label: "Ngày tạo" },
    { value: "name", label: "Tên nhà cung cấp" },
    { value: "suppliersCode", label: "Mã nhà cung cấp" },
    { value: "updatedAt", label: "Ngày cập nhật" },
];

export default function SupplierListPage() {
    const navigate = useNavigate();
    const [searchDraft, setSearchDraft] = useState("");
    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState("");
    const [sortBy, setSortBy] = useState("createdAt");
    const [direction, setDirection] = useState("DESC");
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [action, setAction] = useState(null);
    const [savingStatus, setSavingStatus] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listSuppliers({
            keyword: keyword || undefined,
            status: status || undefined,
            page,
            size: PAGE_SIZE,
            sortBy,
            direction,
        }, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data?.content)) {
                    throw new Error("Invalid supplier page response");
                }
                setResult(data);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;
                setResult(null);
                setError(supplierErrorMessage(requestError, "Không tải được danh sách nhà cung cấp."));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [keyword, status, page, sortBy, direction, reload]);

    const handleSearch = (event) => {
        event.preventDefault();
        setPage(0);
        setKeyword(searchDraft.trim());
    };

    const confirmStatusChange = async () => {
        if (!action || savingStatus) return;
        setSavingStatus(true);
        setError("");
        setNotice("");
        try {
            await changeSupplierStatus(action.id, action.status === "INACTIVE");
            setNotice(action.status === "ACTIVE"
                ? `Đã ẩn nhà cung cấp ${action.name}.`
                : `Đã khôi phục nhà cung cấp ${action.name}.`);
            setAction(null);
            setReload((value) => value + 1);
        } catch (requestError) {
            // BE is authoritative; e.g. it rejects hiding suppliers with unfinished POs.
            setError(supplierErrorMessage(requestError, "Không thể thay đổi trạng thái nhà cung cấp."));
            setAction(null);
        } finally {
            setSavingStatus(false);
        }
    };

    const columns = [
        {
            key: "supplierCode", label: "Mã NCC",
            render: (_value, supplier) => (
                <Link className="font-semibold text-[#c94810] hover:underline" to={`${ROOT}/${supplier.id}`}>
                    {supplier.supplierCode || "—"}
                </Link>
            ),
        },
        { key: "name", label: "Tên nhà cung cấp", render: (value) => <span className="font-medium text-slate-900">{value || "—"}</span> },
        { key: "contactPerson", label: "Người liên hệ", render: (value) => value || "—" },
        { key: "phone", label: "Số điện thoại", render: (value) => value || "—" },
        { key: "email", label: "Email", render: (value) => value || "—" },
        { key: "status", label: "Trạng thái", render: (value) => <StatusBadge status={value} /> },
        {
            key: "id", label: "Thao tác",
            render: (_value, supplier) => (
                <div className="flex flex-wrap items-center gap-2">
                    <Button type="button" variant="ghost" size="sm" onClick={() => navigate(`${ROOT}/${supplier.id}`)}>Chi tiết</Button>
                    <Button type="button" variant="outline" size="sm" onClick={() => navigate(`${ROOT}/${supplier.id}/edit`)}>Sửa</Button>
                    <Button
                        type="button"
                        variant={supplier.status === "ACTIVE" ? "danger" : "secondary"}
                        size="sm"
                        onClick={() => { setError(""); setNotice(""); setAction(supplier); }}
                    >
                        {supplier.status === "ACTIVE" ? "Ẩn" : "Khôi phục"}
                    </Button>
                </div>
            ),
        },
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Quản lý nhà cung cấp"
                    description="Danh sách, tìm kiếm, thông tin liên hệ và trạng thái nhà cung cấp. Mã NCC được hệ thống tự sinh."
                    actions={<Button type="button" onClick={() => navigate(`${ROOT}/new`)}>+ Thêm nhà cung cấp</Button>}
                />

                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}

                <Card>
                    <form onSubmit={handleSearch} className="grid items-end gap-3 lg:grid-cols-[minmax(200px,1fr)_180px_170px_140px_auto]">
                        <SearchInput
                            label="Tìm theo mã, tên hoặc số điện thoại"
                            value={searchDraft}
                            onChange={(event) => setSearchDraft(event.target.value)}
                            placeholder="Nhập mã, tên hoặc SĐT..."
                        />
                        <Select label="Trạng thái" options={STATUS_OPTIONS} value={status} onChange={(event) => { setPage(0); setStatus(event.target.value); }} />
                        <Select label="Sắp xếp theo" options={SORT_OPTIONS} value={sortBy} onChange={(event) => { setPage(0); setSortBy(event.target.value); }} />
                        <Select
                            label="Thứ tự"
                            options={[{ value: "DESC", label: "Giảm dần" }, { value: "ASC", label: "Tăng dần" }]}
                            value={direction}
                            onChange={(event) => { setPage(0); setDirection(event.target.value); }}
                        />
                        <Button type="submit">Tìm kiếm</Button>
                    </form>
                    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">
                        <span>{loading ? "Đang tải..." : `Tìm thấy ${result?.totalElements ?? 0} nhà cung cấp`}</span>
                        <Button type="button" variant="ghost" size="sm" onClick={() => setReload((value) => value + 1)}>Làm mới</Button>
                    </div>
                </Card>

                {loading ? (
                    <div className="flex items-center justify-center gap-3 rounded-2xl bg-white p-12 text-sm text-slate-600" role="status">
                        <Spinner /> Đang tải danh sách nhà cung cấp...
                    </div>
                ) : result && result.content.length ? (
                    <div className="space-y-4">
                        <Table columns={columns} data={result.content} />
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <p className="text-sm text-slate-500">Trang {page + 1} / {Math.max(1, result.totalPages)}</p>
                            <Pagination
                                page={page + 1}
                                totalPages={Math.max(1, result.totalPages)}
                                onChange={(nextPage) => setPage(nextPage - 1)}
                            />
                        </div>
                    </div>
                ) : !error ? (
                    <EmptyState title="Không tìm thấy nhà cung cấp" description="Hãy thử đổi từ khóa hoặc trạng thái lọc." />
                ) : null}
            </div>

            <Modal
                open={Boolean(action)}
                title={action?.status === "ACTIVE" ? "Xác nhận ẩn nhà cung cấp" : "Xác nhận khôi phục"}
                onClose={() => { if (!savingStatus) setAction(null); }}
                footer={(
                    <>
                        <Button type="button" variant="ghost" disabled={savingStatus} onClick={() => setAction(null)}>Hủy</Button>
                        <Button
                            type="button"
                            variant={action?.status === "ACTIVE" ? "danger" : "primary"}
                            disabled={savingStatus}
                            onClick={confirmStatusChange}
                        >
                            {savingStatus ? "Đang xử lý..." : "Xác nhận"}
                        </Button>
                    </>
                )}
            >
                <p className="text-sm leading-6 text-slate-600">
                    {action?.status === "ACTIVE"
                        ? `Ẩn “${action?.name}”? Nhà cung cấp có đơn mua hàng chưa hoàn tất sẽ bị Backend từ chối.`
                        : `Khôi phục “${action?.name}” về trạng thái hoạt động?`}
                </p>
                <p className="mt-3 text-xs text-amber-700">Thao tác này cập nhật dữ liệu thật trên database chung của nhóm.</p>
            </Modal>
        </MainLayout>
    );
}
