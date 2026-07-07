package com.example.movietickets.integration;

import com.example.movietickets.dto.request.CityRequest;
import com.example.movietickets.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec §14: customer hitting an admin endpoint -> 403; unauthenticated request to a protected endpoint -> 401. */
class RbacIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void customerCannotCreateCity() {
        String email = "customer-" + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/auth/register", new RegisterRequest("Cust", email, "password123"), Void.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(email, "password123");
        HttpEntity<CityRequest> request = new HttpEntity<>(new CityRequest("New City"), headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/cities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointIsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/bookings", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void seededAdminCanCreateCity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin@example.com", "AdminPass123!");
        HttpEntity<CityRequest> request = new HttpEntity<>(new CityRequest("Admin City"), headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/cities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
