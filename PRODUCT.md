# ToyStorage

<!-- impeccable:product-schema 1 -->

## Platform
web

## Users and purpose
Administrators manage accounts, roles, locations, audit logs and security alerts. Business Operations handles products, suppliers, purchases, invoices and distribution. Warehouse managers, warehouse staff and delivery staff receive, inspect, store, count, pick, pack and hand over goods. All areas share one server-verified session and role-aware navigation.

## Confirmed constraints
React and Vite frontend; Spring Boot backend; session cookie and CSRF; server-owned authentication stages and MFA. Dashboard figures must use API data and distinguish failure from zero. Preserve existing operational routes and workflows.

## Brand commitments
Original ToyStorage logo (`public/logo.png`), orange #f25d19, English interface, greetings for morning, afternoon and evening. The user requests a personal, thoughtfully designed workspace based on the Impeccable guidance.

## Personalization
Use the authenticated user's actual name and role. Time comes from the browser's local clock. Page shortcuts and compact density are browser-local preferences scoped to each account; they do not grant access or change server settings.

## Evidence
Source snapshots and merge decisions are recorded in MERGE_REPORT.md. Visual QA fixtures are isolated under tests/visual, explicitly marked sample records, never imported by the production app.
