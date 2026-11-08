import { useEffect, useRef, useState } from "react";

export function dayPeriod(date = new Date()) {
  const hour = date.getHours();
  if (hour >= 5 && hour < 12) return { key: "morning", greeting: "Good morning", note: "A fresh start. Your warehouse is within reach.", icon: "sunrise" };
  if (hour >= 12 && hour < 18) return { key: "afternoon", greeting: "Good afternoon", note: "Pick up where you left off. Here's what needs your attention.", icon: "sun" };
  return { key: "evening", greeting: "Good evening", note: "A clear view of what's moving, and what still needs you.", icon: "moon" };
}

export function useWorkspaceClock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const update = () => setNow(new Date());
    const timer = window.setInterval(update, 30000);
    document.addEventListener("visibilitychange", update);
    return () => { window.clearInterval(timer); document.removeEventListener("visibilitychange", update); };
  }, []);
  return { now, ...dayPeriod(now) };
}

export function displayName(user) {
  return (user?.name || user?.fullName || user?.username || "").trim().split(/\s+/).at(-1) || "there";
}

// Preferences never participate in authentication or authorization.
export function useWorkspacePreferences(user) {
  const identity = user?.id ?? user?.email;
  const storageKey = identity == null ? null : `toystorage:workspace:${identity}`;
  const [stored, setStored] = useState({ key: null, value: {} });
  const latest = useRef({ key: null, value: {} });
  useEffect(() => {
    function load() {
      let value = {};
      try {
        const parsed = storageKey && JSON.parse(localStorage.getItem(storageKey) || "{}");
        if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) value = parsed;
      } catch { /* Storage may be unavailable in private or managed browsers. */ }
      latest.current = { key: storageKey, value };
      setStored(latest.current);
    }
    function changed(event) {
      if (event.detail?.key === storageKey) {
        latest.current = { key: storageKey, value: event.detail.value };
        setStored(latest.current);
      }
    }
    load();
    window.addEventListener("workspace-preferences", changed);
    window.addEventListener("storage", load);
    return () => { window.removeEventListener("workspace-preferences", changed); window.removeEventListener("storage", load); };
  }, [storageKey]);
  const value = stored.key === storageKey ? stored.value : {};
  function update(patch) {
    const next = { ...(latest.current.key === storageKey ? latest.current.value : {}), ...patch };
    try { if (storageKey) localStorage.setItem(storageKey, JSON.stringify(next)); } catch { /* Keep in-memory preferences usable. */ }
    latest.current = { key: storageKey, value: next };
    setStored(latest.current);
    window.dispatchEvent(new CustomEvent("workspace-preferences", { detail: { key: storageKey, value: next } }));
  }
  return {
    compact: value.compact === true,
    pins: Array.isArray(value.pins) ? value.pins.filter(k => typeof k === "string").slice(0, 6) : null,
    update,
  };
}
