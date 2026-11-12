import { useEffect, useMemo, useState } from "react";
import ComboboxSelect from "./ComboboxSelect";

// Don vi hanh chinh Viet Nam (2 cap: Tinh/Thanh pho -> Phuong/Xa, ap dung tu 07/2025).
// Nguon: provinces.open-api.vn (public, khong can API key, cho phep CORS).
const API = "https://provinces.open-api.vn/api/v2";

// Cache trong bo nho de mo lai form khong phai tai lai
let provinceCache = null;
const wardCache = new Map();

async function fetchJson(url, signal) {
    const response = await fetch(url, { signal, headers: { Accept: "application/json" } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
}

function loadProvinces(signal) {
    if (provinceCache) return Promise.resolve(provinceCache);
    return fetchJson(`${API}/p/`, signal).then((rows) => {
        provinceCache = (Array.isArray(rows) ? rows : [])
            .map((item) => ({ value: String(item.code), label: item.name }))
            .sort((a, b) => a.label.localeCompare(b.label, "vi"));
        return provinceCache;
    });
}

function loadWards(provinceCode, signal) {
    if (wardCache.has(provinceCode)) return Promise.resolve(wardCache.get(provinceCode));
    return fetchJson(`${API}/p/${encodeURIComponent(provinceCode)}?depth=2`, signal).then((province) => {
        const wards = (Array.isArray(province?.wards) ? province.wards : [])
            .map((item) => ({ value: String(item.code), label: item.name }))
            .sort((a, b) => a.label.localeCompare(b.label, "vi"));
        wardCache.set(provinceCode, wards);
        return wards;
    });
}

export function composeVietnamAddress({ street, wardName, provinceName }) {
    return [street?.trim(), wardName, provinceName].filter(Boolean).join(", ");
}

// Chon dia chi theo Tinh -> Phuong/Xa, chi go tay So nha + Ten duong.
// onChange({ address, street, wardName, provinceName, changed }) moi khi mot phan thay doi.
// changed: "province" | "ward" | "street"
// resetKey: doi gia tri de xoa lua chon (vd: khi mo form moi).
export default function VietnamAddressPicker({ onChange, disabled = false, resetKey }) {
    const [provinces, setProvinces] = useState([]);
    const [wards, setWards] = useState([]);
    const [provinceCode, setProvinceCode] = useState("");
    const [wardCode, setWardCode] = useState("");
    const [street, setStreet] = useState("");
    const [loadingWards, setLoadingWards] = useState(false);
    const [loadError, setLoadError] = useState("");

    useEffect(() => {
        setProvinceCode("");
        setWardCode("");
        setStreet("");
    }, [resetKey]);

    useEffect(() => {
        const controller = new AbortController();
        loadProvinces(controller.signal)
            .then((rows) => { if (!controller.signal.aborted) setProvinces(rows); })
            .catch(() => {
                if (!controller.signal.aborted) setLoadError("Unable to load the Vietnam province list. You can still type the full address below.");
            });
        return () => controller.abort();
    }, []);

    useEffect(() => {
        setWards([]);
        if (!provinceCode) return undefined;
        const controller = new AbortController();
        setLoadingWards(true);
        loadWards(provinceCode, controller.signal)
            .then((rows) => { if (!controller.signal.aborted) setWards(rows); })
            .catch(() => {
                if (!controller.signal.aborted) setLoadError("Unable to load wards for this province. You can still type the full address below.");
            })
            .finally(() => { if (!controller.signal.aborted) setLoadingWards(false); });
        return () => controller.abort();
    }, [provinceCode]);

    const provinceName = useMemo(() => provinces.find((item) => item.value === provinceCode)?.label || "", [provinces, provinceCode]);
    const wardName = useMemo(() => wards.find((item) => item.value === wardCode)?.label || "", [wards, wardCode]);

    const emit = (changed, next) => {
        const parts = { street, wardName, provinceName, ...next };
        onChange?.({ ...parts, changed, address: composeVietnamAddress(parts) });
    };

    return (
        <div className="space-y-3 rounded-xl border border-slate-200 bg-slate-50/60 p-3">
            <div className="grid gap-3 md:grid-cols-2">
                <ComboboxSelect
                    label="Province / City"
                    options={provinces}
                    value={provinceCode}
                    disabled={disabled || !provinces.length}
                    placeholder={provinces.length ? "Type to search province..." : "Loading provinces..."}
                    noMatchText="No matching province."
                    onChange={(value) => {
                        if (value === provinceCode) return;
                        setProvinceCode(value);
                        setWardCode("");
                        setLoadError("");
                        emit("province", { provinceName: provinces.find((item) => item.value === value)?.label || "", wardName: "" });
                    }}
                />
                <ComboboxSelect
                    label="Ward / Commune"
                    options={wards}
                    value={wardCode}
                    disabled={disabled || !provinceCode || loadingWards}
                    placeholder={!provinceCode ? "Select a province first" : loadingWards ? "Loading wards..." : "Type to search ward..."}
                    noMatchText="No matching ward."
                    onChange={(value) => {
                        if (value === wardCode) return;
                        setWardCode(value);
                        emit("ward", { wardName: wards.find((item) => item.value === value)?.label || "" });
                    }}
                />
            </div>
            <label className="block space-y-1.5">
                <span className="block text-sm font-medium text-slate-700">House number &amp; street</span>
                <input
                    type="text"
                    maxLength={120}
                    disabled={disabled}
                    value={street}
                    placeholder="Example: 12 Nguyen Hue"
                    onChange={(event) => {
                        setStreet(event.target.value);
                        emit("street", { street: event.target.value });
                    }}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/20"
                />
            </label>
            {loadError && <p role="alert" className="text-xs text-red-600">{loadError}</p>}
            <p className="text-xs text-slate-500">
                Vietnam administrative units (2 levels, effective 07/2025). Choosing them fills in the full address automatically.
            </p>
        </div>
    );
}
