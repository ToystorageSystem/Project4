const base = "inline-flex items-center justify-center gap-2 rounded-xl font-medium transition focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50";
const variants =  {
    primary:"bg-[#f25d19] text-white hover:bg-[#d94f12] focus:ring-[#f25d19]", secondary:"bg-[#fff1e9] text-[#c94810] hover:bg-[#ffe4d5] focus:ring-[#f25d19]", outline:"border border-[#f25d19] text-[#f25d19] hover:bg-[#fff1e9] focus:ring-[#f25d19]", danger:"bg-red-600 text-white hover:bg-red-700 focus:ring-red-500", success:"bg-green-600 text-white hover:bg-green-700 focus:ring-green-500", ghost:"text-slate-700 hover:bg-slate-100 focus:ring-slate-300"
}
;
const sizes= {
    sm:"h-9 px-3 text-sm",md:"h-10 px-4 text-sm",lg:"h-12 px-5 text-base"
}
;
export default function Button( {
    children,variant="primary",size="md",className="",...props
}
) {
    return <button className= {
        `${base} ${variants[variant]} ${sizes[size]} ${className}`
    }
    {
        ...props
    }
    > {
        children
    }
</button>
}
