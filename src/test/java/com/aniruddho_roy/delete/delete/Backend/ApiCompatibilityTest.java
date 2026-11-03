package com.aniruddho_roy.delete.delete.Backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ApiCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String URL = "https://github.com/owner/repo/pull/12";
    private static final String MODEL = "c57ab8a2-5622-4b6e-8a01-15041022a819";

    @Test
    void predictionSendsOnlyTheRequiredFieldsAndKeepsAnalyzerResponse() throws Exception {
        String response = """
                {"pr_url":"https://github.com/owner/repo/pull/12",
                 "analysis":{"merge_probability":95,"quality_score":92},
                 "features":{"title":"Fix login"}}
                """;
        Predictions api = new Predictions() {
            @Override
            protected HttpResponse<String> post_authenticated_request(String path, String body) {
                assertEquals("/predict", path);
                try {
                    JsonNode request = mapper.readTree(body);
                    assertEquals(2, request.size());
                    assertEquals(URL, request.path("pullRequestUrl").asText());
                    assertEquals(MODEL, request.path("modelId").asText());
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
                return new Response(200, response);
            }
        };
        assertEquals(mapper.readTree(response), api.predict(URL, MODEL));
    }

    @Test
    void invalidUrlOrMissingModelDoesNotSendARequest() {
        Predictions api = new Predictions() {
            @Override
            protected HttpResponse<String> post_authenticated_request(String path, String body) {
                throw new AssertionError("Invalid input must not reach the backend");
            }
        };
        for (String url : new String[]{null, "", "http://github.com/owner/repo/pull/12",
                "https://example.com/owner/repo/pull/12", "https://github.com/owner/repo",
                "https://github.com/owner/bad repo/pull/12"}) {
            assertThrows(IllegalArgumentException.class, () -> api.predict(url, MODEL));
        }
        for (String model : new String[]{null, "", " "}) {
            assertThrows(IllegalArgumentException.class, () -> api.predict(URL, model));
        }
        assertTrue(Predictions.isValidPullRequestUrl(URL + "/"));
    }

    @Test
    void plansAndSubscriptionsReadCreditsDurationAndExpiry() {
        String plan = """
                {"code":"FREE","name":"Free","credits":5,"duration":1,
                 "description":"5 credits per plan period","models":[{"name":"Model 1"}],
                 "creditsUsed":2,"creditsRemaining":3}
                """;
        Subscription api = new Subscription() {
            @Override
            protected HttpResponse<String> get_authenticated_request(String path) {
                return new Response(200, switch (path) {
                    case "/plans" -> "[" + plan + "]";
                    case "/active-plan" -> plan;
                    case "/subscription" -> """
                            {"status":"ACTIVE","startedAt":"2026-11-03T12:00:00",
                             "endedAt":"2026-11-04T12:00:00"}
                            """;
                    default -> throw new AssertionError("Unexpected endpoint: " + path);
                });
            }
        };
        var available = api.getPlans().getFirst();
        assertEquals(5, available.credits());
        assertEquals(1, available.duration());
        assertEquals(java.util.List.of("Model 1"), available.modelNames());
        var current = api.getCurrentSubscription();
        assertEquals("FREE", current.planCode());
        assertEquals(5, current.credits());
        assertEquals(1, current.duration());
        assertEquals(2, current.creditsUsed());
        assertEquals(3, current.creditsRemaining());
        assertEquals("2026-11-04T12:00:00", current.endedAt());
    }

    private record Response(int statusCode, String body) implements HttpResponse<String> {
        public HttpRequest request() { return null; }
        public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (name, value) -> true); }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public URI uri() { return URI.create("http://localhost"); }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
