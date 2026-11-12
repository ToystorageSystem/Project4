// Tim dia chi tren ban do (geocoding) cho form Warehouse/Store.
//
// Nguon chinh: Nominatim (OpenStreetMap) - nhanh (~1s), loc duoc chi Viet Nam,
// tra ten tieng Viet va da dung don vi hanh chinh moi (vd: "Phuong Sai Gon").
// Du phong: Photon - chi dung khi Nominatim loi (Photon public thuong rat cham, ~30s).
//
// Chinh sach Nominatim: toi da 1 request/giay, khong dung cho autocomplete.
// => Chi goi khi nguoi dung bam tim / chon Tinh-Phuong / bam vao ban do, va co throttle.

const NOMINATIM = "https://nominatim.openstreetmap.org";
const PHOTON = "https://photon.komoot.io";
const VIETNAM_BBOX = "102.14,8.18,109.47,23.39"; // minLon,minLat,maxLon,maxLat
const REQUEST_TIMEOUT_MS = 10000;
const PHOTON_TIMEOUT_MS = 25000; // Photon public thuong cham hon nhieu
const MIN_NOMINATIM_GAP_MS = 1100;

let lastNominatimAt = 0;

function wait(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

// Giu khoang cach >= 1.1s giua cac request toi Nominatim
async function throttleNominatim() {
    const gap = Date.now() - lastNominatimAt;
    if (gap < MIN_NOMINATIM_GAP_MS) await wait(MIN_NOMINATIM_GAP_MS - gap);
    lastNominatimAt = Date.now();
}

async function fetchJson(url, timeoutMs = REQUEST_TIMEOUT_MS) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
        const response = await fetch(url, { signal: controller.signal, headers: { Accept: "application/json" } });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return await response.json();
    } finally {
        clearTimeout(timer);
    }
}

// Muc zoom phu hop voi loai ket qua (tinh -> nhin rong, so nha -> nhin gan)
function zoomFor(kind) {
    switch (kind) {
        case "country": return 6;
        case "state": case "province": case "city": return 11;
        case "county": case "district": case "town": case "municipality": return 13;
        case "suburb": case "quarter": case "village": case "ward": case "locality": return 14;
        case "neighbourhood": case "residential": return 15;
        case "road": case "street": case "tertiary": case "secondary": case "primary": return 16;
        default: return 17; // so nha, toa nha, dia diem cu the
    }
}

// Ghep dia chi gon theo kieu Viet Nam: "So nha Duong, Phuong/Xa, Tinh/Thanh pho"
function formatVietnamAddress(address = {}, fallback = "") {
    const street = [address.house_number, address.road].filter(Boolean).join(" ");
    const place = address.amenity || address.building || address.office || address.shop || "";
    const ward = address.suburb || address.quarter || address.village || address.town || address.city_district || "";
    const province = address.city || address.state || address.province || "";
    const seen = new Set();
    const text = [street || place, ward, province]
        .filter((part) => {
            const key = String(part || "").trim().toLocaleLowerCase();
            if (!key || seen.has(key)) return false;
            seen.add(key);
            return true;
        })
        .join(", ");
    return text || fallback;
}

function fromNominatim(row) {
    const kind = row.addresstype || row.type;
    return {
        lat: Number(row.lat),
        lng: Number(row.lon),
        label: formatVietnamAddress(row.address, row.display_name),
        detail: row.display_name,
        kind,
        zoom: zoomFor(kind),
        hasHouseNumber: Boolean(row.address?.house_number),
    };
}

function fromPhoton(feature) {
    const p = feature?.properties || {};
    const [lng, lat] = feature?.geometry?.coordinates || [];
    const address = {
        house_number: p.housenumber, road: p.street, amenity: p.name,
        suburb: p.district || p.locality, city: p.city, state: p.state,
    };
    return {
        lat: Number(lat),
        lng: Number(lng),
        label: formatVietnamAddress(address, p.name || ""),
        detail: [p.name, p.street, p.district, p.city, p.state].filter(Boolean).join(", "),
        kind: p.type,
        zoom: zoomFor(p.type),
        hasHouseNumber: Boolean(p.housenumber),
    };
}

const validPoint = (item) => Number.isFinite(item.lat) && Number.isFinite(item.lng) && item.label;

// Bo ket qua trung nhan (vd: 1 con duong bi chia thanh nhieu doan trong OSM)
function uniqueByLabel(items) {
    const seen = new Set();
    return items.filter((item) => {
        const key = item.label.toLocaleLowerCase();
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    });
}

async function searchNominatim(query) {
    await throttleNominatim();
    const params = new URLSearchParams({
        q: query, format: "jsonv2", addressdetails: "1", limit: "5",
        countrycodes: "vn", "accept-language": "vi",
    });
    const rows = await fetchJson(`${NOMINATIM}/search?${params}`);
    return uniqueByLabel((Array.isArray(rows) ? rows : []).map(fromNominatim).filter(validPoint));
}

async function searchPhoton(query) {
    const params = new URLSearchParams({ q: query, limit: "5", bbox: VIETNAM_BBOX });
    const data = await fetchJson(`${PHOTON}/api/?${params}`, PHOTON_TIMEOUT_MS);
    return uniqueByLabel((Array.isArray(data?.features) ? data.features : []).map(fromPhoton).filter(validPoint));
}

// Bo so nha o dau ("12 Nguyen Hue" -> "Nguyen Hue"): OSM o VN it co so nha,
// tim theo ten duong de van ra dung khu vuc.
function withoutHouseNumber(query) {
    return query.replace(/^\s*(so\s*|số\s*)?\d+[\w/-]*\s*,?\s*/iu, "").trim();
}

/**
 * Tim dia chi. queries: chuoi hoac danh sach (thu lan luot, chi tiet -> tong quat).
 * Tra ve { results, usedQuery, exact } - results toi da 5 ket qua de nguoi dung chon.
 */
export async function searchAddress(queries) {
    const list = [];
    for (const query of (Array.isArray(queries) ? queries : [queries])) {
        const text = String(query || "").trim();
        if (!text) continue;
        list.push(text);
        const stripped = withoutHouseNumber(text);
        if (stripped && stripped !== text) list.push(stripped);
    }
    const unique = [...new Set(list)];

    let nominatimFailed = false;
    for (const [index, query] of unique.entries()) {
        try {
            const results = await searchNominatim(query);
            if (results.length) return { results, usedQuery: query, exact: index === 0 };
        } catch {
            nominatimFailed = true;
            break;
        }
    }

    // Du phong Photon khi Nominatim khong truy cap duoc
    if (nominatimFailed) {
        for (const [index, query] of unique.entries()) {
            const results = await searchPhoton(query);
            if (results.length) return { results, usedQuery: query, exact: index === 0 };
        }
    }
    return { results: [], usedQuery: unique[0] || "", exact: false };
}

/** Lay dia chi tai 1 diem tren ban do. Tra ve { label, hasHouseNumber } hoac null. */
export async function reverseGeocode(lat, lng) {
    try {
        await throttleNominatim();
        const params = new URLSearchParams({
            lat: String(lat), lon: String(lng), format: "jsonv2",
            addressdetails: "1", zoom: "18", "accept-language": "vi",
        });
        const row = await fetchJson(`${NOMINATIM}/reverse?${params}`);
        if (row && !row.error) {
            const item = fromNominatim(row);
            return item.label ? item : null;
        }
        return null;
    } catch {
        const params = new URLSearchParams({ lat: String(lat), lon: String(lng), limit: "1" });
        const data = await fetchJson(`${PHOTON}/reverse?${params}`, PHOTON_TIMEOUT_MS);
        const feature = data?.features?.[0];
        return feature ? fromPhoton(feature) : null;
    }
}
