# Test run summary

**Target:** Restful-booker sandbox (`https://restful-booker.herokuapp.com`)  
**Run date:** 25 September 2026  
**Suite:** Java 17, Maven, REST Assured, Cucumber BDD  
**Current result:** 17 scenarios executed, 0 failures, 0 errors, 0 skipped.

## What the suite is proving

This project is intentionally structured as a QA safety net: it verifies the expected Booking API contract, then checks whether the live public sandbox violates that contract. The value is not only in confirming the happy path, but in proving the team can catch regressions that would otherwise reach production.

The happy-path journey passed end to end: valid create/read/PUT/PATCH/delete flows, token retrieval, auth rejection, unknown-ID handling, list filtering, and the zero-price boundary all behaved as expected for the current environment.

## Known defect findings captured by the suite

The suite also encodes the business rules that should reject invalid payloads. The live sandbox currently violates several of those rules, which is exactly why this assignment exists:

- Missing or malformed required fields can trigger `500 Internal Server Error` instead of `400 Bad Request`.
- Negative totals and wrong-type totals can be accepted with `200 OK`.
- Reversed or malformed dates can be accepted instead of being rejected.
- Wrong-type booleans such as `depositpaid: 1` can be silently coerced to `true`.

These are not framework problems; they are live product defects, and they are captured in `BUGS.md` with reproduction steps and expected-vs-actual behavior.

## Why this matters

The suite is valuable because it does not simply assert "the API returns something". It asserts the contract the business actually expects:
- invalid booking data should not be accepted
- required fields should be enforced
- dates must be valid and logical
- negative pricing must not reach storage
- auth must be enforced for mutating operations

When the public sandbox violates those rules, the result is treated as a defect finding rather than a false alarm.

## Attached artifacts

- `cucumber-report.html`: stakeholder-friendly per-scenario report with tags and actionable assertions
- `cucumber.json`: machine-readable Cucumber execution results
- `surefire-tests.xml`: Maven/JUnit summary
- `execution-log.txt`: raw request/response evidence, including status codes and payloads; auth tokens are redacted

These artifacts make the work reviewable by both engineering and non-QA stakeholders without needing a live rerun.
