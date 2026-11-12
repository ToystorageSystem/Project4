import { useEffect, useMemo, useState } from "react";

// Client-side search + pagination for lists that are already loaded in the browser.
// matchesSearch: case/diacritic-insensitive "contains" over the given values.
const fold = (v) =>
  String(v ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase();

export function matchesSearch(query, ...values) {
  const q = fold(query).trim();
  if (!q) return true;
  const haystack = values.flat().map(fold).join(" ");
  return q.split(/\s+/).every((word) => haystack.includes(word));
}

export function searchRows(rows, query, pick) {
  const list = Array.isArray(rows) ? rows : [];
  if (!String(query ?? "").trim()) return list;
  return list.filter((row) => matchesSearch(query, pick(row)));
}

/**
 * usePager(rows, pageSize, resetKeys) → { rows, page, pages, total, from, to, setPage, pageSize, setPageSize }
 * The page goes back to 1 whenever one of resetKeys changes (search text, filters, tab…).
 */
export default function usePager(rows, initialSize = 20, resetKeys = []) {
  const list = Array.isArray(rows) ? rows : [];
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(initialSize);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => setPage(1), [pageSize, ...resetKeys]);
  const pages = Math.max(1, Math.ceil(list.length / pageSize));
  const current = Math.min(page, pages);
  const slice = useMemo(
    () => list.slice((current - 1) * pageSize, current * pageSize),
    [list, current, pageSize],
  );
  return {
    rows: slice,
    page: current,
    pages,
    total: list.length,
    from: list.length ? (current - 1) * pageSize + 1 : 0,
    to: Math.min(current * pageSize, list.length),
    setPage,
    pageSize,
    setPageSize,
  };
}

// All primitive fields of a row (codes, names, statuses, dates…) for a generic search box.
export const rowText = (row) =>
  Object.values(row || {}).filter((v) => v != null && typeof v !== "object");
