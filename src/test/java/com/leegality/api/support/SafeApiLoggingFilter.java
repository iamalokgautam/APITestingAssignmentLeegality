package com.leegality.api.support;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Logs actionable request/response evidence without exposing credentials or tokens. */
public final class SafeApiLoggingFilter implements Filter {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public Response filter(
            FilterableRequestSpecification request,
            FilterableResponseSpecification responseSpecification,
            FilterContext context) {
        String uri = request.getURI();
        boolean authRequest = uri.endsWith("/auth");

        System.out.printf("%n>>> %s %s%n", request.getMethod(), uri);
        System.out.println("Request headers: Accept: application/json; Content-Type: application/json");
        if (authRequest) {
            System.out.println("Request body: [authentication payload redacted]");
        } else {
            Object body = request.getBody();
            System.out.println("Request body: " + jsonBody(body));
        }

        Response response = context.next(request, responseSpecification);
        String responseBody = response.asString();
        if (authRequest) {
            responseBody = responseBody.replaceAll(
                    "\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"[REDACTED]\"");
        }
        System.out.printf("<<< HTTP %d%n", response.statusCode());
        System.out.println("Response body: " + (responseBody.isEmpty() ? "<empty>" : responseBody));
        return response;
    }

    private static String jsonBody(Object body) {
        if (body == null) {
            return "<none>";
        }
        if (body instanceof String text) {
            return text;
        }
        try {
            return JSON.writeValueAsString(body);
        } catch (JsonProcessingException serializationError) {
            return "[request body could not be serialized for logging]";
        }
    }
}
