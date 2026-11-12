export default function Table( {
    columns=[],data=[],rowKey="id"
}
) {
    return <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
    <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-[#fff8f4]">
            <tr> {
                columns.map(col=>
                <th key= {
                    col.key
                }
                className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-600"> {
                    col.label
                }
            </th>)
        }
    </tr>
</thead>
<tbody className="divide-y divide-slate-100"> {
    data.map(row=>
    <tr key= {
        row[rowKey]
    }
    className="hover:bg-slate-50"> {
        columns.map(col=>
        <td key= {
            col.key
        }
        className="px-4 py-3 text-sm text-slate-700"> {
            col.render?col.render(row[col.key],row):row[col.key]
        }
    </td>)
}
</tr>)
}
</tbody>
</table>
</div>
}
