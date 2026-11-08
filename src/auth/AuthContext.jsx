import { createContext, useContext, useEffect, useRef, useState } from "react";
import { getAuthStatus } from "../api/auth/auth";
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUserState] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const revision = useRef(0);
  // Explicit auth transitions supersede any older status requests.
  function setUser(value) {
    revision.current++;
    setUserState(value);
    setLoading(false);
    setError(null);
  }
  async function refresh() {
    const current = ++revision.current;
    setLoading(true);
    setError(null);
    try {
      const data = await getAuthStatus();
      if (current === revision.current) setUserState(data);
      return data;
    } catch (e) {
      if (current === revision.current) {
        setUserState(null);
        if (e.response?.status !== 401) setError(e);
      }
      return null;
    } finally {
      if (current === revision.current) setLoading(false);
    }
  }
  useEffect(() => {
    sessionStorage.removeItem("currentUser");
    refresh();
    const expire = () => setUser(null);
    window.addEventListener("session-expired", expire);
    return () => {
      revision.current++;
      window.removeEventListener("session-expired", expire);
    };
  }, []);
  return (
    <AuthContext.Provider
      value={{
        user,
        setUser,
        clearUser: () => setUser(null),
        loading,
        error,
        refresh,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
