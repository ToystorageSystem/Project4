# ToyStorage backend integration - schema precautions

`integration/backend` uses Hibernate `ddl-auto=validate`. A successful Java compilation or offline Spring startup cannot establish that the live MySQL schema matches the restored legacy APIs.

1. **Do not run a Seeder or these historical scripts on the shared team database.**
2. Back up/copy the schema to an isolated test database.
3. Run `verify_schema.sql` there first (read-only).
4. Confirm the columns and enum options against the real schema. The dev seed file includes the restored product prices/images, inventory minimums, invoice due dates, and transfer dates, but is not proof the shared database matches.
5. If migrations are necessary, create/review a migration tailored to that database. The `../legacy` scripts are retained only as **historical references** and must not be applied blindly: they can alter enum definitions and fail on already-migrated columns.
6. Only after schema validation, run `mvnw.cmd clean verify` and Postman regression tests on an isolated test database, especially #19 Return Approval -> Picking -> Packing -> Shipment -> Receiving, including retries/concurrency and role checks.

The integration does not enable Hibernate DDL updates or development data seeding.
