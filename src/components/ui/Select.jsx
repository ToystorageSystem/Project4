export default function Select( {
    label,options=[],error,className="",...props
}
) {
    return <label className="block space-y-1.5"> {
        label&&<span className="text-sm font-medium text-slate-700"> {
            label
        }
    </span>
}
<select className= {
    `w-full rounded-xl border bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/20 ${error?"border-red-500":"border-slate-300"} ${className}`
}
{
    ...props
}
> {
    options.map(i=>
    <option key= {
        i.value
    }
    value= {
        i.value
    }
    > {
        i.label
    }
</option>)
}
</select> {
    error&&<span className="text-xs text-red-600"> {
        error
    }
</span>
}
</label>
}
