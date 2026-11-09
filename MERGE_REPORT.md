# Three-branch integration

Source snapshots retrieved from GitHub on 8 November 2026. The original branches were not modified.

| Source | Snapshot | Destination |
| --- | --- | --- |
| frontend | c1a2c9c344da67b84de277ef808bf1eb7ce50d0d | Base application, auth/session, warehouse and delivery workflows, shared UI |
| phuc_frontend | 4d57e09849480efa500bebabace3c459bef5b63c | src/features/admin |
| feature/business-fe-integration | 6c7e8a31ea226ba10564578846cd20df90e8ecb7 | src/pages/business*, src/api/business*, src/components/business, src/utils |

## Conflict decisions

This is a semantic integration, not three applications placed in one ZIP. There is one package.json, lockfile, Vite entry, BrowserRouter, AuthProvider, API origin, sidebar and logout flow.

- The frontend branch owns authentication. Its server-verified, staged MFA flow is retained. The other branches' separate providers and standalone login gates are not mounted.
- Admin's actual five management pages and live dashboard are retained. The dashboard was extracted from its original state-driven App; its quick actions now use real URLs. Sample INITIAL_USERS/INITIAL_WAREHOUSES are not used by this application.
- Admin API paths, request bodies, validation and response parsers are retained. Fetch calls import sessionFetch, which adds fresh CSRF before mutations, preserves native Responses/CSV behavior, includes credentials and clears the common session after business API 401. No automatic mutation retry.
- The warehouse/store Admin page's embedded account-switch form was removed; it cannot create a second authentication flow inside an already protected page. Management actions still require the original permission checks.
- Admin CSS is scoped to .admin-workspace to prevent generic selectors such as table, th, .breadcrumb and .page-heading from changing warehouse/business screens after visiting Admin.
- The business branch's active consolidated /business/* routes and API modules are retained. Its layout delegates to MainLayout; its pages use the common AuthContext and Axios client. Legacy business URLs remain aliases, including the source branch's redirects from old approval/request screens to the consolidated product/purchase-order pages.
- Legacy business screen source files are preserved in the archive even where the source branch no longer mounts them. They are not exposed as additional unsupported workflows.
- Warehouse and delivery pages are retained from frontend; older duplicate warehouse versions in the business branch are not copied over them. The source branch's receiving-pending screen is also included and routed for staff.
- The store-return API compatibility export in frontend is preserved. Shared StatusBadge accepts the business branch's optional label and status formatting. Shared WarehouseInfo uses the English default label.
- Only the additional used dependency lucide-react is added. qrcode.react from the standalone Admin auth app is unnecessary because the retained common MFA flow displays the backend-generated QR.

## Routing and roles

src/auth/access.js is the common authority normalization and home-route policy. A missing/unknown role does not silently receive warehouse modules. Unknown access keys deny access. Existing workflow permissions and backend checks remain the final authority.

src/routes/businessRoutes.jsx contains the active business pages. src/App.jsx mounts six Admin pages and keeps all original warehouse routes. src/components/layout/navigation.js builds menu entries from the same functional areas.

## Presentation

Original public/logo.png; orange identity; English interface; time-of-day greeting and actual user name. Dashboard welcome is shared across Admin, Business and Warehouse. Compact view and navigation search work in the common layout. Admin/business presentation adapters live in src/integration-theme.css; shared operations and personal workspace rules remain in their existing CSS files.

## Validation and limits

- Production bundle builds, including the imported modules and named exports.
- 40 automated tests pass. Tests cover the original workflows and merged role routing, cross-role guards, repeated ROLE_ prefixes, shared API payload/CSRF behavior, no mutation retry, Admin self-lock prevention with the email session field, and rendering the complete app at each supported home route.
- Source-wide no-undef check with browser globals reports no errors; existing React effect/fast-refresh warnings remain.
- Browser inspection uses labeled, isolated test fixtures: Admin/Business dashboards, account and product lists, and a 390px mobile frame. Fixture code is not part of the production entry.
- No live database mutations, emails or production account actions were executed. Live end-to-end backend compatibility is not claimed.
- The integration/backend snapshot inspected still has an older non-staged LoginResponse. This frontend retains the newer auth contract from the requested frontend branch; use a backend with the corresponding auth endpoints and the Admin/Business/Warehouse APIs. The ZIP does not silently weaken CSRF or MFA to accommodate a different backend.

For a Figma output, an editable Design or Code destination was not specified. The requested deliverable is this runnable code ZIP; no Figma import or publication is claimed.
