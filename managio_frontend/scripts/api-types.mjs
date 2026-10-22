// Regenerates src/lib/api/schema.d.ts from the running API's OpenAPI document (D-025).
// The API must run with API_DOCS_ENABLED=true. API_DOCS_URL overrides the default address.
// A Node script instead of shell syntax in package.json, so it works the same on Windows, macOS and Linux.
import { spawnSync } from "node:child_process";

const url = process.env.API_DOCS_URL || "http://localhost:8081/v3/api-docs";
const result = spawnSync("npx", ["openapi-typescript", url, "-o", "src/lib/api/schema.d.ts"], {
  stdio: "inherit",
  shell: process.platform === "win32",
});
process.exit(result.status ?? 1);
