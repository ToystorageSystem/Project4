import { useEffect, useState } from "react";

import BarcodeInput from "../../../components/scanner/BarcodeInput";
import LocationInfo from "../../../components/warehouse/LocationInfo";
import ScanResult from "../../../components/scanner/ScanResult";

import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Input from "../../../components/ui/Input";

export default function PutawayScanPanel({
    item,
    submitting = false,
    onSubmit,
}) {
    const [productBarcode, setProductBarcode] =
        useState("");
    const [locationCode, setLocationCode] =
        useState("");
    const [quantity, setQuantity] =
        useState("");

    const [scanMessage, setScanMessage] =
        useState("");
    const [scanSuccess, setScanSuccess] =
        useState(true);

    useEffect(() => {
        setProductBarcode("");
        setLocationCode(
            item?.toLocationCode || ""
        );
        setQuantity(
            item?.remainingQuantity > 0
                ? String(item.remainingQuantity)
                : ""
        );
        setScanMessage("");
        setScanSuccess(true);
    }, [item?.itemId]);

    if (!item) {
        return (
            <Card>
                <h2 className="font-semibold text-slate-900">
                    Scan Putaway
                </h2>

                <p className="mt-3 text-sm text-slate-500">
                    Chọn một sản phẩm để bắt đầu cất hàng.
                </p>
            </Card>
        );
    }

    const handleProductScan = (code) => {
        setProductBarcode(code);

        const matched =
            code === item.barcode;

        setScanSuccess(matched);
        setScanMessage(
            matched
                ? "Barcode sản phẩm hợp lệ."
                : "Barcode không khớp sản phẩm đã chọn."
        );
    };

    const handleLocationScan = (code) => {
        setLocationCode(code);
        setScanSuccess(true);
        setScanMessage(
            `Đã nhận vị trí ${code}. Backend sẽ kiểm tra vị trí này.`
        );
    };

    const handleSubmit = () => {
        const parsedQuantity =
            Number(quantity);

        if (!productBarcode.trim()) {
            setScanSuccess(false);
            setScanMessage(
                "Vui lòng scan barcode sản phẩm."
            );
            return;
        }

        if (!locationCode.trim()) {
            setScanSuccess(false);
            setScanMessage(
                "Vui lòng scan mã vị trí."
            );
            return;
        }

        if (
            !Number.isInteger(parsedQuantity) ||
            parsedQuantity <= 0
        ) {
            setScanSuccess(false);
            setScanMessage(
                "Số lượng Putaway phải lớn hơn 0."
            );
            return;
        }

        if (
            parsedQuantity >
            (item.remainingQuantity ?? 0)
        ) {
            setScanSuccess(false);
            setScanMessage(
                "Số lượng Putaway vượt quá số lượng còn lại."
            );
            return;
        }

        onSubmit?.({
            productBarcode:
                productBarcode.trim(),
            locationCode:
                locationCode.trim(),
            quantity: parsedQuantity,
        });
    };

    return (
        <div className="space-y-4">
            <Card>
                <h2 className="font-semibold text-slate-900">
                    Scan Putaway
                </h2>

                <div className="mt-4 rounded-xl bg-slate-50 p-4">
                    <div className="font-medium text-slate-900">
                        {item.productName}
                    </div>

                    <div className="mt-1 text-sm text-slate-500">
                        Còn lại:{" "}
                        {item.remainingQuantity ?? 0}
                    </div>
                </div>

                <div className="mt-4 space-y-4">
                    <BarcodeInput
                        label="Product Barcode"
                        placeholder="Quét barcode sản phẩm"
                        onScan={handleProductScan}
                    />

                    <BarcodeInput
                        label="Location Code"
                        placeholder="Quét mã vị trí, ví dụ A01-03"
                        onScan={handleLocationScan}
                    />

                    <Input
                        label="Số lượng"
                        type="number"
                        min="1"
                        max={
                            item.remainingQuantity ?? 0
                        }
                        value={quantity}
                        onChange={(event) =>
                            setQuantity(
                                event.target.value
                            )
                        }
                    />

                    <ScanResult
                        success={scanSuccess}
                        message={scanMessage}
                    />

                    <Button
                        className="w-full"
                        disabled={submitting}
                        onClick={handleSubmit}
                    >
                        {submitting
                            ? "Đang xử lý..."
                            : "Xác nhận Putaway"}
                    </Button>
                </div>
            </Card>

            <LocationInfo
                title="Vị trí hiện tại"
                code={
                    item.fromLocationCode ||
                    "RECEIVING"
                }
            />

            {item.toLocationCode && (
                <LocationInfo
                    title="Vị trí đích"
                    code={item.toLocationCode}
                    name={
                        [
                            item.zone
                                ? `Zone ${item.zone}`
                                : null,
                            item.shelf
                                ? `Shelf ${item.shelf}`
                                : null,
                        ]
                            .filter(Boolean)
                            .join(" • ") || null
                    }
                />
            )}
        </div>
    );
}
