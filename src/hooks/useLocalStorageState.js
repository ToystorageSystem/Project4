
import { useEffect, useState } from "react";

// Luu React State vao localStorage de khong mat khi F5.
export default function useLocalStorageState(
  key,
  initialValue
) {
  const [state, setState] = useState(() => {
    try {
      const saved = window.localStorage.getItem(key);

      if (saved !== null) {
        return JSON.parse(saved);
      }
    } catch (error) {
      console.error(
        "Failed to load local data:",
        error
      );
    }

    return typeof initialValue === "function"
      ? initialValue()
      : initialValue;
  });

  useEffect(() => {
    try {
      window.localStorage.setItem(
        key,
        JSON.stringify(state)
      );
    } catch (error) {
      console.error(
        "Failed to save local data:",
        error
      );
    }
  }, [key, state]);

  return [state, setState];
}
