
import { useEffect, useState } from "react";

import {
    listCategories,
    createCategory,
    updateCategory,
    deactivateCategory,
    activateCategory,
    categoryErrorMessage,
} from "../../../api/business/categories";

import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";

import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Checkbox from "../../../components/ui/Checkbox";
import EmptyState from "../../../components/ui/EmptyState";
import Input from "../../../components/ui/Input";
import Modal from "../../../components/ui/Modal";
import Pagination from "../../../components/ui/Pagination";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả trạng thái" },
    { value: "ACTIVE", label: "Hoạt động" },
    { value: "INACTIVE", label: "Đã ẩn" },
];

const EMPTY_FORM = {
    categoryCode: "",
    name: "",
    description: "",
};

export default function CategoryListPage() {
    // Danh sach va bo loc
    const [searchInput, setSearchInput] = useState("");
    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    // Modal them/sua
    const [formMode, setFormMode] = useState(null);
    const [editingCategory, setEditingCategory] = useState(null);
    const [form, setForm] = useState({ ...EMPTY_FORM });
    const [formError, setFormError] = useState("");
    const [formConfirmed, setFormConfirmed] = useState(false);

    // Modal an/khoi phuc
    const [visibilityTarget, setVisibilityTarget] = useState(null);
    const [visibilityError, setVisibilityError] = useState("");
    const [visibilityConfirmed, setVisibilityConfirmed] = useState(false);

    const [saving, setSaving] = useState(false);

    // 1. Tai danh sach danh muc
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        listCategories(
            { keyword, status, page, size: PAGE_SIZE },
            controller.signal
        )
            .then((result) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(result?.content)) {
                    throw new Error("Invalid category response");
                }

                setData(result);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;

                setError(
                    categoryErrorMessage(
                        requestError,
                        "Không tải được danh sách danh mục."
                    )
                );
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [keyword, status, page, reload]);

    const rows = data?.content || [];

    // 2. Tim kiem
    const applySearch = () => {
        setKeyword(searchInput.trim());
        setPage(0);
        setReload((current) => current + 1);
    };

    // 3. Mo form them
    const openCreate = () => {
        setFormMode("create");
        setEditingCategory(null);
        setForm({ ...EMPTY_FORM });
        setFormError("");
        setFormConfirmed(false);
        setNotice("");
    };

    // 4. Mo form sua
    const openEdit = (category) => {
        setFormMode("edit");
        setEditingCategory(category);

        setForm({
            categoryCode: category.categoryCode || "",
            name: category.name || "",
            description: category.description || "",
        });

        setFormError("");
        setFormConfirmed(false);
        setNotice("");
    };

    const closeForm = () => {
        if (saving) return;

        setFormMode(null);
        setEditingCategory(null);
        setFormError("");
        setFormConfirmed(false);
    };

    const changeForm = (field, value) => {
        setForm((current) => ({
            ...current,
            [field]: value,
        }));

        setFormError("");
        setFormConfirmed(false);
    };

    // 5. Luu danh muc
    const saveCategory = async (event) => {
        event?.preventDefault();

        if (!formMode || saving || !formConfirmed) return;

        const categoryCode = form.categoryCode.trim();
        const name = form.name.trim();
        const description = form.description.trim();

        if (!categoryCode || !name) {
            setFormError("Mã và tên danh mục không được để trống.");
            return;
        }

        if (
            categoryCode.length > 50 ||
            name.length > 150 ||
            description.length > 1000
        ) {
            setFormError("Thông tin vượt quá độ dài cho phép.");
            return;
        }

        const payload = {
            categoryCode,
            name,
            description: description || null,
        };

        setSaving(true);
        setFormError("");

        try {
            if (formMode === "create") {
                await createCategory(payload);
                setNotice("Thêm danh mục thành công.");
            } else {
                await updateCategory(editingCategory.id, payload);
                setNotice("Cập nhật danh mục thành công.");
            }

            closeFormAfterSave();
            setReload((current) => current + 1);
        } catch (requestError) {
            setFormError(
                categoryErrorMessage(
                    requestError,
                    "Không thể lưu danh mục."
                )
            );
        } finally {
            setSaving(false);
        }
    };

    const closeFormAfterSave = () => {
        setFormMode(null);
        setEditingCategory(null);
        setFormConfirmed(false);
        setFormError("");
    };

    // 6. Mo modal an/khoi phuc
    const openVisibility = (category, makeActive) => {
        setVisibilityTarget({ category, makeActive });
        setVisibilityError("");
        setVisibilityConfirmed(false);
        setNotice("");
    };

    const closeVisibility = () => {
        if (saving) return;

        setVisibilityTarget(null);
        setVisibilityError("");
        setVisibilityConfirmed(false);
    };

    // 7. Thay doi trang thai danh muc
    const submitVisibility = async () => {
        if (!visibilityTarget || !visibilityConfirmed || saving) {
            return;
        }

        const { category, makeActive } = visibilityTarget;

        setSaving(true);
        setVisibilityError("");

        try {
            if (makeActive) {
                await activateCategory(category.id);
            } else {
                await deactivateCategory(category.id);
            }

            setNotice(
                makeActive
                    ? "Khôi phục danh mục thành công."
                    : "Ẩn danh mục thành công."
            );

            setVisibilityTarget(null);
            setVisibilityConfirmed(false);
            setReload((current) => current + 1);
        } catch (requestError) {
            setVisibilityError(
                categoryErrorMessage(
                    requestError,
                    "Không thể thay đổi trạng thái danh mục."
                )
            );
        } finally {
            setSaving(false);
        }
    };

    // 8. Cau hinh cot bang
    const columns = [
        {
            key: "categoryCode",
            label: "Mã danh mục",
            render: (value) => (
                <span className="font-semibold text-[#c94810]">
                    {value || "—"}
                </span>
            ),
        },
        {
            key: "name",
            label: "Tên danh mục",
            render: (value) => (
                <span className="font-medium text-slate-900">
                    {value || "—"}
                </span>
            ),
        },
        {
            key: "description",
            label: "Mô tả",
            render: (value) => value || "—",
        },
        {
            key: "status",
            label: "Trạng thái",
            render: (value) => (
                <span
                    className={
                        value === "ACTIVE"
                            ? "inline-flex rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-700"
                            : "inline-flex rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600"
                    }
                >
                    {value === "ACTIVE" ? "Hoạt động" : "Đã ẩn"}
                </span>
            ),
        },
        {
            key: "actions",
            label: "Thao tác",
            render: (_, category) => (
                <div className="flex flex-wrap gap-2">
                    <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openEdit(category)}
                    >
                        Chỉnh sửa
                    </Button>

                    {category.status === "ACTIVE" && (
                        <Button
                            size="sm"
                            variant="danger"
                            onClick={() => openVisibility(category, false)}
                        >
                            Ẩn
                        </Button>
                    )}

                    {category.status === "INACTIVE" && (
                        <Button
                            size="sm"
                            variant="success"
                            onClick={() => openVisibility(category, true)}
                        >
                            Khôi phục
                        </Button>
                    )}
                </div>
            ),
        },
    ];

    return (
        <MainLayout
            title="Business Staff"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Quản lý danh mục sản phẩm"
                    description="Quản lý mã, tên, mô tả và trạng thái danh mục sản phẩm."
                    actions={
                        <Button onClick={openCreate}>
                            + Thêm danh mục
                        </Button>
                    }
                />

                {notice && (
                    <div role="status">
                        <Alert type="success">{notice}</Alert>
                    </div>
                )}

                {error && (
                    <div role="alert">
                        <Alert type="danger">{error}</Alert>
                    </div>
                )}

                {/* Tim kiem va loc */}
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <div className="flex flex-wrap items-end gap-3">
                        <div className="min-w-60 flex-1">
                            <Input
                                label="Tìm theo mã hoặc tên danh mục"
                                placeholder="Nhập mã hoặc tên danh mục..."
                                value={searchInput}
                                onChange={(event) =>
                                    setSearchInput(event.target.value)
                                }
                                onKeyDown={(event) => {
                                    if (event.key === "Enter") {
                                        event.preventDefault();
                                        applySearch();
                                    }
                                }}
                            />
                        </div>

                        <Button
                            variant="outline"
                            onClick={applySearch}
                        >
                            Tìm kiếm
                        </Button>

                        <div className="min-w-48">
                            <Select
                                label="Lọc trạng thái"
                                value={status}
                                options={STATUS_OPTIONS}
                                onChange={(event) => {
                                    setStatus(event.target.value);
                                    setPage(0);
                                }}
                            />
                        </div>

                        <Button
                            variant="ghost"
                            onClick={() =>
                                setReload((current) => current + 1)
                            }
                        >
                            Làm mới
                        </Button>
                    </div>

                    <p className="mt-4 text-sm text-slate-500">
                        Tổng số: {data?.totalElements ?? "—"} danh mục.
                        Danh sách được sắp xếp theo tên.
                    </p>
                </div>

                {/* Bang danh muc */}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm text-slate-600">
                        <Spinner />
                        Đang tải danh mục...
                    </div>
                ) : error ? null : rows.length === 0 ? (
                    <EmptyState
                        title="Không có danh mục"
                        description="Không tìm thấy danh mục phù hợp với điều kiện đã chọn."
                    />
                ) : (
                    <Table columns={columns} data={rows} />
                )}

                {/* Phan trang */}
                {!loading &&
                    !error &&
                    data &&
                    data.totalPages > 0 && (
                        <Pagination
                            page={page + 1}
                            totalPages={data.totalPages}
                            onChange={(selected) =>
                                setPage(selected - 1)
                            }
                        />
                    )}

                {/* Modal them/sua */}
                <Modal
                    open={Boolean(formMode)}
                    title={
                        formMode === "create"
                            ? "Thêm danh mục"
                            : "Chỉnh sửa danh mục"
                    }
                    onClose={closeForm}
                    footer={
                        <>
                            <Button
                                variant="ghost"
                                disabled={saving}
                                onClick={closeForm}
                            >
                                Hủy
                            </Button>

                            <Button
                                disabled={!formConfirmed || saving}
                                onClick={saveCategory}
                            >
                                {saving ? "Đang lưu..." : "Lưu danh mục"}
                            </Button>
                        </>
                    }
                >
                    <form onSubmit={saveCategory} className="space-y-4">
                        {formError && (
                            <Alert type="danger">
                                {formError}
                            </Alert>
                        )}

                        <Input
                            label="Mã danh mục *"
                            placeholder="Ví dụ: CAT-TOYS"
                            maxLength={50}
                            required
                            value={form.categoryCode}
                            onChange={(event) =>
                                changeForm(
                                    "categoryCode",
                                    event.target.value
                                )
                            }
                        />

                        <Input
                            label="Tên danh mục *"
                            placeholder="Nhập tên danh mục"
                            maxLength={150}
                            required
                            value={form.name}
                            onChange={(event) =>
                                changeForm(
                                    "name",
                                    event.target.value
                                )
                            }
                        />

                        <label className="block space-y-1.5">
                            <span className="text-sm font-medium text-slate-700">
                                Mô tả
                            </span>

                            <textarea
                                rows={3}
                                maxLength={1000}
                                value={form.description}
                                onChange={(event) =>
                                    changeForm(
                                        "description",
                                        event.target.value
                                    )
                                }
                                placeholder="Nhập mô tả (không bắt buộc)"
                                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/20"
                            />

                            <span className="block text-xs text-slate-500">
                                {form.description.length}/1000 ký tự
                            </span>
                        </label>

                        <p className="text-sm text-amber-700">
                            Thao tác này sẽ cập nhật thông tin danh mục trong hệ thống.
                        </p>

                        <Checkbox
                            checked={formConfirmed}
                            onChange={(event) =>
                                setFormConfirmed(event.target.checked)
                            }
                            label="Tôi xác nhận thông tin danh mục là chính xác."
                        />
                    </form>
                </Modal>

                {/* Modal an/khoi phuc */}
                <Modal
                    open={Boolean(visibilityTarget)}
                    title={
                        visibilityTarget?.makeActive
                            ? "Khôi phục danh mục"
                            : "Ẩn danh mục"
                    }
                    onClose={closeVisibility}
                    footer={
                        <>
                            <Button
                                variant="ghost"
                                disabled={saving}
                                onClick={closeVisibility}
                            >
                                Hủy
                            </Button>

                            <Button
                                variant={
                                    visibilityTarget?.makeActive
                                        ? "success"
                                        : "danger"
                                }
                                disabled={!visibilityConfirmed || saving}
                                onClick={submitVisibility}
                            >
                                {saving ? "Đang xử lý..." : "Xác nhận"}
                            </Button>
                        </>
                    }
                >
                    <div className="space-y-4">
                        {visibilityError && (
                            <Alert type="danger">
                                {visibilityError}
                            </Alert>
                        )}

                        <p className="text-sm text-slate-700">
                            Bạn có chắc muốn{" "}
                            <strong>
                                {visibilityTarget?.makeActive
                                    ? "khôi phục"
                                    : "ẩn"}
                            </strong>{" "}
                            danh mục{" "}
                            <strong>
                                {visibilityTarget?.category?.name}
                            </strong>
                            ?
                        </p>

                        <p className="text-sm text-slate-500">
                            Mã danh mục:{" "}
                            {visibilityTarget?.category?.categoryCode}
                        </p>

                        <p className="text-sm text-amber-700">
                            Danh mục không bị xóa khỏi cơ sở dữ liệu.
                            Danh mục đã ẩn sẽ không xuất hiện trong
                            danh sách lựa chọn ACTIVE khi tạo sản phẩm.
                            Backend có thể từ chối ẩn nếu danh mục
                            đang liên quan đến dữ liệu chờ xử lý.
                        </p>

                        <Checkbox
                            checked={visibilityConfirmed}
                            onChange={(event) =>
                                setVisibilityConfirmed(event.target.checked)
                            }
                            label="Tôi xác nhận thay đổi trạng thái danh mục."
                        />
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
}
