import Card from "../ui/Card";
export default function ProductInfo( {
    product
}
) {
    if(!product)return null;
    return <Card>
    <div className="space-y-1">
        <h3 className="font-semibold text-slate-900"> {
            product.name||product.productName
        }
    </h3>
    <p className="text-sm text-slate-500">Mã:  {
        product.code||product.productCode||"-"
    }
</p>
<p className="text-sm text-slate-500">Barcode:  {
    product.barcode||"-"
}
</p>
</div>
</Card>
}
