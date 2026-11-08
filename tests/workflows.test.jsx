import { afterEach, beforeEach, describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  waitFor,
  cleanup,
  fireEvent,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import api from "../src/api/api";
import Login from "../src/pages/auth/Login";
import StockCountsPage from "../src/pages/warehouses/stockcount/StockCountsPage";
import StaffReceivingPage from "../src/pages/warehouses/staff/StaffReceivingPage";
import ScanWorkflowPage from "../src/pages/warehouses/staff/ScanWorkflowPage";
const auth = vi.hoisted(() => ({
  user: null,
  setUser: vi.fn(),
  clearUser: vi.fn(),
  loading: false,
  error: null,
  refresh: vi.fn(),
}));
vi.mock("../src/auth/AuthContext", () => ({ useAuth: () => auth }));
const adapter = api.defaults.adapter;
let requests = [];
function backend(handler) {
  api.defaults.adapter = async (config) => {
    const body =
      typeof config.data === "string" ? JSON.parse(config.data) : config.data;
    requests.push({ url: config.url, method: config.method, data: body });
    return {
      data:
        config.url === "/auth/csrf"
          ? { token: "fresh", headerName: "X-CSRF-TOKEN" }
          : handler(config.url, config.method, body),
      status: 200,
      headers: {},
      config,
    };
  };
}
function mount(node, path = "/") {
  return render(<MemoryRouter initialEntries={[path]}>{node}</MemoryRouter>);
}
beforeEach(() => {
  requests = [];
  auth.user = {
    id: 1,
    name: "Manager",
    stage: "AUTHENTICATED",
    authorities: ["ROLE_WAREHOUSE_MANAGER"],
  };
  auth.setUser.mockClear();
  vi.spyOn(window, "confirm").mockReturnValue(true);
});
afterEach(() => {
  cleanup();
  api.defaults.adapter = adapter;
  vi.restoreAllMocks();
});
describe("authentication UI", () => {
  it("sends credentials and respects the backend stage", async () => {
    auth.user = null;
    backend(() => ({ id: 1, stage: "VERIFY_2FA" }));
    mount(<Login />, "/login");
    fireEvent.change(screen.getByLabelText("Work email"), {
      target: { value: "staff@example.com" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "current password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in →" }));
    await waitFor(() =>
      expect(auth.setUser).toHaveBeenCalledWith({ id: 1, stage: "VERIFY_2FA" }),
    );
    expect(requests.find((r) => r.url === "/auth/login").data).toEqual({
      email: "staff@example.com",
      password: "current password",
    });
  });
  it("shows QR and manual key from setup and keeps recovery codes out of storage", async () => {
    auth.user = { id: 1, stage: "SETUP_2FA" };
    backend((url) =>
      url.endsWith("/setup")
        ? { qrDataUrl: "data:image/png;base64,AA==", manualKey: "TESTKEY" }
        : { stage: "AUTHENTICATED", recoveryCodes: ["ONE-TIME-CODE"] },
    );
    mount(<Login />, "/2fa/setup");
    await screen.findByAltText("Google Authenticator setup QR code");
    expect(screen.getByText("TESTKEY")).toBeTruthy();
    fireEvent.change(screen.getByLabelText("Google Authenticator code (6 digits)"), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Continue →" }));
    await screen.findByText("ONE-TIME-CODE");
    expect(requests.find((r) => r.url === "/auth/2fa/confirm").data).toEqual({
      code: "123456",
    });
    expect(sessionStorage.getItem("currentUser")).toBeNull();
    expect(localStorage.length).toBe(0);
  });
  it("blocks mismatched new passwords before a reset request", async () => {
    auth.user = null;
    backend(() => ({}));
    mount(<Login />, "/reset-password?token=test");
    fireEvent.change(screen.getByLabelText("New password · at least 12 characters"), {
      target: { value: "long-password-one" },
    });
    fireEvent.change(screen.getByLabelText("Confirm new password"), {
      target: { value: "long-password-two" },
    });
    fireEvent.submit(
      screen.getByRole("button", { name: "Continue →" }).closest("form"),
    );
    await screen.findByText("Your new passwords do not match.");
    expect(requests).toHaveLength(0);
  });
});
describe("warehouse workflows", () => {
  it("requires individual review and echoes every final quantity on manager approval", async () => {
    const plan = {
      id: 10,
      code: "SC-10",
      status: "PENDING_CONFIRMATION",
      date: "2026-11-09",
      scopeType: "ALL",
      total: 2,
      counted: 2,
    };
    const items = [
      {
        id: 1,
        sku: "SKU-1",
        name: "Đồ chơi 1",
        system: 5,
        first: 5,
        finalQuantity: 5,
        difference: 0,
      },
      {
        id: 2,
        sku: "SKU-2",
        name: "Đồ chơi 2",
        system: 4,
        first: 3,
        finalQuantity: 3,
        difference: -1,
      },
    ];
    backend((url) =>
      url.endsWith("/10")
        ? { plan, items, audit: [], missingSkus: [] }
        : url.endsWith("stock-counts")
          ? [plan]
          : [],
    );
    mount(<StockCountsPage />);
    await screen.findByText("SC-10");
    fireEvent.click(screen.getByRole("button", { name: "Open ↗" }));
    await screen.findByText("Đồ chơi 1");
    const approve = screen.getByRole("button", { name: /Confirm all/ });
    expect(approve.disabled).toBe(true);
    const checks = screen.getAllByRole("checkbox", { name: "Reviewed" });
    fireEvent.click(checks[0]);
    expect(approve.disabled).toBe(true);
    fireEvent.click(checks[1]);
    expect(approve.disabled).toBe(false);
    fireEvent.click(approve);
    await waitFor(() =>
      expect(requests.some((r) => r.url.endsWith("/10/confirm"))).toBe(true),
    );
    expect(requests.find((r) => r.url.endsWith("/10/confirm")).data).toEqual({
      items: [
        { itemId: 1, finalQuantity: 5 },
        { itemId: 2, finalQuantity: 3 },
      ],
    });
  });
  it("records receiving as actual and damaged quantities, without inventing accepted stock", async () => {
    auth.user = {
      id: 2,
      stage: "AUTHENTICATED",
      authorities: ["ROLE_WAREHOUSE_STAFF"],
    };
    const row = {
      receiptId: 4,
      receiptCode: "GR-4",
      status: "RECEIVING",
      items: [
        {
          productId: 8,
          productCode: "P8",
          productName: "Hộp đồ chơi",
          expectedQuantity: 10,
          actualQuantity: null,
          damagedQuantity: 0,
          inspected: false,
        },
      ],
    };
    backend((url) =>
      url.endsWith("/receiving")
        ? [row]
        : url.endsWith("/receiving-detail")
          ? row
          : {},
    );
    mount(<StaffReceivingPage />);
    await screen.findByText("GR-4");
    fireEvent.click(screen.getByRole("button", { name: "Inspect ↗" }));
    await screen.findByLabelText("Received P8");
    fireEvent.change(screen.getByLabelText("Received P8"), {
      target: { value: "9" },
    });
    fireEvent.change(screen.getByLabelText("Damaged P8"), {
      target: { value: "1" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Save inspection results" }),
    );
    await waitFor(() =>
      expect(requests.some((r) => r.url.endsWith("/4/inspections"))).toBe(true),
    );
    expect(
      requests.find((r) => r.url.endsWith("/4/inspections")).data.items[0],
    ).toEqual({
      productId: 8,
      actualQuantity: 9,
      damagedQuantity: 1,
      packageCode: "",
      notes: "",
    });
  });
  it("submits picking scans with the item id and scanned codes", async () => {
    auth.user = {
      id: 2,
      stage: "AUTHENTICATED",
      authorities: ["ROLE_WAREHOUSE_STAFF"],
    };
    const transfer = {
      transferId: 7,
      transferCode: "ST-7",
      status: "PICKING",
      items: [
        {
          itemId: 3,
          productCode: "P3",
          productName: "Gấu bông",
          approvedQuantity: 2,
          pickedQuantity: 0,
          remainingQuantity: 2,
        },
      ],
    };
    backend((url) =>
      url.endsWith("/transfers")
        ? [transfer]
        : url.endsWith("/7")
          ? transfer
          : {},
    );
    mount(<ScanWorkflowPage kind="picking" />);
    await screen.findByText("ST-7");
    fireEvent.click(screen.getByRole("button", { name: "Open task ↗" }));
    await screen.findByLabelText("Item line");
    fireEvent.change(screen.getByLabelText("Item line"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByLabelText("Scan product barcode"), {
      target: { value: "89300003" },
    });
    fireEvent.change(screen.getByLabelText("Scan location code"), {
      target: { value: "A-01" },
    });
    fireEvent.change(screen.getByLabelText("Quantity"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Record" }));
    await waitFor(() =>
      expect(requests.some((r) => r.url.endsWith("/7/items/3"))).toBe(true),
    );
    expect(requests.find((r) => r.url.endsWith("/7/items/3")).data).toEqual({
      productBarcode: "89300003",
      locationCode: "A-01",
      quantity: 2,
    });
  });
});
