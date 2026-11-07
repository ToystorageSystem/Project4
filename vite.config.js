
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Che do kiem thu Backend Integration.
// Mac dinh giu nguyen cau hinh Backend cu.
const integrationMode =
  process.env.TOYSTORAGE_2FA_INTEGRATION === "1";

const apiTarget = integrationMode
  ? "http://127.0.0.1:8082"
  : "http://localhost:8080";

const authTarget = integrationMode
  ? "http://127.0.0.1:8082"
  : "http://localhost:8081";

export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,

    proxy: {
      // Login + Google Authenticator.
      "/auth2fa": {
        target: authTarget,
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/auth2fa/, "/api/auth"),
      },

      // Existing Backend business APIs.
      "/api": {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
});
