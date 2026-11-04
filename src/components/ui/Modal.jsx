export default function Modal( {
    open,title,children,footer,onClose
}
) {
    if(!open)return null;
    return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
    <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
            <h3 className="font-semibold text-slate-900"> {
                title
            }
        </h3>
        <button onClick= {
            onClose
        }
        className="rounded-lg px-2 py-1 text-slate-500 hover:bg-slate-100">×</button>
    </div>
    <div className="px-5 py-4"> {
        children
    }
</div> {
    footer&&<div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4"> {
        footer
    }
</div>
}
</div>
</div>
}
