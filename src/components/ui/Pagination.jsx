import Button from "./Button";
export default function Pagination( {
    page=1,totalPages=1,onChange
}
) {
    return <div className="flex items-center justify-end gap-2">
    <Button variant="outline" size="sm" disabled= {
        page<=1
    }
    onClick= {
        ()=>onChange?.(page-1)
    }
    >Trước</Button>
    <span className="text-sm text-slate-600"> {
        page
    }
    /  {
        totalPages
    }
</span>
<Button variant="outline" size="sm" disabled= {
    page>=totalPages
}
onClick= {
    ()=>onChange?.(page+1)
}
>Sau</Button>
</div>
}
