
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import "./css/index.css";
import AdminAuthGate from "./AdminAuthGate.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <AdminAuthGate />
  </StrictMode>
);
