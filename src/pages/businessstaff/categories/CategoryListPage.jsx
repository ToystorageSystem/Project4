
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
    { value: "", label: "All Statuses" },
    { value: "ACTIVE", label: "Active" },
    { value: "INACTIVE", label: "Inactive" },
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
                    throw new Error("Invalid product category list data");
                }

                setData(result);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;

                setError(
                    categoryErrorMessage(
                        requestError,
                        "Unable to load categories."
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
            setFormError("Category code and name are required.");
            return;
        }

        if (
            categoryCode.length > 50 ||
            name.length > 150 ||
            description.length > 1000
        ) {
            setFormError("Information exceeds the maximum allowed length.");
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
                setNotice("Category added successfully.");
            } else {
                await updateCategory(editingCategory.id, payload);
                setNotice("Category updated successfully.");
            }

            closeFormAfterSave();
            setReload((current) => current + 1);
        } catch (requestError) {
            setFormError(
                categoryErrorMessage(
                    requestError,
                    "Unable to save category."
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
                    ? "Category restored successfully."
                    : "Category hidden successfully."
            );

            setVisibilityTarget(null);
            setVisibilityConfirmed(false);
            setReload((current) => current + 1);
        } catch (requestError) {
            setVisibilityError(
                categoryErrorMessage(
                    requestError,
                    "Unable to change category status."
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
            label: "Category ID",
            render: (value) => (
                <span className="font-semibold text-[#c94810]">
                    {value || "—"}
                </span>
            ),
        },
        {
            key: "name",
            label: "Category Name",
            render: (value) => (
                <span className="font-medium text-slate-900">
                    {value || "—"}
                </span>
            ),
        },
        {
            key: "description",
            label: "Description",
            render: (value) => value || "—",
        },
        {
            key: "status",
            label: "Status",
            render: (value) => (
                <span
                    className={
                        value === "ACTIVE"
                            ? "inline-flex rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-700"
                            : "inline-flex rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600"
                    }
                >
                    {value === "ACTIVE" ? "Active" : "Inactive"}
                </span>
            ),
        },
        {
            key: "actions",
            label: "Actions",
            render: (_, category) => (
                <div className="flex flex-wrap gap-2">
                    <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openEdit(category)}
                    >
                        Edit
                    </Button>

                    {category.status === "ACTIVE" && (
                        <Button
                            size="sm"
                            variant="danger"
                            onClick={() => openVisibility(category, false)}
                        >
                            Inactive
                        </Button>
                    )}

                    {category.status === "INACTIVE" && (
                        <Button
                            size="sm"
                            variant="success"
                            onClick={() => openVisibility(category, true)}
                        >
                            Restore
                        </Button>
                    )}
                </div>
            ),
        },
    ];

    return (
        <MainLayout
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Product Category Management"
                    description="Manage category codes, names, descriptions, and status."
                    actions={
                        <Button onClick={openCreate}>
                            + Add Category
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
                                label="Search by category code or name"
                                placeholder="Enter category code or name..."
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
                            Search
                        </Button>

                        <div className="min-w-48">
                            <Select
                                label="Status Filter"
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
                            Refresh
                        </Button>
                    </div>

                    <p className="mt-4 text-sm text-slate-500">
                        Total: {data?.totalElements ?? "—"} categories. Sorted alphabetically by name.
                    </p>
                </div>

                {/* Bang danh muc */}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm text-slate-600">
                        <Spinner />
                        Loading categories...
                    </div>
                ) : error ? null : rows.length === 0 ? (
                    <EmptyState
                        title="No Categories"
                        description="No categories match the selected filters."
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
                            ? "Add Category"
                            : "Edit Category"
                    }
                    onClose={closeForm}
                    footer={
                        <>
                            <Button
                                variant="ghost"
                                disabled={saving}
                                onClick={closeForm}
                            >
                                Cancel
                            </Button>

                            <Button
                                disabled={!formConfirmed || saving}
                                onClick={saveCategory}
                            >
                                {saving ? "Saving..." : "Save Category"}
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
                            label="Category Code *"
                            placeholder="Example: CAT-TOYS"
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
                            label="Category Name *"
                            placeholder="Enter category name"
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
                                Description
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
                                placeholder="Enter a description (optional)"
                                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/20"
                            />

                            <span className="block text-xs text-slate-500">
                                {form.description.length}/1000 characters
                            </span>
                        </label>

                        <p className="text-sm text-amber-700">
                            This action will update category information in the system.
                        </p>

                        <Checkbox
                            checked={formConfirmed}
                            onChange={(event) =>
                                setFormConfirmed(event.target.checked)
                            }
                            label="I confirm the category information is correct."
                        />
                    </form>
                </Modal>

                {/* Modal an/khoi phuc */}
                <Modal
                    open={Boolean(visibilityTarget)}
                    title={
                        visibilityTarget?.makeActive
                            ? "Restore Category"
                            : "Hide Category"
                    }
                    onClose={closeVisibility}
                    footer={
                        <>
                            <Button
                                variant="ghost"
                                disabled={saving}
                                onClick={closeVisibility}
                            >
                                Cancel
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
                                {saving ? "Processing..." : "Confirm"}
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
                            Are you sure you want to{" "}
                            <strong>
                                {visibilityTarget?.makeActive
                                    ? "restore"
                                    : "hide"}
                            </strong>{" "}
                            category{" "}
                            <strong>
                                {visibilityTarget?.category?.name}
                            </strong>
                            ?
                        </p>

                        <p className="text-sm text-slate-500">
                            Category Code:{" "}
                            {visibilityTarget?.category?.categoryCode}
                        </p>

                        <p className="text-sm text-amber-700">
                            Categories are not deleted from the database. Hidden categories are excluded from active category selections when creating products. Hiding a category may be restricted when related requests are pending.
                        </p>

                        <Checkbox
                            checked={visibilityConfirmed}
                            onChange={(event) =>
                                setVisibilityConfirmed(event.target.checked)
                            }
                            label="I confirm this category status change."
                        />
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
}
