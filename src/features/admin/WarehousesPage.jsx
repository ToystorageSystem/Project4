
import { isAdmin } from "../../auth/access";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  AlertTriangle, Building2, CheckCircle2, ClipboardList, Database,
  Eye, MapPin, Pencil, Plus, Power, RefreshCw, Search, Store,
  UserMinus, Users, Warehouse, X,
} from "lucide-react";
import {
  CircleMarker, MapContainer, TileLayer, useMap, useMapEvents,
} from "react-leaflet";

import "leaflet/dist/leaflet.css";
import { getAdminUsers } from "./api/admin/userAccountsApi";
import {
  activateWarehouse,
  assignWarehouseEmployee,
  assignWarehouseManager,
  createWarehouse,
  deactivateWarehouse,
  getWarehouseDeactivationImpact,
  getWarehouseEmployees,
  getWarehouseSession,
  getWarehouses,
  unassignWarehouseEmployee,
  updateWarehouse,
} from "./api/admin/warehousesApi";
import "./css/WarehousesPage.css";
import VietnamAddressPicker from "../../components/ui/VietnamAddressPicker";
import { reverseGeocode, searchAddress } from "./utils/geocoding";

// ======================================================
// HELPERS
// ======================================================

// DB luu "WAREHOUSE_MANAGER"/"WAREHOUSE_STAFF", code dung "WH_MANAGER"/"WH_STAFF"
// => quy ve cung mot ten (giong normalizeRole o WarehouseAssignmentService).
function normalizeRole(value) {
  return String(value ?? "")
    .trim()
    .toUpperCase()
    .replace(/^ROLE[-_]/, "")
    .replaceAll("-", "_")
    .replace(/^WAREHOUSE_/, "WH_");
}

function messageFor(error) {
  if (error?.status === 401) {
    return "Session expired. Please sign in again.";
  }
  if (error?.status === 403) {
    return "Access denied. Please check your Admin permissions.";
  }
  // Hi\u1ec7n \u0111\u00fang message backend tr\u1ea3 v\u1ec1 (tr\u01b0\u1edbc \u0111\u00e2y message ti\u1ebfng Vi\u1ec7t b\u1ecb thay b\u1eb1ng c\u00e2u chung chung)
  return error?.message || "An unexpected error occurred.";
}

function mapLocation(item) {
  return {
    id: item.id,
    code: item.warehousesCode || "",
    name: item.name || "",
    type: item.type,
    address: item.address || "",
    phone: item.phone || "",
    status: item.status,
    employees: [],
    employeesLoaded: false,
  };
}

function mapEmployee(item) {
  return {
    id: item.userId,
    code: item.userCode || "",
    name: item.name || "",
    email: item.email || "",
    status: item.status,
    warehouseId: item.warehouseId,
    roles: Array.isArray(item.roles)
      ? item.roles.map(normalizeRole)
      : [],
  };
}

function managerRole(location) {
  return location?.type === "MAIN_WAREHOUSE"
    ? "WH_MANAGER"
    : "STORE_MANAGER";
}

function staffRole(location) {
  return location?.type === "MAIN_WAREHOUSE"
    ? "WH_STAFF"
    : "STORE_STAFF";
}

function managerOf(location) {
  return location?.employees.find((user) =>
    user.roles.includes(managerRole(location))
  );
}

function StatusBadge({ status }) {
  return (
    <span className={`wh-status ${String(status || "").toLowerCase()}`}>
      {status}
    </span>
  );
}

function InfoRow({ label, children }) {
  return (
    <div className="wh-info-row">
      <span>{label}</span>
      <strong>{children ?? "Unavailable"}</strong>
    </div>
  );
}

// ======================================================
// MAP - KEEP WORKING TILE PROVIDER
// ======================================================

const DEFAULT_MAP_CENTER = [10.8231, 106.6297];

function MapClickListener({ onSelect }) {
  useMapEvents({
    click(event) {
      onSelect(event.latlng);
    },
  });
  return null;
}

function MapRecenter({ point }) {
  const map = useMap();

  useEffect(() => {
    if (point) {
      map.setView([point.lat, point.lng], point.zoom ?? map.getZoom());
    }
  }, [map, point]);

  return null;
}

function AddressPickerMap({ point, onSelect }) {
  return (
    <div
      style={{
        borderRadius: 10,
        overflow: "hidden",
        border: "1px solid #dfe6f1",
      }}
    >
      <MapContainer
        center={DEFAULT_MAP_CENTER}
        zoom={12}
        scrollWheelZoom={false}
        style={{ height: 255, width: "100%" }}
      >
        <TileLayer
          url="https://tile.openstreetmap.de/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />
        <MapClickListener onSelect={onSelect} />
        <MapRecenter point={point} />

        {point && (
          <CircleMarker
            center={[point.lat, point.lng]}
            radius={9}
            pathOptions={{
              color: "#285bd4",
              fillColor: "#4e7cf1",
              fillOpacity: 0.75,
            }}
          />
        )}
      </MapContainer>
    </div>
  );
}

// ======================================================
// MAIN COMPONENT
// ======================================================

export default function WarehousesPage() {
  const [locations, setLocations] = useState([]);
  const [connected, setConnected] = useState(false);
  const [canManage, setCanManage] = useState(false);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [detailWarning, setDetailWarning] = useState("");
  const [notice, setNotice] = useState(null);

  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [dialog, setDialog] = useState(null);

  const [form, setForm] = useState({
    name: "",
    type: "STORE",
    address: "",
    phone: "",
  });
  const [formError, setFormError] = useState("");

  const [candidates, setCandidates] = useState([]);
  const [candidateError, setCandidateError] = useState("");
  const [candidateLoading, setCandidateLoading] = useState(false);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState("");
  const [selectedManagerId, setSelectedManagerId] = useState("");

  const [mapPoint, setMapPoint] = useState(null);
  const [mapBusy, setMapBusy] = useState(false);
  const [mapMessage, setMapMessage] = useState("");
  const [mapError, setMapError] = useState("");
  const [mapResults, setMapResults] = useState([]);
  const mapRequestId = useRef(0);
  // Doi gia tri de reset bo chon Tinh/Phuong moi lan mo form
  const [addressPickerKey, setAddressPickerKey] = useState(0);

  const [impact, setImpact] = useState(null);
  const [impactLoading, setImpactLoading] = useState(false);

  // ====================================================
  // LOAD WAREHOUSES
  // ====================================================

  const loadLocations = useCallback(async () => {
    setLoading(true);
    setError("");
    setDetailWarning("");

    try {
      const [first, session] = await Promise.all([
        getWarehouses(0, 100),
        getWarehouseSession(),
      ]);

      const authorities = Array.isArray(session?.authorities)
        ? session.authorities
        : [];

      // Tạm tắt check WAREHOUSE_MANAGE: chỉ cần là admin
      setCanManage(isAdmin(session));
      // setCanManage(
      //   isAdmin(session) && authorities.includes("WAREHOUSE_MANAGE")
      // );

      if (!Array.isArray(first?.content)) {
        throw new Error("Unexpected warehouse list response.");
      }

      const pages = Number(first.totalPages || 1);
      if (pages > 20) {
        throw new Error("Too many warehouses or stores. A paginated view is required.");
      }

      const all = [...first.content];

      for (let page = 1; page < pages; page += 1) {
        const next = await getWarehouses(page, 100);

        if (!Array.isArray(next?.content)) {
          throw new Error("A warehouse page could not be loaded.");
        }

        all.push(...next.content);
      }

      const loaded = await Promise.all(
        all.map(async (raw) => {
          const location = mapLocation(raw);

          try {
            const employees = await getWarehouseEmployees(location.id);

            if (!Array.isArray(employees)) {
              throw new Error("Invalid employee response.");
            }

            return {
              ...location,
              employees: employees.map(mapEmployee),
              employeesLoaded: true,
            };
          } catch {
            return location;
          }
        })
      );

      setLocations(loaded);
      setConnected(true);

      if (loaded.some((item) => !item.employeesLoaded)) {
        setDetailWarning(
          "Some employee lists are unavailable. Assignment actions are disabled for those warehouses or stores."
        );
      }

      return true;
    } catch (err) {
      setError(messageFor(err));
      setConnected(false);
      setLocations([]);
      setCanManage(false);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadLocations();
  }, [loadLocations]);

  // ====================================================
  // FILTER
  // ====================================================

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();

    return locations.filter((item) => {
      const searchable = [
        item.code,
        item.name,
        item.address,
        item.phone,
        item.type,
      ].join(" ").toLowerCase();

      return (
        searchable.includes(query) &&
        (typeFilter === "ALL" || item.type === typeFilter) &&
        (statusFilter === "ALL" || item.status === statusFilter)
      );
    });
  }, [locations, search, typeFilter, statusFilter]);

  const selected = locations.find(
    (item) => item.id === dialog?.warehouseId
  );

  const availableEmployees = candidates.filter((item) =>
    item.roles.includes(staffRole(selected))
  );

  const availableManagers = candidates.filter((item) =>
    item.roles.includes(managerRole(selected))
  );

  // ====================================================
  // DIALOG
  // ====================================================

  function closeDialog() {
    if (busy) return;

    setDialog(null);
    setFormError("");
    setImpact(null);
    setSelectedEmployeeId("");
    setSelectedManagerId("");
    setMapBusy(false);
    mapRequestId.current += 1;
  }

  function openCreate() {
    setForm({
      name: "",
      type: "STORE",
      address: "",
      phone: "",
    });

    setFormError("");
    setMapPoint(null);
    setMapBusy(false);
    setMapMessage("");
    setMapError("");
    setMapResults([]);
    setAddressPickerKey((key) => key + 1);
    setDialog({ type: "create" });
  }

  function openEdit(item) {
    setForm({
      name: item.name,
      type: item.type,
      address: item.address,
      phone: item.phone,
    });

    setFormError("");
    setCandidateError("");
    setSelectedEmployeeId("");
    setSelectedManagerId("");
    setMapPoint(null);
    setMapBusy(false);
    setMapMessage("");
    setMapError("");
    setMapResults([]);
    setAddressPickerKey((key) => key + 1);

    setDialog({
      type: "edit",
      warehouseId: item.id,
    });

    // Hien vi tri dia chi dang luu tren ban do
    if (item.address) {
      void locateAddress(item.address);
    }

    void readCandidates();
  }

  function openDialog(type, location) {
    setFormError("");
    setDialog({
      type,
      warehouseId: location.id,
    });

    if (type === "impact") {
      void readImpact(location.id);
    }
  }

  // ====================================================
  // DEACTIVATION IMPACT
  // ====================================================

  async function readImpact(id) {
    setImpact(null);
    setImpactLoading(true);
    setFormError("");

    try {
      const data = await getWarehouseDeactivationImpact(id);

      if (
        typeof data?.canDeactivate !== "boolean" ||
        typeof data?.totalBlockingRecords !== "number"
      ) {
        throw new Error("Unexpected deactivation impact response.");
      }

      setImpact(data);
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setImpactLoading(false);
    }
  }

  // ====================================================
  // LOAD EMPLOYEE CANDIDATES
  // ====================================================

  async function readCandidates() {
    setCandidateLoading(true);
    setCandidateError("");
    setCandidates([]);

    try {
      const first = await getAdminUsers(0, 100);

      if (!Array.isArray(first?.content)) {
        throw new Error("Unexpected user list response.");
      }

      const pages = Number(first.totalPages || 1);

      if (pages > 20) {
        throw new Error("Too many users. Please use a filtered employee search.");
      }

      const users = [...first.content];

      for (let page = 1; page < pages; page += 1) {
        const next = await getAdminUsers(page, 100);

        if (!Array.isArray(next?.content)) {
          throw new Error("A user page could not be loaded.");
        }

        users.push(...next.content);
      }

      setCandidates(
        users
          .filter(
            (user) =>
              user.status === "ACTIVE" &&
              user.warehouseId == null
          )
          .map((user) => ({
            id: user.id,
            name: user.name || "",
            email: user.email || "",
            roles: Array.isArray(user.roles)
              ? user.roles.map((role) =>
                  normalizeRole(role.roleCode)
                )
              : [],
          }))
      );
    } catch (err) {
      setCandidateError(
        err?.status === 403
          ? "USER_READ permission is required to list available users."
          : messageFor(err)
      );
    } finally {
      setCandidateLoading(false);
    }
  }

  // ====================================================
  // MAP ADDRESS SEARCH (Nominatim, du phong Photon)
  // ====================================================

  // queries: chuoi hoac danh sach chuoi (thu lan luot tu chi tiet den tong quat)
  async function locateAddress(queries = form.address) {
    const candidates = (Array.isArray(queries) ? queries : [queries])
      .map((value) => String(value || "").trim())
      .filter(Boolean);

    if (!candidates.length) {
      setMapError("Enter an address before searching on the map.");
      return;
    }

    const requestId = ++mapRequestId.current;

    setMapBusy(true);
    setMapError("");
    setMapMessage("");
    setMapResults([]);

    try {
      const { results, exact } = await searchAddress(candidates);

      if (requestId !== mapRequestId.current) {
        return;
      }

      if (!results.length) {
        setMapError(
          "Address not found on the map. Check the street and ward, or click the correct point on the map."
        );
        return;
      }

      const best = results[0];
      setMapPoint({ lat: best.lat, lng: best.lng, zoom: best.zoom });
      // Nhieu ket qua thi cho nguoi dung chon lai (vd: cung ten duong o 2 phuong)
      setMapResults(results.length > 1 ? results : []);

      if (exact && best.hasHouseNumber) {
        setMapMessage(`Found: ${best.label}. Check the marker before saving.`);
      } else if (exact) {
        setMapMessage(
          `Found the area: ${best.label}. Click the exact building on the map if needed.`
        );
      } else {
        setMapMessage(
          `Exact address not on the map; showing the closest area: ${best.label}. Click the exact point on the map.`
        );
      }
    } catch (error) {
      if (requestId === mapRequestId.current) {
        console.error("Map address search:", error);
        setMapError(
          "Map search is unavailable right now. You can still save the typed address."
        );
      }
    } finally {
      if (requestId === mapRequestId.current) {
        setMapBusy(false);
      }
    }
  }

  // Chon 1 ket qua khac trong danh sach goi y
  function pickMapResult(result) {
    mapRequestId.current += 1;
    setMapBusy(false);
    setMapPoint({ lat: result.lat, lng: result.lng, zoom: result.zoom });
    setMapResults([]);
    setMapError("");
    setMapMessage(`Selected on map: ${result.label}.`);
  }

  // ====================================================
  // REVERSE ADDRESS LOOKUP (bam vao ban do)
  // ====================================================

  async function chooseMapPoint(point) {
    const lat = Number(point.lat);
    const lng = Number(point.lng);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
      return;
    }

    const requestId = ++mapRequestId.current;

    // Giu nguyen muc zoom hien tai khi bam vao ban do
    setMapPoint({ lat, lng, zoom: null });
    setMapBusy(true);
    setMapError("");
    setMapMessage("");
    setMapResults([]);

    try {
      const place = await reverseGeocode(lat, lng);

      if (requestId !== mapRequestId.current) {
        return;
      }

      if (!place?.label) {
        setMapError(
          "No address found near this point. You can enter it manually."
        );
        return;
      }

      setForm((previous) => ({
        ...previous,
        address: place.label.slice(0, 255),
      }));

      setMapMessage(
        place.hasHouseNumber
          ? "Address filled from the map. Verify it before saving."
          : "Street and ward filled from the map. Add the house number to the Full Address."
      );
    } catch (error) {
      if (requestId === mapRequestId.current) {
        console.error("Map reverse lookup:", error);

        setMapError(
          "Unable to read an address for this point. The marker is kept; enter the address manually."
        );
      }
    } finally {
      if (requestId === mapRequestId.current) {
        setMapBusy(false);
      }
    }
  }

  // ====================================================
  // SINGLE SAVE CHANGES
  // ====================================================

  async function saveLocation(event) {
    event.preventDefault();

    if (busy || !canManage || !dialog) {
      return;
    }

    const creating = dialog.type === "create";
    const editing = dialog.type === "edit";

    if (!creating && !editing) {
      return;
    }

    const name = form.name.trim();
    const address = form.address.trim();
    const phone = form.phone.trim();

    const detailsChanged =
      editing &&
      selected &&
      (
        name !== selected.name ||
        address !== selected.address ||
        phone !== selected.phone
      );

    const managerId = selectedManagerId
      ? Number(selectedManagerId)
      : null;

    const employeeId = selectedEmployeeId
      ? Number(selectedEmployeeId)
      : null;

    const hasManagerChoice = managerId !== null;
    const hasEmployeeChoice = employeeId !== null;

    if (
      editing &&
      !detailsChanged &&
      !hasManagerChoice &&
      !hasEmployeeChoice
    ) {
      setDialog(null);
      return;
    }

    if (creating || detailsChanged) {
      if (name.length < 3 || name.length > 150) {
        setFormError(
          "Warehouse / store name must contain 3-150 characters."
        );
        return;
      }

      if (address.length < 5 || address.length > 255) {
        setFormError(
          "Address must contain 5-255 characters."
        );
        return;
      }

      if (!/^\+?[0-9]{9,15}$/.test(phone)) {
        setFormError(
          "Phone must contain 9-15 digits, optionally starting with +."
        );
        return;
      }

      const duplicate = locations.some(
        (item) =>
          item.name.toLowerCase() === name.toLowerCase() &&
          item.id !== selected?.id
      );

      if (duplicate) {
        setFormError(
          "This warehouse / store name already exists."
        );
        return;
      }
    }

    if (editing && (hasManagerChoice || hasEmployeeChoice)) {
      if (
        !selected ||
        !selected.employeesLoaded ||
        selected.status !== "ACTIVE" ||
        candidateLoading ||
        candidateError
      ) {
        setFormError(
          "Employee assignments are unavailable. Refresh and try again."
        );
        return;
      }

      if (
        hasManagerChoice &&
        (
          !Number.isSafeInteger(managerId) ||
          !availableManagers.some(
            (item) => Number(item.id) === managerId
          )
        )
      ) {
        setFormError(
          "Select an eligible, unassigned manager."
        );
        return;
      }

      if (
        hasEmployeeChoice &&
        (
          !Number.isSafeInteger(employeeId) ||
          !availableEmployees.some(
            (item) => Number(item.id) === employeeId
          )
        )
      ) {
        setFormError(
          "Select an eligible, unassigned employee."
        );
        return;
      }

      if (
        hasManagerChoice &&
        hasEmployeeChoice &&
        managerId === employeeId
      ) {
        setFormError(
          "Select different users for manager and employee."
        );
        return;
      }
    }

    setBusy(true);
    setFormError("");

    let savedSteps = 0;

    try {
      if (creating) {
        await createWarehouse({
          name,
          type: form.type,
          address,
          phone,
        });

        savedSteps += 1;
      } else {
        if (detailsChanged) {
          await updateWarehouse(selected.id, {
            name,
            address,
            phone,
          });

          savedSteps += 1;
        }

        if (hasManagerChoice) {
          await assignWarehouseManager(
            selected.id,
            managerId
          );

          savedSteps += 1;
          setSelectedManagerId("");
        }

        if (hasEmployeeChoice) {
          await assignWarehouseEmployee(
            selected.id,
            employeeId
          );

          savedSteps += 1;
          setSelectedEmployeeId("");
        }
      }

      setDialog(null);

      setNotice({
        type: "success",
        message: creating
          ? "Warehouse / store created."
          : "Changes saved successfully.",
      });

      await loadLocations();
    } catch (err) {
      setFormError(
        savedSteps > 0
          ? `${messageFor(err)} Some changes were saved. Review the data before retrying.`
          : messageFor(err)
      );

      if (savedSteps > 0) {
        await loadLocations();
        await readCandidates();
      }
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // ACTIVATE / DEACTIVATE
  // ====================================================

  async function changeStatus() {
    if (!selected || !canManage || busy) {
      return;
    }

    if (
      selected.status === "ACTIVE" &&
      (impactLoading || !impact || !impact.canDeactivate)
    ) {
      setFormError(
        "Deactivation is blocked or impact results are unavailable."
      );
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      if (selected.status === "ACTIVE") {
        await deactivateWarehouse(selected.id);
      } else {
        await activateWarehouse(selected.id);
      }

      setDialog(null);

      setNotice({
        type: "success",
        message: "Warehouse / store status updated.",
      });

      await loadLocations();
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // REMOVE EMPLOYEE
  // ====================================================

  async function removeTeamMember(user) {
    if (
      dialog?.type !== "edit" ||
      !selected ||
      !canManage ||
      busy ||
      !selected.employeesLoaded
    ) {
      return;
    }

    if (user.roles.includes(managerRole(selected))) {
      setFormError(
        "Assign a replacement manager before removing the current manager."
      );
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      await unassignWarehouseEmployee(selected.id, user.id);

      setNotice({
        type: "success",
        message: "Employee unassigned.",
      });

      await loadLocations();
      await readCandidates();
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setBusy(false);
    }
  }

  const modalTitle = {
    create: "Create Warehouse / Store",
    edit: "Edit Warehouse / Store",
    details: "Warehouse / Store Details",
    impact: "Deactivation Impact",
    activate: "Activate Warehouse / Store",
  }[dialog?.type];

  // ====================================================
  // PAGE
  // ====================================================

  return (
    <div className="page-stack wh-page">
      {/* HEADER */}
      <div className="page-heading wh-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / ISSUE #25
          </span>
          <h1>Warehouses &amp; Stores</h1>
          <p>
            Manage warehouses, stores, operations and employee assignments.
          </p>
        </div>

        <div className="wh-header-actions">
          <span className="demo-chip">ACCOUNT DATA</span>
          <button
            type="button"
            className="wh-button wh-button-primary"
            onClick={openCreate}
            disabled={!connected || !canManage || loading || busy}
          >
            <Plus size={17} />
            Create Warehouse / Store
          </button>
        </div>
      </div>

      {/* BACKEND CONNECTION */}
      <section className="panel wh-table-panel">
        <div className="wh-section-header">
          <div>
            <h2>Account access</h2>
            <p>
              {loading
                ? "Loading warehouses and stores..."
                : connected
                  ? "Warehouse and store data is available"
                  : error
                    ? "Unable to load warehouses and stores. See details below."
                    : "Sign in to read live data"}
            </p>
          </div>

          <div className="wh-header-actions">
            {/* <span className="demo-chip">
              {canManage
                ? "WAREHOUSE_MANAGE ENABLED"
                : "READ ONLY / WAREHOUSE_MANAGE NOT GRANTED"}
            </span> */}
          </div>
        </div>

        {error && (
          <div className="wh-modal-body">
            <p className="wh-form-error" role="alert">
              {error}
            </p>
          </div>
        )}

        {detailWarning && (
          <div className="wh-modal-body">
            <p className="wh-form-error">
              {detailWarning}
            </p>
          </div>
        )}
      </section>

      {/* STATISTICS */}
      <div className="wh-stat-grid">
        {[
          ["Total Warehouses / Stores", locations.length, Building2],
          [
            "Active Warehouses / Stores",
            locations.filter((item) => item.status === "ACTIVE").length,
            CheckCircle2,
          ],
          [
            "Main Warehouses",
            locations.filter((item) => item.type === "MAIN_WAREHOUSE").length,
            Warehouse,
          ],
          [
            "Stores",
            locations.filter((item) => item.type === "STORE").length,
            Store,
          ],
        ].map(([label, value, Icon]) => (
          <div className="panel wh-stat" key={label}>
            <span className="wh-stat-icon">
              <Icon size={19} />
            </span>
            <span>{label}</span>
            <strong>{value}</strong>
            <small>Current records</small>
          </div>
        ))}
      </div>

      {/* NOTIFICATION */}
      {notice && (
        <div className={`wh-notice ${notice.type}`} role="status">
          {notice.type === "success" ? (
            <CheckCircle2 size={18} />
          ) : (
            <AlertTriangle size={18} />
          )}
          <span>{notice.message}</span>
          <button
            type="button"
            aria-label="Close notification"
            onClick={() => setNotice(null)}
          >
            <X size={17} />
          </button>
        </div>
      )}

      {/* WAREHOUSE / STORE MANAGEMENT */}
      <section className="panel wh-table-panel">
        <div className="wh-section-header">
          <div>
            <h2>Warehouse / Store Management</h2>
            <p>
              {filtered.length} of {locations.length} warehouses and stores shown
            </p>
          </div>

          <div className="wh-header-actions">
            <button
              type="button"
              className="wh-button wh-button-secondary"
              onClick={() => void loadLocations()}
              disabled={loading || busy}
            >
              <RefreshCw size={15} />
              Refresh
            </button>
            <span className="demo-chip">ACCOUNT DATA</span>
          </div>
        </div>

        {/* FILTERS */}
        <div className="wh-toolbar">
          <label className="wh-search">
            <Search size={17} />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search warehouses or stores..."
            />
          </label>

          <select
            aria-label="Filter by type"
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
          >
            <option value="ALL">All types</option>
            <option value="MAIN_WAREHOUSE">Main Warehouse</option>
            <option value="STORE">Store</option>
          </select>

          <select
            aria-label="Filter by status"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>

        {/* TABLE */}
        <div className="wh-table-scroll">
          <table className="wh-table">
            <thead>
              <tr>
                <th>Warehouse / Store Code</th>
                <th>Warehouse / Store Name</th>
                <th>Type</th>
                <th>Manager</th>
                <th>Employees</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="wh-empty">
                    {loading
                      ? "Loading warehouses and stores..."
                      : "No matching warehouses or stores found."}
                  </td>
                </tr>
              ) : (
                filtered.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <span className="wh-code">{item.code}</span>
                    </td>
                    <td>
                      <div className="wh-location-cell">
                        <span className="wh-location-icon">
                          {item.type === "STORE" ? (
                            <Store size={18} />
                          ) : (
                            <Warehouse size={18} />
                          )}
                        </span>
                        <div>
                          <strong>{item.name}</strong>
                          <small>{item.address}</small>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className="wh-type">
                        {item.type === "STORE"
                          ? "Store"
                          : "Main Warehouse"}
                      </span>
                    </td>
                    <td>
                      {!item.employeesLoaded
                        ? "Unavailable"
                        : managerOf(item)?.name || "Not assigned"}
                    </td>
                    <td>
                      <span className="wh-count">
                        <Users size={15} />
                        {item.employeesLoaded
                          ? item.employees.length
                          : "—"}
                      </span>
                    </td>
                    <td>
                      <StatusBadge status={item.status} />
                    </td>
                    <td>
                      <div className="wh-actions">
                        <button
                          type="button"
                          title="View Details"
                          aria-label={`View ${item.name}`}
                          onClick={() => openDialog("details", item)}
                        >
                          <Eye size={16} />
                        </button>

                        <button
                          type="button"
                          title="Edit Warehouse / Store"
                          disabled={!canManage || busy}
                          onClick={() => openEdit(item)}
                        >
                          <Pencil size={16} />
                        </button>

                        <button
                          type="button"
                          title={
                            item.status === "ACTIVE"
                              ? "Check Deactivation Impact"
                              : "Activate Warehouse / Store"
                          }
                          disabled={
                            busy ||
                            (item.status === "INACTIVE" && !canManage)
                          }
                          onClick={() =>
                            openDialog(
                              item.status === "ACTIVE"
                                ? "impact"
                                : "activate",
                              item
                            )
                          }
                        >
                          <Power size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="wh-table-footer">
          <Database size={15} />
          Current records. No warehouse/store delete endpoint is available.
        </div>
      </section>

      {/* MODALS */}
      {dialog && (
        <div
          className="wh-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeDialog();
            }
          }}
        >
          <section
            className="wh-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="wh-dialog-title"
          >
            <div className="wh-modal-header">
              <div>
                <span className="eyebrow">
                  ISSUE #25 · ACCOUNT DATA
                </span>
                <h2 id="wh-dialog-title">{modalTitle}</h2>
              </div>

              <button
                type="button"
                className="wh-close"
                onClick={closeDialog}
                disabled={busy}
                aria-label="Close dialog"
              >
                <X size={20} />
              </button>
            </div>

            {/* CREATE / EDIT */}
            {(dialog.type === "create" ||
              dialog.type === "edit") && (
              <form className="wh-modal-body" onSubmit={saveLocation}>
                <div className="wh-form-grid">
                  <label className="wh-field">
                    Warehouse / Store Name *
                    <input
                      required
                      minLength={3}
                      maxLength={150}
                      value={form.name}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          name: event.target.value,
                        }))
                      }
                    />
                  </label>

                  <label className="wh-field">
                    Warehouse / Store Type *
                    <select
                      value={form.type}
                      disabled={dialog.type === "edit"}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          type: event.target.value,
                        }))
                      }
                    >
                      <option value="MAIN_WAREHOUSE">
                        Main Warehouse
                      </option>
                      <option value="STORE">Store</option>
                    </select>
                  </label>

                  {/* ADDRESS AND MAP */}
                  <div className="wh-field wh-field-full">
                    <VietnamAddressPicker
                      resetKey={addressPickerKey}
                      disabled={busy}
                      onChange={({ address, wardName, provinceName, changed }) => {
                        setForm((previous) => ({
                          ...previous,
                          address: address.slice(0, 255),
                        }));
                        setMapMessage("");
                        // Chon Tinh/Phuong xong thi tu dong dua ban do toi khu vuc do
                        // (khong goi khi dang go so nha de tranh spam Photon).
                        if (changed === "street" || !provinceName) return;
                        void locateAddress([
                          address,
                          [wardName, provinceName].filter(Boolean).join(", "),
                          provinceName,
                        ]);
                      }}
                    />

                    <label htmlFor="wh-address-input">
                      Full Address *
                    </label>

                    <input
                      id="wh-address-input"
                      required
                      minLength={5}
                      maxLength={255}
                      placeholder="House number street, ward, province/city"
                      value={form.address}
                      onChange={(event) => {
                        setForm((previous) => ({
                          ...previous,
                          address: event.target.value,
                        }));
                        setMapMessage("");
                      }}
                      onKeyDown={(event) => {
                        // Enter = tim tren ban do (khong submit ca form)
                        if (event.key === "Enter") {
                          event.preventDefault();
                          if (!mapBusy && form.address.trim()) {
                            void locateAddress();
                          }
                        }
                      }}
                    />

                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 10,
                        flexWrap: "wrap",
                      }}
                    >
                      <button
                        type="button"
                        className="wh-button wh-button-secondary"
                        onClick={() => void locateAddress()}
                        disabled={
                          mapBusy ||
                          busy ||
                          !form.address.trim()
                        }
                      >
                        <MapPin size={15} />
                        {mapBusy
                          ? "Searching map..."
                          : "Find on Map"}
                      </button>

                      <span
                        style={{
                          fontSize: 11,
                          color: "#74849b",
                          fontWeight: 400,
                        }}
                      >
                        Press Enter or Find on Map to search. Click the map to fill the address from that point.
                      </span>
                    </div>

                    <AddressPickerMap
                      point={mapPoint}
                      onSelect={(point) =>
                        void chooseMapPoint(point)
                      }
                    />

                    {mapResults.length > 1 && (
                      <div className="wh-map-results" role="list" aria-label="Other matching locations">
                        <span>Not the right place? Choose another match:</span>
                        {mapResults.map((result, index) => (
                          <button
                            key={`${result.lat},${result.lng},${index}`}
                            type="button"
                            role="listitem"
                            title={result.detail}
                            className={
                              mapPoint &&
                              mapPoint.lat === result.lat &&
                              mapPoint.lng === result.lng
                                ? "active"
                                : ""
                            }
                            onClick={() => pickMapResult(result)}
                          >
                            <MapPin size={13} />
                            {result.label}
                          </button>
                        ))}
                      </div>
                    )}

                    {mapMessage && (
                      <p
                        className="wh-note"
                        role="status"
                        style={{ margin: 0 }}
                      >
                        {mapMessage}
                      </p>
                    )}

                    {mapError && (
                      <p
                        className="wh-form-error"
                        role="alert"
                        style={{ margin: 0 }}
                      >
                        {mapError}
                      </p>
                    )}

                    <small
                      style={{
                        color: "#8c9ab0",
                        fontSize: 11,
                        fontWeight: 400,
                      }}
                    >
                      Map data: OpenStreetMap. Address search: Nominatim (Photon as fallback).
                    </small>
                  </div>

                  <label className="wh-field wh-field-full">
                    Phone Number *
                    <input
                      required
                      type="tel"
                      placeholder="e.g. 0901234567"
                      value={form.phone}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          phone: event.target.value,
                        }))
                      }
                    />
                  </label>
                </div>

                {/* EMPLOYEES AND MANAGER */}
                {dialog.type === "edit" && selected && (
                  <section
                    style={{
                      marginTop: 28,
                      padding: 16,
                      border: "1px solid #e5ebf5",
                      borderRadius: 12,
                    }}
                  >
                    <div
                      className="wh-employees-header"
                      style={{ marginTop: 0 }}
                    >
                      <div>
                        <h3>Employees &amp; Manager</h3>
                        <p>
                          Assign a manager, add employees, or remove staff for this warehouse / store.
                        </p>
                      </div>
                    </div>

                    <InfoRow label="Current Manager">
                      {selected.employeesLoaded
                        ? managerOf(selected)?.name || "Not assigned"
                        : "Unavailable"}
                    </InfoRow>

                    {selected.status !== "ACTIVE" && (
                      <p className="wh-note">
                        Activate this warehouse / store before changing employee assignments.
                      </p>
                    )}

                    <div
                      className="wh-form-grid"
                      style={{ marginTop: 20 }}
                    >
                      <div className="wh-field">
                        <label htmlFor="wh-manager-select">
                          Assign / Change Manager
                        </label>
                        <select
                          id="wh-manager-select"
                          value={selectedManagerId}
                          onChange={(event) =>
                            setSelectedManagerId(event.target.value)
                          }
                          disabled={
                            candidateLoading ||
                            busy ||
                            !canManage ||
                            !selected.employeesLoaded ||
                            selected.status !== "ACTIVE" ||
                            Boolean(candidateError)
                          }
                        >
                          <option value="">
                            Select an available manager...
                          </option>
                          {availableManagers.map((user) => (
                            <option key={user.id} value={user.id}>
                              {user.name} ({user.email})
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="wh-field">
                        <label htmlFor="wh-employee-select">
                          Add Employee
                        </label>
                        <select
                          id="wh-employee-select"
                          value={selectedEmployeeId}
                          onChange={(event) =>
                            setSelectedEmployeeId(event.target.value)
                          }
                          disabled={
                            candidateLoading ||
                            busy ||
                            !canManage ||
                            !selected.employeesLoaded ||
                            selected.status !== "ACTIVE" ||
                            Boolean(candidateError)
                          }
                        >
                          <option value="">
                            Select an available employee...
                          </option>
                          {availableEmployees.map((user) => (
                            <option key={user.id} value={user.id}>
                              {user.name} ({user.email})
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>

                    {candidateLoading && (
                      <p className="wh-note">
                        Loading eligible employees...
                      </p>
                    )}

                    {candidateError && (
                      <p className="wh-form-error" role="alert">
                        {candidateError}
                      </p>
                    )}

                    {!candidateLoading &&
                      !candidateError &&
                      availableEmployees.length === 0 &&
                      availableManagers.length === 0 && (
                        <p className="wh-note">
                          No unassigned eligible employees or managers are available.
                        </p>
                      )}

                    <div className="wh-employees-header">
                      <div>
                        <h3>Assigned Employees</h3>
                        <p>
                          {selected.employeesLoaded
                            ? `${selected.employees.length} assigned`
                            : "Employee list unavailable"}
                        </p>
                      </div>
                    </div>

                    <div className="wh-employee-list">
                      {selected.employees.length === 0 ? (
                        <p className="wh-empty-employees">
                          {selected.employeesLoaded
                            ? "No employees assigned."
                            : "Unable to load employees."}
                        </p>
                      ) : (
                        selected.employees.map((user) => (
                          <div
                            className="wh-employee-row"
                            key={user.id}
                          >
                            <div>
                              <strong>{user.name}</strong>
                              <small>
                                {user.roles.join(", ") || "No role"}
                              </small>
                            </div>

                            {user.roles.includes(
                              managerRole(selected)
                            ) ? (
                              <span className="wh-manager-tag">
                                Manager
                              </span>
                            ) : (
                              <button
                                type="button"
                                className="wh-small-action"
                                disabled={
                                  !canManage ||
                                  busy ||
                                  selected.status !== "ACTIVE"
                                }
                                onClick={() =>
                                  void removeTeamMember(user)
                                }
                              >
                                <UserMinus size={15} />
                                Remove
                              </button>
                            )}
                          </div>
                        ))
                      )}
                    </div>
                  </section>
                )}

                {formError && (
                  <p className="wh-form-error" role="alert">
                    {formError}
                  </p>
                )}

                {/* SINGLE SAVE BUTTON */}
                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="wh-button wh-button-primary"
                    disabled={busy}
                  >
                    {busy
                      ? "Saving..."
                      : dialog.type === "create"
                        ? "Create Warehouse / Store"
                        : "Save Changes"}
                  </button>
                </div>
              </form>
            )}

            {/* DETAILS */}
            {dialog.type === "details" && selected && (
              <div className="wh-modal-body">
                <InfoRow label="Warehouse / Store Code">
                  {selected.code}
                </InfoRow>
                <InfoRow label="Warehouse / Store Name">
                  {selected.name}
                </InfoRow>
                <InfoRow label="Type">
                  {selected.type === "STORE"
                    ? "Store"
                    : "Main Warehouse"}
                </InfoRow>
                <InfoRow label="Address">
                  {selected.address}
                </InfoRow>
                <InfoRow label="Phone">
                  {selected.phone || "Not provided"}
                </InfoRow>
                <InfoRow label="Manager">
                  {selected.employeesLoaded
                    ? managerOf(selected)?.name || "Not assigned"
                    : "Unavailable"}
                </InfoRow>
                <InfoRow label="Status">
                  {selected.status}
                </InfoRow>

                <div className="wh-employees-header">
                  <div>
                    <h3>Assigned Employees</h3>
                    <p>
                      {selected.employeesLoaded
                        ? `${selected.employees.length} employees assigned`
                        : "Employee list unavailable"}
                    </p>
                  </div>
                </div>

                <div className="wh-employee-list">
                  {selected.employees.length === 0 ? (
                    <p className="wh-empty-employees">
                      {selected.employeesLoaded
                        ? "No employees assigned."
                        : "Unable to load employees."}
                    </p>
                  ) : (
                    selected.employees.map((user) => (
                      <div
                        className="wh-employee-row"
                        key={user.id}
                      >
                        <div>
                          <strong>{user.name}</strong>
                          <small>
                            {user.roles.join(", ") || "No role"}
                          </small>
                        </div>
                        {user.roles.includes(
                          managerRole(selected)
                        ) ? (
                          <span className="wh-manager-tag">
                            Manager
                          </span>
                        ) : (
                          <span className="wh-manager-tag">
                            Staff
                          </span>
                        )}
                      </div>
                    ))
                  )}
                </div>

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    onClick={closeDialog}
                  >
                    Close
                  </button>
                  <button
                    type="button"
                    className="wh-button wh-button-primary"
                    disabled={!canManage}
                    onClick={() => openEdit(selected)}
                  >
                    <Pencil size={15} />
                    Edit Warehouse / Store
                  </button>
                </div>
              </div>
            )}

            {/* DEACTIVATION IMPACT */}
            {dialog.type === "impact" && selected && (
              <div className="wh-modal-body">
                <p className="wh-confirm-text">
                  Review open business operations before deactivating{" "}
                  <strong>{selected.name}</strong>.
                </p>

                {impact && (
                  <>
                    <div className="wh-impact-list">
                      {Object.entries(
                        impact.blockingByModule || {}
                      ).length === 0 ? (
                        <div className="wh-impact-row">
                          <span>
                            <ClipboardList size={16} />
                            No blocking modules
                          </span>
                          <strong>0</strong>
                        </div>
                      ) : (
                        Object.entries(
                          impact.blockingByModule
                        ).map(([module, count]) => (
                          <div
                            className="wh-impact-row"
                            key={module}
                          >
                            <span>
                              <ClipboardList size={16} />
                              {module.replaceAll("_", " ")}
                            </span>
                            <strong>{count}</strong>
                          </div>
                        ))
                      )}
                    </div>

                    <div
                      className={`wh-impact-result ${
                        impact.canDeactivate ? "clear" : "blocked"
                      }`}
                    >
                      {impact.canDeactivate ? (
                        <CheckCircle2 size={19} />
                      ) : (
                        <AlertTriangle size={19} />
                      )}
                      <span>
                        {impact.canDeactivate
                          ? "No blocking operations. Deactivation is allowed by the impact check."
                          : `${impact.totalBlockingRecords} blocking record(s). Resolve them before deactivation.`}
                      </span>
                    </div>
                  </>
                )}

                {formError && (
                  <p className="wh-form-error" role="alert">
                    {formError}
                  </p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    onClick={closeDialog}
                    disabled={busy}
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    className="wh-button wh-button-danger"
                    disabled={
                      !canManage ||
                      busy ||
                      impactLoading ||
                      !impact?.canDeactivate
                    }
                    onClick={() => void changeStatus()}
                  >
                    {busy
                      ? "Processing..."
                      : "Deactivate Warehouse / Store"}
                  </button>
                </div>
              </div>
            )}

            {/* ACTIVATE */}
            {dialog.type === "activate" && selected && (
              <div className="wh-modal-body">
                <p className="wh-confirm-text">
                  Activate <strong>{selected.name}</strong>?
                </p>

                {formError && (
                  <p className="wh-form-error" role="alert">
                    {formError}
                  </p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    onClick={closeDialog}
                    disabled={busy}
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    className="wh-button wh-button-primary"
                    disabled={busy}
                    onClick={() => void changeStatus()}
                  >
                    {busy
                      ? "Processing..."
                      : "Activate Warehouse / Store"}
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
