export default function EmptyState( {
    title="Không có dữ liệu",description="Hiện chưa có dữ liệu để hiển thị."
}
) {
    return <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
    <h3 className="font-semibold text-slate-800"> {
        title
    }
</h3>
<p className="mt-1 text-sm text-slate-500"> {
    description
}
</p>
</div>
}
