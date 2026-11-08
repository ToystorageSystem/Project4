import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getBusinessGoodsReceipts, goodsReceiptErrorMessage } from "../../../api/business/goodsReceipts";
import Card from "../../../components/ui/Card";
import { formatReceiptDate, receiptStatusLabels } from "./receiptText";

// Phieu nhap lien quan den PO, khong thuc hien ghi du lieu.
export default function RelatedGoodsReceipts({ purchaseOrderId }) {
    const [items, setItems] = useState([]);
    const [total, setTotal] = useState(0);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!purchaseOrderId) return;
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getBusinessGoodsReceipts({ purchaseOrderId, size: 100, page: 0 }, controller.signal)
            .then((result) => {
                if (!controller.signal.aborted) {
                    setItems(Array.isArray(result?.items) ? result.items : []);
                    setTotal(result?.totalElements ?? 0);
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted) setError(goodsReceiptErrorMessage(err, "Unable to view linked goods receipt."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [purchaseOrderId]);

    return <Card className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
                <h2 className="text-lg font-semibold text-slate-900">Linked Goods Receipts</h2>
                <p className="mt-1 text-sm text-slate-500">A purchase order may be delivered in multiple batches. Creating a goods receipt does not immediately increase stock.</p>
            </div>
            <Link className="font-semibold text-[#c94810] hover:underline"
                to={`/business/goods-receipts/list?purchaseOrderId=${purchaseOrderId}`}>
                View All ({total})
            </Link>
        </div>
        {loading ? <p className="text-sm text-slate-500">Loading goods receipts...</p>
            : error ? <p role="alert" className="text-sm text-red-700">{error}</p>
                : items.length === 0 ? <p className="text-sm text-slate-500">This purchase order has no goods receipts yet.</p>
                    : <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 text-slate-600"><tr>
                                <th className="p-3">Document No.</th><th className="p-3">Created At</th>
                                <th className="p-3">Expected Quantity</th><th className="p-3">Status</th>
                            </tr></thead>
                            <tbody>{items.map((item) => <tr className="border-t" key={item.id}>
                                <td className="p-3"><Link className="font-semibold text-[#c94810] hover:underline"
                                    to={`/business/goods-receipts/${item.id}`}>{item.receiptCode}</Link></td>
                                <td className="p-3">{formatReceiptDate(item.createdAt)}</td>
                                <td className="p-3">{item.totalExpectedQuantity ?? 0}</td>
                                <td className="p-3">{receiptStatusLabels[item.status] || item.status}</td>
                            </tr>)}</tbody>
                        </table>
                    </div>}
    </Card>;
}
