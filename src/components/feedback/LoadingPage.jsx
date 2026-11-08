import Spinner from "../ui/Spinner";
export default function LoadingPage( {
    text="Loading data..."
}
) {
    return <div className="flex min-h-[260px] items-center justify-center">
    <div className="flex items-center gap-3 text-sm text-slate-500">
        <Spinner/> {
            text
        }
    </div>
</div>
}
