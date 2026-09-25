# Reservation Hub API automation (Restful-booker)

This project is a QA safety net for the Booking API used by Reservation Hub. It covers the core customer flow, auth policy, negative validation, and boundary checks for a public sandboxed API that is intentionally seeded with defects.

From a reviewer perspective, this is designed to demonstrate practical QA judgement: strong regression coverage, business-rule assertions, defect documentation, and clear evidence that the live API can violate the expected contract.

Run with Maven 3.8+:

```bash
mvn clean test
```

The suite writes an HTML report to `target/cucumber-report.html`, Cucumber JSON to `target/cucumber.json`, and JUnit/Surefire XML to `target/surefire-reports/`. The repository also keeps a checked-in CI workflow that stores those artifacts even when the live sandbox is misbehaving.

## What this suite is designed to do

This is not a generic smoke test. It is designed to answer the product question: "Does the API enforce the business contract we expect?"

The suite covers:
- health checks and cold-start handling
- token retrieval and auth enforcement
- create/read/update/delete happy path
- unauthorized PUT/DELETE/PATCH rejection
- unknown-ID handling
- list filtering by booking fields
- malformed payloads, wrong types, missing fields, negative totals, zero totals, reversed dates, malformed dates
- schema validation for create responses

These scenarios are deliberately chosen to catch the exact kinds of defects that can otherwise reach production and trigger partner escalations.

## Expected contract vs current sandbox reality

The project asserts the correct Booking API behavior, not the current public sandbox behavior.

In practice, this means the suite is designed to detect when the API:
- returns 5xx for client validation problems
- accepts negative totals or wrong-type numeric fields
- accepts reversed or malformed dates
- silently coerces invalid input instead of rejecting it

When the sandbox violates the expected contract, the defect is surfaced in the run output and captured in `BUGS.md`. That is the core purpose of this assignment: to create a safety net that catches defects before they reach customers.

## Configuration and diagnostics

The default endpoint is `https://restful-booker.herokuapp.com`. Override it with `-DbaseUrl=https://...` or `BASE_URL`. Supply `BOOKER_USERNAME` and `BOOKER_PASSWORD` for credentials; the defaults are the public sandbox credentials from the assignment.

The request specification uses `Accept: application/json`, not the broader REST Assured JSON value, because that value triggers `418` on this sandbox. Requests have connection and socket timeouts, and logs include method, URL, safe headers, request/response bodies, and status. Auth payloads, tokens, and auth headers are redacted.

## Strategy and test design

The suite keeps test data self-contained and independent of the seeded database. Each scenario creates its own booking and cleans up any record it created, even if the API unexpectedly accepts invalid payloads.

Assertions are intentionally specific and product-oriented:
- status codes
- response schema compatibility
- field-level values
- persisted updates after PUT/PATCH
- auth side effects
- deletion and not-found behavior

Tags such as `@auth`, `@negative`, `@crud`, `@P1`, and `@P2` make the report navigable by feature and priority, which helps a stakeholder quickly understand the risk profile.

## Reliability and scope

The health check retries only safe GET `/ping` requests with a short exponential backoff. Mutating calls are not retried because repeated writes can create duplicate bookings. Requests have finite timeouts, and the suite is intentionally resilient to the shared/public sandbox resetting or warming up.

This is a focused regression suite for booking validation and auth behavior. It is not a load test, a penetration test, or a full OpenAPI compatibility suite. The live findings and their evidence are documented in `test-report/README.md` and `BUGS.md`.

## Report and defect outputs

The project includes:
- a readable Cucumber HTML report
- JSON and XML result artifacts
- a defect log with exact reproduction steps and expected vs actual behavior

This combination gives both engineering and non-technical stakeholders a clear view of the product risk and the remediation work needed. In short, this is a safety-net suite that does not just test happy paths; it catches the kinds of validation regressions that matter to customers and partners.
