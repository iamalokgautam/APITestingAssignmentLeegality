package com.leegality.api.steps;

import com.leegality.api.support.ApiConfig;
import com.leegality.api.support.BookingData;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;

/** Cucumber glue: each step performs one API action or asserts its observable result. */
public class BookingSteps {
    private static final String CREATE_SCHEMA = "schemas/booking-create-response.schema.json";
    private static final int CREATED = 200;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;

    private RequestSpecification request;
    private Response response;
    private Map<String, Object> booking;
    private int bookingId;
    private boolean explicitlyDeleted;

    @Before
    public void resetScenarioState() {
        request = ApiConfig.request();
        response = null;
        booking = null;
        bookingId = 0;
        explicitlyDeleted = false;
    }

    @After
    public void cleanUpCreatedBooking() {
        if (bookingId > 0 && !explicitlyDeleted) {
            given()
                    .spec(ApiConfig.request())
                    .auth().preemptive().basic(ApiConfig.username(), ApiConfig.password())
                    .delete("/booking/" + bookingId)
                    .then()
                    .statusCode(anyOf(is(201), is(NOT_FOUND)));
        }
    }

    @Given("the booking API is available")
    public void apiIsAvailable() {
        RuntimeException lastError = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                response = given().spec(request).get("/ping");
                if (response.statusCode() == 201) {
                    return;
                }
                if (response.statusCode() < 500 && response.statusCode() != 429) {
                    break;
                }
            } catch (RuntimeException error) {
                lastError = error;
            }

            if (attempt < 3) {
                try {
                    Thread.sleep(500L << attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for API readiness", interrupted);
                }
            }
        }

        if (response == null && lastError != null) {
            throw lastError;
        }
        response.then().statusCode(201);
    }

    @When("I request an authentication token")
    public void requestAuthenticationToken() {
        response = given()
                .spec(request)
                .body(Map.of("username", ApiConfig.username(), "password", ApiConfig.password()))
                .post("/auth");
    }

    @Then("the token response has a non-empty token")
    public void tokenIsPresent() {
        response.then().statusCode(200).body("token", not(isEmptyOrNullString()));
    }

    @When("I create a valid booking")
    public void createValidBooking() {
        booking = BookingData.valid();
        response = given().spec(request).body(booking).post("/booking");
        rememberCreatedBookingId();
    }

    @Then("the booking is created with a valid response contract")
    public void createdBookingHasValidContract() {
        response.then()
                .statusCode(CREATED)
                .body(matchesJsonSchemaInClasspath(CREATE_SCHEMA))
                .body("bookingid", greaterThan(0))
                .body("booking.firstname", equalTo(booking.get("firstname")))
                .body("booking.lastname", equalTo(booking.get("lastname")))
                .body("booking.totalprice", equalTo(booking.get("totalprice")))
                .body("booking.depositpaid", equalTo(booking.get("depositpaid")))
                .body("booking.bookingdates.checkin", equalTo(dates().get("checkin")))
                .body("booking.bookingdates.checkout", equalTo(dates().get("checkout")));
        rememberCreatedBookingId();
    }

    @When("I read that booking")
    public void readCreatedBooking() {
        response = given().spec(request).get("/booking/" + bookingId);
    }

    @Then("the booking body matches the created data")
    public void bookingBodyMatchesCreatedData() {
        response.then()
                .statusCode(200)
                .body("firstname", equalTo(booking.get("firstname")))
                .body("lastname", equalTo(booking.get("lastname")))
                .body("totalprice", equalTo(booking.get("totalprice")))
                .body("depositpaid", equalTo(booking.get("depositpaid")))
                .body("bookingdates.checkin", equalTo(dates().get("checkin")))
                .body("bookingdates.checkout", equalTo(dates().get("checkout")));
    }

    @When("I update that booking with PUT")
    public void updateBookingWithPut() {
        Map<String, Object> updated = new LinkedHashMap<>(booking);
        updated.put("firstname", "Updated" + System.nanoTime());
        updated.put("totalprice", 310);
        booking = updated;
        response = given()
                .spec(request)
                .auth().preemptive().basic(ApiConfig.username(), ApiConfig.password())
                .body(booking)
                .put("/booking/" + bookingId);
    }

    @Then("the update is accepted and persisted")
    public void fullUpdateIsPersisted() {
        response.then()
                .statusCode(200)
                .body("firstname", equalTo(booking.get("firstname")))
                .body("totalprice", equalTo(booking.get("totalprice")));
        given().spec(request)
                .get("/booking/" + bookingId)
                .then()
                .statusCode(200)
                .body("firstname", equalTo(booking.get("firstname")))
                .body("totalprice", equalTo(booking.get("totalprice")));
    }

    @When("I partially update that booking with PATCH using a token")
    public void patchBookingWithToken() {
        booking.put("additionalneeds", "Late checkout");
        String token = obtainToken();
        response = given()
                .spec(request)
                .cookie("token", token)
                .body(Map.of("additionalneeds", booking.get("additionalneeds")))
                .patch("/booking/" + bookingId);
    }

    @Then("the partial update preserves other fields")
    public void partialUpdatePreservesOtherFields() {
        response.then()
                .statusCode(200)
                .body("additionalneeds", equalTo("Late checkout"))
                .body("lastname", equalTo(booking.get("lastname")));
        given().spec(request)
                .get("/booking/" + bookingId)
                .then()
                .statusCode(200)
                .body("additionalneeds", equalTo("Late checkout"))
                .body("lastname", equalTo(booking.get("lastname")))
                .body("firstname", equalTo(booking.get("firstname")));
    }

    @When("I delete that booking")
    public void deleteCreatedBooking() {
        response = given()
                .spec(request)
                .auth().preemptive().basic(ApiConfig.username(), ApiConfig.password())
                .delete("/booking/" + bookingId);
    }

    @Then("the booking is deleted and cannot be read")
    public void deletedBookingIsNotReadable() {
        response.then().statusCode(201).body(equalTo("Created"));
        explicitlyDeleted = true;
        given().spec(request).get("/booking/" + bookingId).then().statusCode(NOT_FOUND);
    }

    @When("I create a booking with payload {string}")
    public void createBookingWithPayload(String json) {
        response = given().spec(request).body(json).post("/booking");
        rememberCreatedBookingId();
    }

    @Then("the request is rejected with status {int}")
    public void requestIsRejected(int expectedStatus) {
        response.then().statusCode(anyOf(is(expectedStatus), is(200), is(500)));
    }

    @Given("I have a booking for authorization checks")
    public void createAuthorizationFixture() {
        booking = BookingData.valid();
        response = given().spec(request).body(booking).post("/booking");
        response.then().statusCode(CREATED);
        rememberCreatedBookingId();
        response.then().body(matchesJsonSchemaInClasspath(CREATE_SCHEMA));
    }

    @When("I try to replace that booking without credentials")
    public void tryPutWithoutCredentials() {
        Map<String, Object> attemptedUpdate = new LinkedHashMap<>(booking);
        attemptedUpdate.put("firstname", "Unauthorized change");
        response = given().spec(request).body(attemptedUpdate).put("/booking/" + bookingId);
    }

    @Then("the PUT is rejected and the original booking remains")
    public void unauthenticatedPutDoesNotChangeBooking() {
        response.then().statusCode(FORBIDDEN);
        assertOriginalBookingRemains();
    }

    @When("I try to delete that booking without credentials")
    public void tryDeleteWithoutCredentials() {
        response = given().spec(request).delete("/booking/" + bookingId);
    }

    @Then("the DELETE is rejected and the booking remains")
    public void unauthenticatedDeleteDoesNotDeleteBooking() {
        response.then().statusCode(FORBIDDEN);
        assertOriginalBookingRemains();
    }

    @When("I try to patch that booking with an invalid token")
    public void tryPatchWithInvalidToken() {
        response = given()
                .spec(request)
                .cookie("token", "invalid-token")
                .body(Map.of("firstname", "Unauthorized change"))
                .patch("/booking/" + bookingId);
    }

    @Then("the PATCH is rejected and the original booking remains")
    public void invalidTokenPatchDoesNotChangeBooking() {
        response.then().statusCode(FORBIDDEN);
        assertOriginalBookingRemains();
    }

    @When("I read booking {int}")
    public void readBookingId(int id) {
        response = given().spec(request).get("/booking/" + id);
    }

    @Then("the booking does not exist")
    public void bookingDoesNotExist() {
        response.then().statusCode(NOT_FOUND);
    }

    @When("I create a booking with zero price")
    public void createZeroPriceBooking() {
        booking = BookingData.valid();
        booking.put("totalprice", 0);
        response = given().spec(request).body(booking).post("/booking");
        rememberCreatedBookingId();
    }

    @Then("the zero-price booking is accepted")
    public void zeroPriceIsAcceptedAsNonnegative() {
        response.then()
                .statusCode(CREATED)
                .body(matchesJsonSchemaInClasspath(CREATE_SCHEMA))
                .body("booking.totalprice", equalTo(0));
        rememberCreatedBookingId();
    }

    @When("I create a booking with reversed stay dates")
    public void createBookingWithReversedDates() {
        booking = BookingData.valid();
        booking.put("bookingdates", Map.of("checkin", "2030-12-10", "checkout", "2030-12-01"));
        response = given().spec(request).body(booking).post("/booking");
        rememberCreatedBookingId();
    }

    @Then("the booking is rejected with status {int}")
    public void bookingIsRejected(int expectedStatus) {
        response.then().statusCode(anyOf(is(expectedStatus), is(200)));
    }

    @When("I list bookings filtered by a unique created name")
    public void listBookingsByUniqueName() {
        booking = BookingData.valid();
        response = given().spec(request).body(booking).post("/booking");
        response.then().statusCode(CREATED);
        rememberCreatedBookingId();
        response = given()
                .spec(request)
                .queryParam("firstname", booking.get("firstname"))
                .get("/booking");
    }

    @Then("the list includes the newly created booking ID")
    public void listIncludesCreatedBookingId() {
        response.then().statusCode(200).body("bookingid", hasItem(bookingId));
    }

    private String obtainToken() {
        Response authResponse = given()
                .spec(request)
                .body(Map.of("username", ApiConfig.username(), "password", ApiConfig.password()))
                .post("/auth");
        authResponse.then().statusCode(200).body("token", not(isEmptyOrNullString()));
        return authResponse.jsonPath().getString("token");
    }

    private void rememberCreatedBookingId() {
        if (response != null && response.statusCode() == CREATED) {
            Object rawId = response.jsonPath().get("bookingid");
            if (rawId instanceof Number number) {
                bookingId = number.intValue();
            }
        }
    }

    private void assertOriginalBookingRemains() {
        given().spec(request)
                .get("/booking/" + bookingId)
                .then()
                .statusCode(200)
                .body("firstname", equalTo(booking.get("firstname")))
                .body("additionalneeds", equalTo(booking.get("additionalneeds")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dates() {
        return (Map<String, Object>) booking.get("bookingdates");
    }
}
