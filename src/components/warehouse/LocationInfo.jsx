import Card from "../ui/Card";
export default function LocationInfo( {
    title="Vị trí",code,name,address
}
) {
    return <Card>
    <h3 className="font-semibold text-slate-900"> {
        title
    }
</h3>
<div className="mt-2 space-y-1 text-sm text-slate-600"> {
    code&&<div>Mã:  {
        code
    }
</div>
}
{
    name&&<div> {
        name
    }
</div>
}
{
    address&&<div> {
        address
    }
</div>
}
</div>
</Card>
}
