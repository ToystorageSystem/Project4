import { afterEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { dayPeriod, displayName, useWorkspacePreferences } from "../src/hooks/useWorkspacePersonalization";
afterEach(() => { cleanup(); vi.restoreAllMocks(); localStorage.clear(); });
it("uses the correct greeting across each day boundary", () => {
  for (const [hour, minute, key] of [[4,59,"evening"],[5,0,"morning"],[11,59,"morning"],[12,0,"afternoon"],[17,59,"afternoon"],[18,0,"evening"],[0,0,"evening"]]) {
    expect(dayPeriod(new Date(2026,10,8,hour,minute)).key).toBe(key);
  }
  expect(displayName({ name: "  Đinh Tùng Lâm  " })).toBe("Lâm");
});
function PreferenceProbe({ id, type }) {
  const { compact, pins, update } = useWorkspacePreferences({ id });
  return <button onClick={() => update(type === "density" ? { compact: !compact } : { pins: ["inventory"] })}>{id}/{type}: {compact ? "compact" : "comfortable"}/{pins?.join() || "defaults"}</button>;
}
it("shares account preferences between controls and preserves each setting", () => {
  render(<><PreferenceProbe id={1} type="density" /><PreferenceProbe id={1} type="pins" /><PreferenceProbe id={2} type="density" /></>);
  fireEvent.click(screen.getByText("1/density: comfortable/defaults"));
  fireEvent.click(screen.getByText("1/pins: compact/defaults"));
  expect(screen.getByText("1/density: compact/inventory")).toBeTruthy();
  expect(screen.getByText("2/density: comfortable/defaults")).toBeTruthy();
  expect(JSON.parse(localStorage.getItem("toystorage:workspace:1"))).toEqual({ compact:true, pins:["inventory"] });
});
it("recovers from malformed saved preferences", () => {
  localStorage.setItem("toystorage:workspace:1", "not-json");
  render(<PreferenceProbe id={1} type="density" />);
  fireEvent.click(screen.getByText("1/density: comfortable/defaults"));
  expect(screen.getByText("1/density: compact/defaults")).toBeTruthy();
});
it("keeps both preferences in memory when storage is readable but writes fail", () => {
  localStorage.setItem("toystorage:workspace:1", "{}");
  vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => { throw new DOMException("Quota exceeded", "QuotaExceededError"); });
  render(<><PreferenceProbe id={1} type="density" /><PreferenceProbe id={1} type="pins" /></>);
  fireEvent.click(screen.getByText("1/density: comfortable/defaults"));
  fireEvent.click(screen.getByText("1/pins: compact/defaults"));
  expect(screen.getByText("1/density: compact/inventory")).toBeTruthy();
  expect(localStorage.getItem("toystorage:workspace:1")).toBe("{}");
});
