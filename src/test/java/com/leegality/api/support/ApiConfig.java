package com.leegality.api.support;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** Shared, environment-overridable REST Assured configuration. */
public final class ApiConfig {
    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int SOCKET_TIMEOUT_MS = 10_000;

    private ApiConfig() {
        // Utility class.
    }

    public static String baseUrl() {
        return System.getProperty(
                "baseUrl",
                System.getenv().getOrDefault("BASE_URL", "https://restful-booker.herokuapp.com"));
    }

    public static String username() {
        return System.getenv().getOrDefault("BOOKER_USERNAME", "admin");
    }

    public static String password() {
        return System.getenv().getOrDefault("BOOKER_PASSWORD", "password123");
    }

    public static RequestSpecification request() {
        HttpClientConfig httpClient = HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", CONNECTION_TIMEOUT_MS)
                .setParam("http.socket.timeout", SOCKET_TIMEOUT_MS);
        RestAssuredConfig restAssuredConfig = RestAssuredConfig.config().httpClient(httpClient);

        Filter safeLogger = new SafeApiLoggingFilter();
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl())
                .setContentType(ContentType.JSON)
                // The sandbox rejects REST Assured's broad ContentType.JSON Accept value with 418.
                .setAccept("application/json")
                .setConfig(restAssuredConfig)
                .addFilter(safeLogger)
                .build();
    }
}
