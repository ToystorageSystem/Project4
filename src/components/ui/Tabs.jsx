export default function Tabs( {
    tabs=[],value,onChange
}
) {
    return <div className="flex gap-1 rounded-xl bg-slate-100 p-1"> {
        tabs.map(tab=> {
            const active=value===tab.value;
            return <button key= {
                tab.value
            }
            onClick= {
                ()=>onChange?.(tab.value)
            }
            className= {
                `rounded-lg px-3 py-2 text-sm font-medium transition ${active?"bg-white text-[#f25d19] shadow-sm":"text-slate-600 hover:text-slate-900"}`
            }
            > {
                tab.label
            }
        </button>
    }
    )
}
</div>
}
