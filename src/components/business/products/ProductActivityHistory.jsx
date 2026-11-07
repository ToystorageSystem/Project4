import { useEffect, useMemo, useState } from "react";
import { getProductHistory, productErrorMessage } from "../../../api/business/products";
import { formatProductDate, formatProductPrice } from "./ProductStatusBadge";

// Các trường quan trọng với nhân viên nghiệp vụ. Không hiện raw JSON cho người dùng.
const FIELDS = [
  ["productCode", "Mã sản phẩm"],
  ["barcode", "Mã vạch"],
  ["name", "Tên sản phẩm"],
  ["categoryName", "Danh mục"],
  ["brandId", "Mã thương hiệu"],
  ["baseUnit", "Đơn vị tính"],
  ["purchasePrice", "Giá nhập"],
  ["sellingPrice", "Giá bán"],
  ["status", "Trạng thái"],
  ["imageUrl", "Đường dẫn hình ảnh"],
];

const STATUS = {
  ACTIVE: "Đang hoạt động",
  INACTIVE: "Tạm ẩn",
  DISCONTINUED: "Ngừng kinh doanh",
  REJECTED: "Bị từ chối (dữ liệu cũ)",
  PENDING_CREATE: "Chờ duyệt cũ",
  PENDING_UPDATE: "Chờ sửa cũ",
};

function parseSnapshot(value) {
  if (!value) return {};
  try { return JSON.parse(value); } catch { return {}; }
}

function displayValue(key, value) {
  if (value == null || value === "") return "—";
  if (key === "status") return STATUS[value] || value;
  if (key === "purchasePrice" || key === "sellingPrice") return formatProductPrice(value);
  return String(value);
}

function getChanges(log) {
  const before = parseSnapshot(log.oldValue);
  const after = parseSnapshot(log.newValue);
  const created = log.action === "CREATE";
  return FIELDS.filter(([key]) => {
    if (created) return ["productCode", "name", "categoryName", "status"].includes(key);
    return String(before[key] ?? "") !== String(after[key] ?? "");
  }).map(([key, label]) => ({
    key,
    label,
    oldText: created ? null : displayValue(key, before[key]),
    newText: displayValue(key, after[key]),
  }));
}

export default function ProductActivityHistory({ productId }) {
  const [page, setPage] = useState(0);
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getProductHistory(productId, { page, size: 8 }, controller.signal)
      .then((data) => { if (!controller.signal.aborted) setResponse(data); })
      .catch((cause) => { if (!controller.signal.aborted) setError(productErrorMessage(cause, "Không tải được lịch sử sản phẩm.")); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [productId, page]);

  const logs = useMemo(() => response?.content || [], [response]);

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" aria-label="Lịch sử thay đổi sản phẩm">
      <h2 className="text-lg font-bold text-slate-900">Lịch sử thay đổi</h2>
      <p className="mb-4 mt-1 text-sm text-slate-500">Lưu người thao tác, thời gian và những thông tin được thay đổi. Không cần phê duyệt.</p>
      {loading && <p className="text-sm text-slate-500">Đang tải lịch sử...</p>}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {!loading && !error && logs.length === 0 && <p className="text-sm text-slate-500">Chưa có lịch sử kể từ khi áp dụng quy trình mới. Các yêu cầu cũ vẫn được lưu trong hệ thống.</p>}
      {!loading && !error && logs.map((log) => {
        const changes = getChanges(log);
        const note = parseSnapshot(log.newValue).changeNote;
        return (
          <article key={log.id} className="mb-3 rounded-xl border border-slate-100 bg-slate-50 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <strong className="text-sm text-slate-900">{log.action === "CREATE" ? "Tạo sản phẩm" : "Cập nhật sản phẩm"}</strong>
              <time className="text-xs text-slate-500">{formatProductDate(log.createdAt)}</time>
            </div>
            <p className="mt-1 text-xs text-slate-600">Người thực hiện: {log.changedByName || "Hệ thống"}</p>
            {note && <p className="mt-2 text-sm text-slate-700"><strong>Ghi chú:</strong> {note}</p>}
            {changes.length > 0 && (
              <ul className="mt-3 space-y-2 text-sm">
                {changes.map((change) => (
                  <li key={change.key} className="rounded-lg bg-white px-3 py-2">
                    <span className="font-medium text-slate-700">{change.label}: </span>
                    {change.oldText != null && <><span className="text-slate-500">{change.oldText}</span><span className="mx-2 text-slate-400">→</span></>}
                    <span className="font-semibold text-slate-900">{change.newText}</span>
                  </li>
                ))}
              </ul>
            )}
          </article>
        );
      })}
      {!loading && !error && response && response.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between gap-3 text-sm">
          <button type="button" disabled={page === 0} onClick={() => setPage((v) => Math.max(0, v - 1))} className="rounded-lg border px-3 py-2 disabled:opacity-40">Trang trước</button>
          <span>Trang {page + 1} / {response.totalPages}</span>
          <button type="button" disabled={page + 1 >= response.totalPages} onClick={() => setPage((v) => v + 1)} className="rounded-lg border px-3 py-2 disabled:opacity-40">Trang sau</button>
        </div>
      )}
    </section>
  );
}
