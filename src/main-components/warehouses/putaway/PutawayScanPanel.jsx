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
                    Select a product to begin putaway.
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
                ? "Product barcode verified."
                : "Barcode does not match the selected product."
        );
    };

    const handleLocationScan = (code) => {
        setLocationCode(code);
        setScanSuccess(true);
        setScanMessage(
            `Location scanned: ${code}. The backend will validate this location.`
        );
    };

    const handleSubmit = () => {
        const parsedQuantity =
            Number(quantity);

        if (!productBarcode.trim()) {
            setScanSuccess(false);
            setScanMessage(
                "Please scan the product barcode."
            );
            return;
        }

        if (!locationCode.trim()) {
            setScanSuccess(false);
            setScanMessage(
                "Please scan the location barcode."
            );
            return;
        }

        if (
            !Number.isInteger(parsedQuantity) ||
            parsedQuantity <= 0
        ) {
            setScanSuccess(false);
            setScanMessage(
                "Putaway quantity must be greater than zero."
            );
            return;
        }

        if (
            parsedQuantity >
            (item.remainingQuantity ?? 0)
        ) {
            setScanSuccess(false);
            setScanMessage(
                "Putaway quantity exceeds the remaining quantity."
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
                        Remaining:{" "}
                        {item.remainingQuantity ?? 0}
                    </div>
                </div>

                <div className="mt-4 space-y-4">
                    <BarcodeInput
                        label="Product Barcode"
                        placeholder="Scan Product Barcode"
                        onScan={handleProductScan}
                    />

                    <BarcodeInput
                        label="Location Code"
                        placeholder="Scan location barcode (e.g., A01-03)"
                        onScan={handleLocationScan}
                    />

                    <Input
                        label="Quantity"
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
                            ? "Processing..."
                            : "Confirm Putaway"}
                    </Button>
                </div>
            </Card>

            <LocationInfo
                title="Current Location"
                code={
                    item.fromLocationCode ||
                    "RECEIVING"
                }
            />

            {item.toLocationCode && (
                <LocationInfo
                    title="Destination Location"
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
