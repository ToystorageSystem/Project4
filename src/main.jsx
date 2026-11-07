
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import "./css/index.css";

import AdminAuthGate from "./AdminAuthGate.jsx";
import IntegratedAdminAuthGate from "./IntegratedAdminAuthGate.jsx";

// Che do kiem thu Login + Google Authenticator.
// Mac dinh van su dung giao dien Admin cu.
const integrationMode =
  import.meta.env.DEV &&
  import.meta.env.VITE_TOYSTORAGE_2FA_INTEGRATION === "1";

const AuthGate = integrationMode
  ? IntegratedAdminAuthGate
  : AdminAuthGate;

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <AuthGate />
  </StrictMode>
);
