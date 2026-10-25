export default function MetricCard({ label, value, subtitle }) {
    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-slate-500">{label}</p>
            <p className="mt-3 text-3xl font-bold tracking-tight text-slate-900">
                {value}
            </p>
            {subtitle && <p className="mt-2 text-xs text-slate-500">{subtitle}</p>}
        </div>
    );
}

export function formatCount(value) {
    if (value === null || value === undefined) return "—";
    const number = Number(value);
    return Number.isFinite(number) ? number.toLocaleString("vi-VN") : "—";
}
