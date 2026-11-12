// Pagination bar used under client-side paged lists (see hooks/usePager.js).
export default function Pager({ pager, sizes = [10, 20, 50, 100], label = "rows" }) {
  if (!pager || pager.total === 0) return null;
  const { page, pages, total, from, to, setPage, pageSize, setPageSize } = pager;
  return (
    <nav className="ops-pager" aria-label="Pagination">
      <span>
        {from}–{to} of {total} {label}
      </span>
      <div>
        <select aria-label="Rows per page" value={pageSize} onChange={(e) => setPageSize(Number(e.target.value))}>
          {sizes.map((s) => (
            <option key={s} value={s}>{s} / page</option>
          ))}
        </select>
        <button type="button" className="ops-button secondary" disabled={page <= 1} onClick={() => setPage(1)} aria-label="First page">«</button>
        <button type="button" className="ops-button secondary" disabled={page <= 1} onClick={() => setPage(page - 1)} aria-label="Previous page">‹</button>
        <strong>Page {page} / {pages}</strong>
        <button type="button" className="ops-button secondary" disabled={page >= pages} onClick={() => setPage(page + 1)} aria-label="Next page">›</button>
        <button type="button" className="ops-button secondary" disabled={page >= pages} onClick={() => setPage(pages)} aria-label="Last page">»</button>
      </div>
    </nav>
  );
}

// Search box with the same look as the existing toolbars.
export function SearchBox({ value, onChange, placeholder = "Search…", label = "Search" }) {
  return (
    <input
      type="search"
      className="ops-search"
      aria-label={label}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
