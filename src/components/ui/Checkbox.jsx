export default function Checkbox( {
    label,className="",...props
}
) {
    return <label className= {
        `inline-flex items-center gap-2 text-sm text-slate-700 ${className}`
    }
    >
    <input type="checkbox" className="h-4 w-4 rounded border-slate-300 accent-[#f25d19]"  {
        ...props
    }
    />
    <span> {
        label
    }
</span>
</label>
}
