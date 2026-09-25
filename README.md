Reservation Hub API automation (Restful-booker)

Java 17, REST Assured and Cucumber BDD suite for the public Restful-booker sandbox. Run with Maven 3.8+:

```bash
mvn clean test
```

The suite writes an HTML report to `target/cucumber-report.html`, Cucumber JSON to `target/cucumber.json`, and JUnit/Surefire XML to `target/surefire-reports/`. A checked-in GitHub Actions workflow runs the suite on pushes and pull requests and uploads these reports even when assertions fail.

## Configuration and diagnostics

The default endpoint is `https://restful-booker.herokuapp.com`. Override it with `-DbaseUrl=https://...` or `BASE_URL`. Supply `BOOKER_USERNAME` and `BOOKER_PASSWORD` for credentials; the defaults are the public sandbox credentials from the assignment. The request specification uses `Accept: application/json`. REST Assured's broader JSON Accept value caused this sandbox to return 418, so it is set explicitly. Requests have connection and response timeouts. Logs include method, URL, safe headers, request/response bodies, and status; auth payloads, tokens, and auth headers are redacted.

## Strategy

The feature suite covers health, token retrieval and use, create/read/PUT/PATCH/delete, list filtering, unauthorized PUT/DELETE/PATCH, unknown IDs, and malformed, missing, wrong-type, negative-price, zero-price, reversed-date, and malformed-date payloads. The zero-price boundary is treated as valid because the response schema specifies a minimum of zero; negative totals must be rejected. Tests create unique records and use IDs from their own responses. Each scenario cleans up any created booking, including invalid inputs that the API unexpectedly accepts.

Assertions check status, response schema, response fields, persisted PUT/PATCH changes, authorization side effects, and post-delete absence. Tags such as `@auth`, `@negative`, `@crud`, `@P1`, and `@P2` make the HTML report navigable. The API's reproduced validation defects intentionally fail their assertions; that red result is the safety net working as designed.

## Reliability and scope

The health check retries only safe GET `/ping` requests with a short exponential backoff. Mutating calls are not retried because a repeated POST could create duplicates. Requests have finite timeouts, and test data is independent of the seeded IDs. The service is shared and may reset or become unavailable; CI therefore retains reports and diagnostics for failed runs.

This is a focused regression suite, not load or penetration testing, nor full OpenAPI compatibility testing. The live run and reproduced defects are documented in `test-report/README.md` and `BUGS.md`.
