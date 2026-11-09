import { afterEach, describe, it, expect, vi } from "vitest";
import api from "../src/api/api";
import { authPath, canAccess, isManager, isStaff } from "../src/auth/access";
const adapter = api.defaults.adapter;
afterEach(() => {
  api.defaults.adapter = adapter;
  vi.restoreAllMocks();
});
describe("session API", () => {
  it("refreshes CSRF before every write, including session rotation", async () => {
    const calls = [];
    let token = 0;
    api.defaults.adapter = async (config) => {
      calls.push({
        url: config.url,
        token: config.headers.get("X-CSRF-TOKEN"),
      });
      return {
        data:
          config.url === "/auth/csrf"
            ? { token: `token-${++token}`, headerName: "X-CSRF-TOKEN" }
            : {},
        status: 200,
        headers: {},
        config,
      };
    };
    await api.post("/auth/login", {});
    await api.post("/auth/2fa/verify", {});
    expect(calls).toEqual([
      { url: "/auth/csrf", token: undefined },
      { url: "/auth/login", token: "token-1" },
      { url: "/auth/csrf", token: undefined },
      { url: "/auth/2fa/verify", token: "token-2" },
    ]);
  });
  it("keeps invalid OTP on the auth screen but expires unauthorized business sessions", async () => {
    const expired = vi.fn();
    window.addEventListener("session-expired", expired);
    api.defaults.adapter = async (config) => {
      throw { response: { status: 401 }, config };
    };
    await expect(api.get("/auth/status")).rejects.toBeTruthy();
    expect(expired).not.toHaveBeenCalled();
    await expect(api.get("/warehouse/inventory/balances")).rejects.toBeTruthy();
    expect(expired).toHaveBeenCalledOnce();
    window.removeEventListener("session-expired", expired);
  });
  it("never retries a failed mutation", async () => {
    let writes = 0;
    api.defaults.adapter = async (config) => {
      if (config.url === "/auth/csrf")
        return {
          data: { token: "t", headerName: "X-CSRF-TOKEN" },
          status: 200,
          headers: {},
          config,
        };
      writes++;
      throw { response: { status: 503 }, config };
    };
    await expect(
      api.patch("/stock-transfers/1/confirm-shipment"),
    ).rejects.toBeTruthy();
    expect(writes).toBe(1);
  });
});
describe("roles and authentication stages", () => {
  it("routes pending sessions through the required security steps", () => {
    for (const [stage, path] of Object.entries({
      CHANGE_PASSWORD: "/change-password",
      SETUP_2FA: "/2fa/setup",
      VERIFY_2FA: "/2fa",
      AUTHENTICATED: "/warehouse/dashboard",
    }))
      expect(authPath({ stage, authorities: ["ROLE_WAREHOUSE_MANAGER"] })).toBe(path);
    expect(authPath(null)).toBe("/login");
    expect(authPath({ stage: "UNKNOWN" })).toBe("/login");
  });
  it("separates staff execution, manager decisions, and store permissions", () => {
    const manager = { authorities: ["ROLE_WAREHOUSE_MANAGER"] },
      staff = { authorities: ["ROLE_ROLE-WH-STAFF"] };
    expect(isManager(manager)).toBe(true);
    expect(isStaff(staff)).toBe(true);
    expect(canAccess(staff, "stock-counts")).toBe(false);
    expect(canAccess(staff, "picking")).toBe(true);
    expect(canAccess(manager, "stock-counts")).toBe(true);
    expect(canAccess(manager, "picking")).toBe(false);
    expect(canAccess(manager, "stores")).toBe(false);
    expect(canAccess({ authorities: ["STORE_VIEW"] }, "stores")).toBe(true);
    expect(canAccess({ authorities: [] }, "packing-confirmation")).toBe(false);
  });
});
