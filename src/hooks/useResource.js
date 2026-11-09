import { useCallback, useEffect, useRef, useState } from "react";
import api, { errorMessage } from "../api/api";
export default function useResource(path) {
  const [data, setData] = useState(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState("");
  const revision = useRef(0);
  const reload = useCallback(async () => {
    const current = ++revision.current;
    if (!path) {
      setData(null);
      setLoading(false);
      setError("");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const response = await api.get(path);
      if (current === revision.current) setData(response.data);
      return response.data;
    } catch (e) {
      if (current === revision.current) setError(errorMessage(e));
      return null;
    } finally {
      if (current === revision.current) setLoading(false);
    }
  }, [path]);
  useEffect(() => {
    setData(null);
    reload();
    return () => {
      revision.current++;
    };
  }, [reload]);
  return { data, loading, error, reload };
}
