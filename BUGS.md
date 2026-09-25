# Reproduced API defects

**Target:** `https://restful-booker.herokuapp.com`  
**Observed:** 25 September 2026. Requests used `Accept: application/json`. Created invalid records were removed by the scenario teardown or direct cleanup after reproduction.

This defect log is written in product terms: the suite is asserting the expected business contract for Booking creation, and the public sandbox currently violates that contract in several places.

## RB-01 - Missing or mistyped required fields produce HTTP 500

**Severity: High** - Client validation failures become server errors, obscuring the cause and generating avoidable 5xx responses.

**Steps to reproduce**

```bash
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{}'
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"firstname":"OnlyName"}'
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"firstname":123,"lastname":"Bad","totalprice":20,"depositpaid":true,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}}'
```

**Expected:** `400 Bad Request` with a validation message; no booking should be created.

**Actual:** `500 Internal Server Error`, body `Internal Server Error`, for all three requests.

## RB-02 - Negative and non-numeric totals are accepted

**Severity: High** - Invalid pricing data can enter the booking store and materially compromise downstream billing and revenue logic.

**Steps to reproduce**

```bash
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"firstname":"Bad","lastname":"Negative","totalprice":-1,"depositpaid":true,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}}'
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"firstname":"Bad","lastname":"PriceType","totalprice":"twenty","depositpaid":true,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}}'
```

**Expected:** `400 Bad Request`; no booking should be created. Zero is allowed by the schema boundary, but negative and non-numeric totals must be rejected.

**Actual:** Both returned `200 OK` and created booking IDs. The negative value was stored as `-1`; the string total was stored/returned as `null`.

## RB-03 - Reversed or malformed dates are accepted

**Severity: High** - Impossible stay intervals and invalid dates can corrupt inventory and downstream availability logic.

**Steps to reproduce**

```bash
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"firstname":"Bad","lastname":"Reversed","totalprice":25,"depositpaid":true,"bookingdates":{"checkin":"2030-12-10","checkout":"2030-12-01"}}'
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"firstname":"Bad","lastname":"Malformed","totalprice":25,"depositpaid":true,"bookingdates":{"checkin":"not-a-date","checkout":"2030-12-03"}}'
```

**Expected:** `400 Bad Request`; dates must be valid and checkout must not precede check-in.

**Actual:** Both returned `200 OK` and created records. The malformed check-in was normalized to `0NaN-aN-aN`.

## RB-04 - Numeric `depositpaid` is silently coerced to true

**Severity: Medium** - Accepting a wrong JSON type can incorrectly mark a deposit as paid and create misleading booking data.

**Reproduction**

```bash
curl -i 'https://restful-booker.herokuapp.com/booking' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"firstname":"Bad","lastname":"DepositType","totalprice":20,"depositpaid":1,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}}'
```

**Expected:** `400 Bad Request`; `depositpaid` must be a JSON boolean.

**Actual:** `200 OK`; the API returned `depositpaid: true`.

## Defect-tracking intent

These are live API defects that the safety-net suite is designed to catch. The project retains detailed request/response evidence in `test-report/execution-log.txt`, and the execution summary in `test-report/README.md` ties the findings back to the business rules being asserted.
