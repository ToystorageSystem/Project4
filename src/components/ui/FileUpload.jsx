export default function FileUpload( {
    label="Upload File",accept,onChange
}
) {
    return <label className="flex cursor-pointer flex-col items-center justify-center rounded-2xl border border-dashed border-[#f25d19]/40 bg-[#fff8f4] px-5 py-8 text-center transition hover:bg-[#fff1e9]">
    <span className="text-sm font-medium text-[#c94810]"> {
        label
    }
</span>
<span className="mt-1 text-xs text-slate-500">Choose an image or file from your device</span>
<input type="file" accept= {
    accept
}
className="hidden" onChange= {
    e=>onChange?.(e.target.files?.[0]||null)
}
/>
</label>
}
