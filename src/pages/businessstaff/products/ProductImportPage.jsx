import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertCircle, CheckCircle2, Download, FileSpreadsheet, FileUp, PackagePlus, X } from "lucide-react";
import {
    importProductExcelSimple,
    previewProductExcelSimple,
    productErrorMessage,
} from "../../../api/business/products";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";

const ROOT = "/business/products";
const TEMPLATE = "/api/products/import-simple/template";
const cell = (value) => value === null || value === undefined || value === "" ? "—" : String(value);
const money = (value) => value === null || value === undefined ? "—" : `${Number(value).toLocaleString("en-US")} VND`;

export default function ProductImportPage() {
    const navigate = useNavigate();
    const inputRef = useRef(null);
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);
    const [result, setResult] = useState(null);
    const [error, setError] = useState("");
    const [checking, setChecking] = useState(false);
    const [importing, setImporting] = useState(false);
    const [dragging, setDragging] = useState(false);

    const updateFile = (chosen) => {
        setFile(chosen ?? null);
        setPreview(null);
        setResult(null);
        setError("");
    };

    const checkExcel = async () => {
        if (!file || checking || importing) return;
        if (!/\.(xlsx|xls)$/i.test(file.name)) {
            setError("Please choose an .xlsx or .xls file.");
            return;
        }
        setChecking(true);
        setError("");
        setPreview(null);
        setResult(null);
        try {
            const response = await previewProductExcelSimple(file);
            if (!Array.isArray(response?.rows)) throw new Error("Invalid preview response");
            setPreview(response);
        } catch (err) {
            setError(productErrorMessage(err, "Unable to preview the Excel file."));
        } finally {
            setChecking(false);
        }
    };

    const confirmImport = async () => {
        if (!file || !preview || preview.invalidRows !== 0 || preview.validRows <= 0 || importing) return;
        setImporting(true);
        setError("");
        try {
            const response = await importProductExcelSimple(file);
            setResult(response);
            setPreview(null);
        } catch (err) {
            setError(productErrorMessage(err, "Import failed. Please preview again before retrying."));
            setPreview(null);
        } finally {
            setImporting(false);
        }
    };

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-5">
                <PageHeader
                    title="Import Products from Excel"
                    description="Add products and suppliers together with one easy-to-fill Excel sheet."
                    actions={<Button variant="ghost" onClick={() => navigate(ROOT)}>← Product List</Button>}
                />

                <div className="grid gap-3 md:grid-cols-3">
                    {[
                        { number: "1", title: "Download the template", description: "Use the Excel file prepared for ToyStorage." },
                        { number: "2", title: "Fill product & supplier", description: "One product and one supplier per row." },
                        { number: "3", title: "Upload & import", description: "Fix any highlighted rows, then import." },
                    ].map((step) => (
                        <Card key={step.number}>
                            <div className="flex gap-3">
                                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-orange-100 font-bold text-orange-700">{step.number}</div>
                                <div>
                                    <h2 className="font-semibold text-slate-900">{step.title}</h2>
                                    <p className="mt-1 text-sm text-slate-500">{step.description}</p>
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>

                <Card>
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex min-w-0 gap-3">
                            <FileSpreadsheet className="mt-1 shrink-0 text-orange-600" size={23} />
                            <div>
                                <h2 className="font-semibold text-slate-900">Product & Supplier Excel Template</h2>
                                <p className="mt-1 text-sm text-slate-500">Only one visible worksheet. Category, Supplier and Brand have dropdown suggestions; you can type a new supplier name.</p>
                            </div>
                        </div>
                        <a href={TEMPLATE} download className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-orange-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-orange-700">
                            <Download size={16} /> Download Excel Template
                        </a>
                    </div>
                    <div className="mt-4 grid gap-2 border-t border-slate-100 pt-4 text-sm text-slate-600 sm:grid-cols-2">
                        <p><strong className="text-slate-800">Product Code:</strong> generated automatically.</p>
                        <p><strong className="text-slate-800">Supplier:</strong> choose an existing name or type a new one.</p>
                        <p><strong className="text-slate-800">Same Barcode:</strong> allowed for different suppliers.</p>
                        <p><strong className="text-slate-800">Purchase Price / MOQ:</strong> enter on the same row.</p>
                        <p><strong className="text-slate-800">Brand (optional):</strong> choose an active brand from the list or leave it empty. A new brand must be created in Brands first.</p>
                    </div>
                </Card>

                <Card>
                    <div className="mb-3 flex items-center gap-2">
                        <FileUp size={20} className="text-orange-600" />
                        <h2 className="font-semibold text-slate-900">Upload your completed file</h2>
                    </div>
                    <div
                        className={`rounded-xl border-2 border-dashed px-5 py-8 text-center transition-colors ${dragging ? "border-orange-500 bg-orange-50" : "border-slate-200 bg-slate-50"}`}
                        onDragOver={(event) => { event.preventDefault(); setDragging(true); }}
                        onDragLeave={() => setDragging(false)}
                        onDrop={(event) => { event.preventDefault(); setDragging(false); updateFile(event.dataTransfer.files?.[0]); }}
                    >
                        <input ref={inputRef} type="file" className="sr-only" accept=".xlsx,.xls" onChange={(event) => updateFile(event.target.files?.[0])} aria-label="Choose Excel file" />
                        <FileSpreadsheet size={32} className="mx-auto text-slate-400" />
                        <p className="mt-2 text-sm font-medium text-slate-800">Drag an Excel file here, or select it from your device.</p>
                        <p className="mt-1 text-xs text-slate-500">.xlsx / .xls · up to 5 MB · up to 300 products</p>
                        <button type="button" onClick={() => inputRef.current?.click()} className="mt-4 rounded-lg border border-orange-500 bg-white px-4 py-2 text-sm font-semibold text-orange-600 hover:bg-orange-50">Choose Excel File</button>
                    </div>
                    {file && (
                        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3">
                            <div className="flex min-w-0 items-center gap-2 text-sm text-slate-700"><FileSpreadsheet size={18} className="shrink-0 text-green-700" /><span className="break-all">{file.name}</span></div>
                            <button type="button" onClick={() => { updateFile(null); if (inputRef.current) inputRef.current.value = ""; }} className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-red-700"><X size={15} /> Remove</button>
                        </div>
                    )}
                    <div className="mt-4 flex justify-end">
                        <Button disabled={!file || checking || importing} onClick={checkExcel}>
                            {checking ? "Checking file..." : "Check File & Preview"}
                        </Button>
                    </div>
                </Card>

                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {result && (
                    <Card>
                        <div role="status" className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                            <div className="flex items-center gap-2 text-green-800"><CheckCircle2 size={22} /><span className="font-semibold">Successfully imported {result.importedRows} products and their suppliers.</span></div>
                            <Button onClick={() => navigate(ROOT)}>View Products</Button>
                        </div>
                    </Card>
                )}

                {preview && (
                    <Card>
                        <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                                <h2 className="text-lg font-semibold text-slate-900">Review before importing</h2>
                                <p className="mt-1 text-sm text-slate-500">Nothing has been saved yet. Correct any invalid rows in Excel and upload again.</p>
                            </div>
                            <div className="flex flex-wrap gap-2 text-sm">
                                <span className="rounded-lg bg-slate-100 px-3 py-2">Total: <b>{preview.totalRows}</b></span>
                                <span className="rounded-lg bg-green-50 px-3 py-2 text-green-700">Valid: <b>{preview.validRows}</b></span>
                                <span className="rounded-lg bg-red-50 px-3 py-2 text-red-700">Invalid: <b>{preview.invalidRows}</b></span>
                            </div>
                        </div>
                        <div className="mt-4 max-h-[450px] overflow-auto rounded-xl border border-slate-200">
                            <table className="w-full min-w-[1050px] border-collapse text-left text-sm">
                                <thead className="sticky top-0 bg-slate-100 text-xs font-semibold uppercase tracking-wide text-slate-600">
                                    <tr>{["Row", "Product", "Barcode", "Category", "Brand", "Supplier", "Purchase / Selling", "MOQ", "Result"].map((label) => <th key={label} className="whitespace-nowrap px-3 py-3">{label}</th>)}</tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {preview.rows.map((item) => (
                                        <tr key={item.rowNumber} className={item.valid ? "bg-white" : "bg-red-50"}>
                                            <td className="px-3 py-3 font-medium">{item.rowNumber}</td>
                                            <td className="px-3 py-3 font-medium">{cell(item.name)}</td>
                                            <td className="px-3 py-3 font-mono text-xs">{cell(item.barcode)}</td>
                                            <td className="px-3 py-3">{cell(item.categoryName)}</td>
                                            <td className="px-3 py-3">{cell(item.brandName)}</td>
                                            <td className="px-3 py-3"><div>{cell(item.supplierName)}</div><div className="text-xs text-slate-500">{cell(item.supplierPhone)}</div></td>
                                            <td className="px-3 py-3"><div>Buy: {money(item.purchasePrice)}</div><div className="text-slate-500">Sell: {money(item.sellingPrice)}</div></td>
                                            <td className="px-3 py-3">{cell(item.minimumOrderQuantity)}</td>
                                            <td className="px-3 py-3">
                                                {item.valid ? <span className="inline-flex items-center gap-1 text-green-700"><CheckCircle2 size={15} /> Ready</span> : <div className="text-red-700"><span className="inline-flex items-center gap-1 font-semibold"><AlertCircle size={15} /> Fix row</span><ul className="mt-1 list-disc space-y-1 pl-4 text-xs">{item.errors?.map((message, i) => <li key={i}>{message}</li>)}</ul></div>}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {preview.invalidRows > 0 ? (
                            <div className="mt-4"><Alert type="danger">Please fix {preview.invalidRows} row(s) in your Excel file and upload again. No data has been saved.</Alert></div>
                        ) : (
                            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-lg bg-green-50 p-4">
                                <p className="text-sm font-medium text-green-800">All {preview.validRows} products are ready. Product Codes will be generated automatically.</p>
                                <Button disabled={importing} onClick={confirmImport}>
                                    <PackagePlus size={16} className="mr-1 inline" /> {importing ? "Importing..." : `Import ${preview.validRows} Products`}
                                </Button>
                            </div>
                        )}
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}
