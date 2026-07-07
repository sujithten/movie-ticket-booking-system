package com.example.movietickets.integration;

import com.example.movietickets.dto.request.CityRequest;
import com.example.movietickets.exception.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec §14: malformed request -> 400 with a structured error body; plus a basic CRUD round trip. */
class ValidationAndCrudIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin@example.com", "AdminPass123!");
        return headers;
    }

    @Test
    void blankCityNameIsRejectedWithStructuredError() {
        HttpEntity<CityRequest> request = new HttpEntity<>(new CityRequest(""), adminHeaders());

        ResponseEntity<ApiError> response = restTemplate.postForEntity("/cities", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    void adminCanCreateThenReadCity() {
        HttpEntity<CityRequest> request = new HttpEntity<>(new CityRequest("Round Trip City"), adminHeaders());
        ResponseEntity<String> created = restTemplate.postForEntity("/cities", request, String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> listed = restTemplate.getForEntity("/cities", String.class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).contains("Round Trip City");
    }
}
