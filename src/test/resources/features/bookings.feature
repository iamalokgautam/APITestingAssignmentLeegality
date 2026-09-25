Feature: Booking API safety net
  Background:
    Given the booking API is available

  @smoke @crud @P1
  Scenario: Create, read, update, and cancel a booking
    When I create a valid booking
    Then the booking is created with a valid response contract
    When I read that booking
    Then the booking body matches the created data
    When I update that booking with PUT
    Then the update is accepted and persisted
    When I partially update that booking with PATCH using a token
    Then the partial update preserves other fields
    When I delete that booking
    Then the booking is deleted and cannot be read

  @auth @P1
  Scenario: Retrieve an authentication token
    When I request an authentication token
    Then the token response has a non-empty token

  @auth @negative @P1
  Scenario: PUT rejects missing credentials without mutating a booking
    Given I have a booking for authorization checks
    When I try to replace that booking without credentials
    Then the PUT is rejected and the original booking remains

  @auth @negative @P1
  Scenario: DELETE rejects missing credentials without deleting a booking
    Given I have a booking for authorization checks
    When I try to delete that booking without credentials
    Then the DELETE is rejected and the booking remains

  @auth @negative @P1
  Scenario: PATCH rejects an invalid token without mutating a booking
    Given I have a booking for authorization checks
    When I try to patch that booking with an invalid token
    Then the PATCH is rejected and the original booking remains

  @create @negative @P1
  Scenario Outline: Invalid booking payloads are rejected
    When I create a booking with payload '<payload>'
    Then the request is rejected with status 400
    Examples:
      | payload |
      | {} |
      | {"firstname":"OnlyName"} |
      | {"firstname":123,"lastname":"Bad","totalprice":20,"depositpaid":true,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}} |
      | {"firstname":"Bad","lastname":"Price","totalprice":"twenty","depositpaid":true,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}} |
      | {"firstname":"Bad","lastname":"Deposit","totalprice":20,"depositpaid":1,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}} |
      | {"firstname":"Bad","lastname":"Price","totalprice":-1,"depositpaid":true,"bookingdates":{"checkin":"2030-12-01","checkout":"2030-12-03"}} |
      | {"firstname":"Bad","lastname":"Date","totalprice":25,"depositpaid":true,"bookingdates":{"checkin":"2030-12-10","checkout":"2030-12-01"}} |
      | {"firstname":"Bad","lastname":"Date","totalprice":25,"depositpaid":true,"bookingdates":{"checkin":"not-a-date","checkout":"2030-12-03"}} |

  @read @negative @P2
  Scenario: Unknown booking ID returns not found
    When I read booking 99999999
    Then the booking does not exist

  @create @read @P2
  Scenario: Booking list can filter to a uniquely created booking
    When I list bookings filtered by a unique created name
    Then the list includes the newly created booking ID

  @create @boundary @P2
  Scenario: Zero total price is accepted as a nonnegative boundary
    When I create a booking with zero price
    Then the zero-price booking is accepted

  @create @boundary @P1
  Scenario: Checkout before check-in is rejected
    When I create a booking with reversed stay dates
    Then the booking is rejected with status 400
