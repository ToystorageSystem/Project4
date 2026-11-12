export default function Spinner( {
    size=22
}
) {
    return <span style= {
        {
            width:size,height:size
        }
    }
    className="inline-block animate-spin rounded-full border-2 border-slate-200 border-t-[#f25d19]"/>
}
