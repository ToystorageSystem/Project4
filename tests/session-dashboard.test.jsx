import { afterEach, expect, it, vi } from "vitest";
import { render, screen, cleanup, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import api from "../src/api/api";
import { AuthProvider, useAuth } from "../src/auth/AuthContext";
import ProtectedRoute from "../src/auth/ProtectedRoute";
import Dashboard from "../src/pages/warehouses/dashboard/WarehouseDashboardPage";
const original = api.defaults.adapter;
afterEach(() => {
  cleanup();
  api.defaults.adapter = original;
  sessionStorage.clear();
  vi.restoreAllMocks();
});
function respond(handler) {
  api.defaults.adapter = async (config) => {
    const data = handler(config.url);
    return { data, status: 200, headers: {}, config };
  };
}
function mount(node) {
  render(
    <MemoryRouter initialEntries={["/warehouse/dashboard"]}>
      <AuthProvider>{node}</AuthProvider>
    </MemoryRouter>,
  );
}
it("ignores forged saved users and checks the server before granting access", async () => {
  sessionStorage.setItem(
    "currentUser",
    JSON.stringify({
      stage: "AUTHENTICATED",
      authorities: ["ROLE_WAREHOUSE_MANAGER"],
    }),
  );
  api.defaults.adapter = async (config) => {
    throw { response: { status: 401 }, config };
  };
  mount(
    <Routes>
      <Route element={<ProtectedRoute access="stock-counts" />}>
        <Route
          path="/warehouse/dashboard"
          element={<p>Privileged content</p>}
        />
      </Route>
      <Route path="/login" element={<p>Server requires login</p>} />
    </Routes>,
  );
  await screen.findByText("Server requires login");
  expect(screen.queryByText("Privileged content")).toBeNull();
  expect(sessionStorage.getItem("currentUser")).toBeNull();
});
it("blocks staff who directly open a manager route", async () => {
  respond(() => ({
    stage: "AUTHENTICATED",
    authorities: ["ROLE_WAREHOUSE_STAFF"],
  }));
  mount(
    <Routes>
      <Route element={<ProtectedRoute access="stock-counts" />}>
        <Route
          path="/warehouse/dashboard"
          element={<p>Privileged content</p>}
        />
      </Route>
    </Routes>,
  );
  await screen.findByText("Access denied");
  expect(screen.queryByText("Privileged content")).toBeNull();
});
it("reads paged totals and keeps partial dashboard failures distinct from zero", async () => {
  api.defaults.adapter = async (config) => {
    const url = config.url;
    let data;
    if (url === "/auth/status")
      data = {
        id: 1,
        name: "Tùng Lâm",
        stage: "AUTHENTICATED",
        authorities: ["ROLE_WAREHOUSE_MANAGER"],
      };
    else if (url.includes("/monitoring"))
      data = {
        totalElements: 21,
        content: [
          {
            receiptId: 1,
            receiptCode: "GR-001",
            staffName: "Lan",
            progressPercent: 50,
            issueProducts: 2,
          },
        ],
      };
    else if (url.includes("/waiting-review"))
      throw { response: { status: 503 }, config };
    else if (url === "/putaway-plans")
      data = [{ status: "AVAILABLE" }, { status: "COMPLETED" }];
    else data = { totalElements: 7, content: [] };
    return { data, status: 200, headers: {}, config };
  };
  mount(<Dashboard />);
  await screen.findByText("GR-001");
  expect(screen.getByText("21")).toBeTruthy();
  expect(screen.getByText("7")).toBeTruthy();
  expect(screen.getByText("Data unavailable")).toBeTruthy();
  expect(screen.getByRole("heading", { name: "Queue temporarily unavailable." })).toBeTruthy();
  expect(screen.getByRole("alert").textContent).toContain(
    "Some data could not be loaded",
  );
});
it("clears the session after unauthorized operational requests", async () => {
  respond(() => ({
    id: 1,
    stage: "AUTHENTICATED",
    authorities: ["ROLE_WAREHOUSE_STAFF"],
  }));
  function Probe() {
    const { user } = useAuth();
    return <p>{user ? "Session is live" : "Session is cleared"}</p>;
  }
  mount(<Probe />);
  await screen.findByText("Session is live");
  api.defaults.adapter = async (config) => {
    throw { response: { status: 401 }, config };
  };
  await expect(api.get("/warehouse/inventory/balances")).rejects.toBeTruthy();
  await waitFor(() =>
    expect(screen.getByText("Session is cleared")).toBeTruthy(),
  );
});
