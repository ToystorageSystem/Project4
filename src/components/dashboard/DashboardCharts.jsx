import { useState } from "react";

const COLORS = ["#e87532", "#416b63", "#e8b459", "#788bb0", "#b78795", "#8a857d"];
const format = (value) => value.toLocaleString("en-GB");
const valid = (value) => typeof value === "number" && Number.isFinite(value) && value >= 0;

export function groupStatuses(rows = [], field = "status") {
  const groups = new Map();
  for (const row of rows) {
    const label = String(row[field] || "Unknown").replaceAll("_", " ").toLowerCase();
    groups.set(label, (groups.get(label) || 0) + 1);
  }
  return Array.from(groups, ([label, value]) => ({ label, value }));
}

export default function DashboardChart({ title, data = [], loading = false, unavailable = false, type = "bar" }) {
  const [active, setActive] = useState(null);
  const values = data.filter(item => valid(item.value));
  const total = values.reduce((sum, item) => sum + item.value, 0);
  const max = Math.max(1, ...values.map(item => item.value));
  const partial = data.some(item => !valid(item.value));
  const selected = values.find(item => item.label === active);
  let angle = 0;
  const segments = values.map((item, index) => {
    const start = angle;
    angle += total ? item.value / total * 360 : 0;
    return `${COLORS[index % COLORS.length]} ${start}deg ${angle}deg`;
  });
  const empty = unavailable || !data.length || (!partial && total === 0);
  return <section className="analytics-card" aria-label={title} aria-busy={loading}>
    <div className="analytics-card-heading"><h2>{title}</h2><span className="ts-chart-type-label">{type === "donut" ? "Distribution" : "Overview"}</span></div>
    {loading ? <div className="ts-chart-state" role="status"><span className="ts-chart-skeleton" />Loading chart…</div>
      : empty ? <div className="ts-chart-state">{unavailable ? "Data unavailable" : "No data to display"}</div>
      : type === "donut" && partial ? <div className="ts-chart-state">Data unavailable</div>
      : type === "donut" ? <div className="ts-donut-layout">
        <div className="ts-donut-chart" role="img" aria-label={`${title}: ${values.map(i => `${i.label} ${format(i.value)}`).join(", ")}`}
          style={{ background: `conic-gradient(${segments.join(", ")})` }}>
          <div className="ts-donut-center"><strong>{format(selected?.value ?? total)}</strong><span>{selected?.label || "Total"}</span></div>
        </div>
        <ul className="ts-chart-legend">{values.map((item, index) => <li key={item.label}>
          <button type="button" aria-label={`${item.label}: ${format(item.value)} (${Math.round(item.value / total * 100)}%)`} onMouseEnter={() => setActive(item.label)} onMouseLeave={() => setActive(null)} onFocus={() => setActive(item.label)} onBlur={() => setActive(null)}
            onClick={() => setActive(item.label)} aria-pressed={active === item.label}>
            <span className="ts-chart-dot" style={{ background: COLORS[index % COLORS.length] }} /><span className="ts-chart-label">{item.label}</span><strong>{format(item.value)}</strong><small>{Math.round(item.value / total * 100)}%</small>
          </button>
        </li>)}</ul>
      </div> : <div className="ts-bar-chart">{data.map((item, index) => <div className="ts-bar-chart-row" key={item.label}>
        <div className="ts-bar-caption"><span>{item.label}</span><strong>{valid(item.value) ? format(item.value) : "Unavailable"}</strong></div>
        <div className="ts-bar-track" role={valid(item.value) ? "meter" : undefined} aria-label={item.label} aria-valuemin={0} aria-valuemax={max} aria-valuenow={valid(item.value) ? item.value : undefined}>
          <span style={{ width: `${valid(item.value) ? item.value / max * 100 : 0}%`, background: COLORS[index % COLORS.length] }} />
        </div>
      </div>)}</div>}
  </section>;
}
