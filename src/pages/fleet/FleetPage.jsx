import { useCallback, useEffect, useRef, useState } from "react";
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import api, { errorMessage } from "../../api/api";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import { Table } from "../../components/operations/WorkspaceUI";
import WorkspaceIcon from "../../components/ui/WorkspaceIcon";

const list = value => Array.isArray(value) ? value : [];
const hasPoint = value => Number.isFinite(value?.latitude) && Number.isFinite(value?.longitude);
const km = value => Number.isFinite(value) ? `${value.toFixed(1)} km` : "—";
function FitRoute({ route, vehicle }) {
  const map = useMap();
  useEffect(() => {
    const points = list(route?.visits).filter(hasPoint).map(p => [p.latitude, p.longitude]);
    if (hasPoint(vehicle)) points.unshift([vehicle.latitude, vehicle.longitude]);
    if (points.length > 1) map.fitBounds(points, { padding: [35, 35], maxZoom: 15 });
    else if (points.length) map.setView(points[0], 14);
  }, [map, route, vehicle?.id]);
  return null;
}
export default function FleetPage() {
  const [board, setBoard] = useState(null), [error, setError] = useState(""), [busy, setBusy] = useState(false);
  const [route, setRoute] = useState(null), [vehicle, setVehicle] = useState(null), [suggestions, setSuggestions] = useState(null);
  const [registration, setRegistration] = useState({ plate: "", driverId: "", capacityPackages: "" });
  const [coordinate, setCoordinate] = useState({ latitude: "", longitude: "" });
  const alive = useRef(true), fetching = useRef(false), routeSignature = useRef(null), selectedVehicleId = useRef(null);
  const refresh = useCallback(async () => {
    if (fetching.current) return;
    fetching.current = true;
    try { const { data } = await api.get("/fleet/board"); if (alive.current) setBoard(data); }
    catch (e) { if (alive.current) setError(e?.isAxiosError ? errorMessage(e) : e?.message || errorMessage(e)); }
    finally { fetching.current = false; }
  }, []);
  useEffect(() => { alive.current = true; refresh(); const timer = setInterval(refresh, 5000); return () => { alive.current = false; clearInterval(timer); }; }, [refresh]);
  async function action(operation) {
    if (busy) return;
    setBusy(true); setError("");
    try { await operation(); await refresh(); }
    catch (e) { if (alive.current) setError(e?.isAxiosError ? errorMessage(e) : e?.message || errorMessage(e)); }
    finally { if (alive.current) setBusy(false); }
  }
  async function openRoute(item) {
    await action(async () => { const { data } = await api.get(`/fleet/vehicles/${item.id}/route`); if (alive.current) { selectedVehicleId.current = item.id; routeSignature.current = JSON.stringify(list(item.stops)); setVehicle(item); setRoute(data); } });
  }
  async function recommend(manifest) {
    await action(async () => { const { data } = await api.get(`/fleet/recommendations/${manifest.id}`); if (alive.current) setSuggestions({ manifest, items: list(data) }); });
  }
  useEffect(() => {
    if (selectedVehicleId.current == null || !board) return;
    const current = list(board.vehicles).find(v => v.id === selectedVehicleId.current);
    if (!current) { selectedVehicleId.current = null; setRoute(null); setVehicle(null); return; }
    const signature = JSON.stringify(list(current.stops));
    setVehicle(current);
    if (signature === routeSignature.current) return;
    routeSignature.current = signature;
    api.get(`/fleet/vehicles/${current.id}/route`).then(({ data }) => {
      if (alive.current && selectedVehicleId.current === current.id) setRoute(data);
    }).catch(e => { if (alive.current) { setRoute(null); setError(errorMessage(e)); } });
  }, [board]);
  const vehicles = list(board?.vehicles), sites = list(board?.sites), points = vehicles.filter(hasPoint);
  const ownSite = sites.find(s => s.id === board?.myWarehouseId);
  const center = hasPoint(ownSite) ? [ownSite.latitude, ownSite.longitude] : points.length ? [points[0].latitude, points[0].longitude] : [10.7769, 106.7009];
  return <MainLayout>
    <PageHeader title="Vehicles & routes" actions={<button className="ops-button secondary" onClick={refresh} disabled={busy}><WorkspaceIcon name="refresh" size={16} />Refresh</button>} />
    {error && <div className="ops-alert" role="alert">{error}</div>}
    {!board ? <p role="status">Loading fleet…</p> : <>
      <section className="ops-panel" style={{ overflow: "hidden", marginBottom: 24 }} aria-label="Vehicle map">
        <MapContainer center={center} zoom={12} style={{ height: 380, width: "100%" }}>
          <TileLayer url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>' />
          <FitRoute route={route} vehicle={vehicle} />
          {points.map(v => <CircleMarker key={v.id} center={[v.latitude, v.longitude]} radius={10} pathOptions={{ color: v.stale ? "#777" : "#c4511b", fillOpacity: .85 }}>
            <Popup><strong>{v.plate}</strong><br />{v.driverName}<br />{v.stale ? "Last known / stale location" : "Live vehicle location"}<br />{v.receivedAt && new Date(v.receivedAt).toLocaleString()}<br /><button className="text-link" onClick={() => openRoute(v)}>View route</button></Popup>
          </CircleMarker>)}
          {list(route?.visits).filter(hasPoint).map((p, i) => <CircleMarker key={`${p.deliveryId}-${p.kind}-${i}`} center={[p.latitude, p.longitude]} radius={7} pathOptions={{ color: "#416b63" }}><Popup>{i + 1}. {p.kind} · {p.label}</Popup></CircleMarker>)}
          {list(route?.roadGeometry).length > 1 && <Polyline positions={route.roadGeometry} pathOptions={{ color: "#e87532", weight: 4 }} />}
        </MapContainer>
      </section>
      <section className="ops-panel" style={{ padding: 20, marginBottom: 24 }}>
        <h2>Vehicles</h2>
        <Table head={["Vehicle", "Driver", "New pickups", "Location", "Deliveries", "Route"]}>
          {vehicles.map(v => <tr key={v.id}><td><strong>{v.plate}</strong><br />{v.capacityPackages} packages</td><td>{v.driverName}</td><td>{v.available ? "Available" : "Not accepting"}</td><td>{!v.sharing ? "Sharing off" : v.stale ? "Stale / last known" : "Live"}{v.receivedAt && <><br /><time>{new Date(v.receivedAt).toLocaleTimeString()}</time></>}</td><td>{list(v.stops).length}</td><td><button className="text-link" disabled={busy} onClick={() => openRoute(v)}>View route</button></td></tr>)}
          {!vehicles.length && <tr><td colSpan={6}>No vehicles visible for this account.</td></tr>}
        </Table>
      </section>
      {route && <section className="ops-panel" style={{ padding: 20, marginBottom: 24 }}>
        <h2>Route · {route.plate}</h2><p>{km(route.distanceKm)} · {route.distanceModel === "ROAD_DISTANCE" ? "Road distance" : "Straight-line estimate"}</p>
        <ol style={{ paddingLeft: 24 }}>{list(route.visits).map((p, i) => <li key={i} style={{ padding: "8px 0" }}>{p.kind} · {p.label} · {p.packages} packages</li>)}</ol>
      </section>}
      {board.canDispatch && <section className="ops-panel" style={{ padding: 20, marginBottom: 24 }}>
        <h2>Packed & checked · awaiting vehicle</h2>
        <Table head={["Manifest", "Destination", "Packages", "Assign"]}>{list(board.readyManifests).map(m => <tr key={m.id}><td>{m.manifestCode}</td><td>{m.fromName} → {m.toName}</td><td>{m.packageCount}</td><td><button className="ops-button secondary" disabled={busy} onClick={() => recommend(m)}>Find vehicle</button></td></tr>)}{!list(board.readyManifests).length && <tr><td colSpan={4}>No unassigned checked manifests.</td></tr>}</Table>
      </section>}
      {suggestions && <section className="ops-panel" style={{ padding: 20, marginBottom: 24 }}>
        <h2>Vehicle options · {suggestions.manifest.manifestCode}</h2>
        {!suggestions.items.length && <p>No available vehicle has enough capacity.</p>}
        <Table head={["Vehicle", "Active deliveries", "Peak load", "Added distance", "Position", "Assign"]}>{suggestions.items.map(c => <tr key={c.vehicleId}><td>{c.plate}<br />{c.driverName}</td><td>{c.activeDeliveries}</td><td>{c.peakLoad}/{c.capacityPackages}</td><td>{km(c.extraKm)}<br />{c.distanceModel === "ROAD_DISTANCE" ? "Road distance" : "Straight-line estimate"}</td><td>{c.positionSource === "LIVE_GPS" ? "Live GPS" : "Warehouse base"}</td><td><button className="ops-button" disabled={busy} onClick={() => action(async () => {
          if (!window.confirm(`Assign ${suggestions.manifest.manifestCode} to ${c.plate}?`)) return;
          await api.post(`/fleet/manifests/${suggestions.manifest.id}/assign`, { vehicleId: c.vehicleId }); setSuggestions(null); setRoute(null); selectedVehicleId.current = null;
        })}>Assign</button></td></tr>)}</Table>
      </section>}
      <div className="analytics-grid">
        {board.myWarehouseId != null && <form className="analytics-card" onSubmit={e => { e.preventDefault(); action(async () => {
          const lat = Number(coordinate.latitude), lon = Number(coordinate.longitude);
          if (!coordinate.latitude.trim() || !coordinate.longitude.trim() || !Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) throw Error("Enter valid latitude and longitude.");
          await api.put(`/fleet/sites/${board.myWarehouseId}`, { latitude: lat, longitude: lon });
        }); }}><h2>My location coordinates</h2><p>{ownSite?.name}</p>{["latitude", "longitude"].map(key => <label className="ops-field" key={key}><span>{key === "latitude" ? "Latitude" : "Longitude"}</span><input type="number" step="any" required value={coordinate[key]} onChange={e => setCoordinate({ ...coordinate, [key]: e.target.value })} /></label>)}<button className="ops-button secondary" disabled={busy}>Save location</button></form>}
        {board.canDispatch && <form className="analytics-card" onSubmit={e => { e.preventDefault(); action(async () => {
          await api.post("/fleet/vehicles", { plate: registration.plate.trim(), driverId: Number(registration.driverId), capacityPackages: Number(registration.capacityPackages) }); setRegistration({ plate: "", driverId: "", capacityPackages: "" });
        }); }}><h2>Register vehicle</h2><label className="ops-field"><span>License plate</span><input required maxLength={30} value={registration.plate} onChange={e => setRegistration({ ...registration, plate: e.target.value })} /></label><label className="ops-field"><span>Capacity (packages)</span><input required type="number" min={1} max={10000} value={registration.capacityPackages} onChange={e => setRegistration({ ...registration, capacityPackages: e.target.value })} /></label><label className="ops-field"><span>Driver</span><select required value={registration.driverId} onChange={e => setRegistration({ ...registration, driverId: e.target.value })}><option value="">Choose driver</option>{list(board.drivers).map(d => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label><button className="ops-button" disabled={busy}>Register</button></form>}
      </div>
    </>}
  </MainLayout>;
}
