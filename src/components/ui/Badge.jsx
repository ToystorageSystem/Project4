const variants= {
    default:"bg-slate-100 text-slate-700",orange:"bg-[#fff1e9] text-[#c94810]",green:"bg-green-100 text-green-700",blue:"bg-blue-100 text-blue-700",yellow:"bg-amber-100 text-amber-700",red:"bg-red-100 text-red-700"
}
;
export default function Badge( {
    children,variant="default",className=""
}
) {
    return <span className= {
        `inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${variants[variant]} ${className}`
    }
    > {
        children
    }
</span>
}
