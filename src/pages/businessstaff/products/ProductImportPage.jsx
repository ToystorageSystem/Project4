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

const ROOT = "/business/products";
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
            setError("Only .xlsx and .xls files are supported.");
            return;
        }
        setChecking(true);
        setError("");
        setPreview(null);
        setResult(null);
        try {
            const response = await previewProductExcel(file);
            if (!Array.isArray(response?.rows)) throw new Error("Invalid preview data");
            setPreview(response);
        } catch (requestError) {
            setError(productErrorMessage(requestError, "Unable to preview Excel file."));
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
            setError(productErrorMessage(requestError, "Unable to import Excel file. Preview the file again before retrying."));
            setPreview(null); // The server revalidates; a fresh preview is required.
            setConfirmed(false);
        } finally {
            setImporting(false);
        }
    };

    const columns = [
        { key: "rowNumber", label: "Row" },
        { key: "productCode", label: "SKU", render: (value) => value || "—" },
        { key: "barcode", label: "Barcode", render: (value) => value || "—" },
        { key: "name", label: "Product Name", render: (value) => value || "—" },
        { key: "categoryId", label: "Category ID", render: (value) => value ?? "—" },
        { key: "brandId", label: "Brand ID", render: (value) => value ?? "—" },
        { key: "valid", label: "Result", render: (value) => <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${value ? "bg-emerald-100 text-emerald-800" : "bg-red-100 text-red-800"}`}>{value ? "Valid" : "Invalid"}</span> },
        { key: "errors", label: "Error", render: (value) => value?.length ? <ul className="list-disc space-y-1 pl-4 text-xs text-red-700">{value.map((item, i) => <li key={i}>{item}</li>)}</ul> : "—" },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="Import Products from Excel" description="Preview all valid and invalid rows before confirming. Valid products are created immediately after import confirmation."
                    actions={<Button variant="ghost" onClick={() => navigate(ROOT)}>← Product List</Button>}
                />
                <Card>
                    <div className="space-y-4">
                        <Alert type="info">
                            Select an .xlsx or .xls Excel file. <strong>The first worksheet</strong> must contain a header row with exactly 10 columns in the following order (case-sensitive):
                            <div className="mt-2 overflow-x-auto rounded-lg bg-white p-3 font-mono text-xs text-slate-800">{HEADERS.join(" | ")}</div>
                            <></>
                        </Alert>  
                    <div className="flex flex-wrap items-center gap-3">

                        {/* Nut chon file Excel */}
                        <label className="inline-flex cursor-pointer items-center rounded-lg border border-orange-500 bg-white px-4 py-2 text-sm font-medium text-orange-600 transition hover:bg-orange-50 focus-within:ring-2 focus-within:ring-orange-300">
                            <input
                                type="file"
                                accept=".xlsx,.xls"
                                aria-label="Choose Excel File"
                                onChange={chooseFile}
                                className="sr-only"
                            />
                            Choose Excel File
                        </label>

                        {/* Hien thi ten file da chon */}
                        <span className="max-w-full break-all text-sm text-slate-600">
                            {file ? file.name : "No file selected"}
                        </span>

                        {/* Kiem tra file Excel */}
                        <Button
                            onClick={checkExcel}
                            disabled={!file || checking || importing}
                        >
                            {checking ? "Validating..." : "Preview / Validate"}
                        </Button>
                    </div>

                    </div>
                </Card>
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {result && <div role="status"><Alert type="success">Successfully imported {result.importedRows} / {result.totalRows} products from Excel. Products are activated immediately, and creation history has been recorded.</Alert></div>}
                {preview && (
                    <>
                        <div className="grid gap-4 sm:grid-cols-3">
                            <Card><p className="text-sm text-slate-500">Total Rows</p><p className="mt-1 text-2xl font-bold text-slate-900">{preview.totalRows}</p></Card>
                            <Card><p className="text-sm text-slate-500">Valid</p><p className="mt-1 text-2xl font-bold text-emerald-700">{preview.validRows}</p></Card>
                            <Card><p className="text-sm text-slate-500">Invalid</p><p className="mt-1 text-2xl font-bold text-red-700">{preview.invalidRows}</p></Card>
                        </div>
                        <Card className="space-y-4">
                            <h2 className="text-lg font-semibold text-slate-900">Row Validation Results</h2>
                            <p className="text-sm text-slate-500">This is a preview only. No products have been created. Scroll horizontally to view additional columns.</p>
                            <div className="max-h-[440px] overflow-auto"><Table columns={columns} data={preview.rows} rowKey="rowNumber" /></div>
                            {preview.invalidRows > 0 ? (
                                <Alert type="danger">The file contains {preview.invalidRows} invalid rows. Correct the Excel file and validate again. Import is allowed only when all rows are valid.</Alert>
                            ) : (
                                <Alert type="success">Inspected {preview.validRows} valid rows. The system will validate them again upon import confirmation.</Alert>
                            )}
                            {preview.invalidRows === 0 && preview.validRows > 0 && (
                                <div className="space-y-4 border-t border-slate-100 pt-4">
                                    <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                        <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} label="I confirm that valid products will be saved directly to the database." />
                                    </div>
                                    <div className="flex justify-end">
                                        <Button onClick={confirmImport} disabled={!confirmed || importing}>{importing ? "Importing..." : `Confirm Import ${preview.validRows} products`}</Button>
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

