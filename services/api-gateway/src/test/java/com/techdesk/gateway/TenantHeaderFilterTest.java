package com.techdesk.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class TenantHeaderFilterTest {

    private TenantHeaderFilter filter;
    private final ServerHttpRequest[] capturedRequest = new ServerHttpRequest[1];
    private GatewayFilterChain chain;

    @BeforeEach
    public void setUp() {
        filter = new TenantHeaderFilter();
        capturedRequest[0] = null;
        chain = exchange -> {
            capturedRequest[0] = exchange.getRequest();
            return Mono.empty();
        };
    }

    @Test
    public void testSubdomainResolution() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://company-a.techdesk.com/api/tickets")
                .header(HttpHeaders.HOST, "company-a.techdesk.com")
                .build();
        
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertNotNull(capturedRequest[0], "Chain should have been called");
        String tenantHeader = capturedRequest[0].getHeaders().getFirst("X-Tenant-ID");
        assertEquals("company-a", tenantHeader);
    }

    @Test
    public void testJwtResolution() {
        String payload = "{\"tenant\":\"company-b\"}";
        String base64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String dummyJwt = "header." + base64Payload + ".signature";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://techdesk.com/api/tickets")
                .header(HttpHeaders.HOST, "techdesk.com")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + dummyJwt)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertNotNull(capturedRequest[0]);
        String tenantHeader = capturedRequest[0].getHeaders().getFirst("X-Tenant-ID");
        assertEquals("company-b", tenantHeader);
    }

    @Test
    public void testPrecedenceSubdomainOverJwt() {
        String payload = "{\"tenant\":\"company-jwt\"}";
        String base64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String dummyJwt = "header." + base64Payload + ".signature";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://company-subdomain.techdesk.com/api/tickets")
                .header(HttpHeaders.HOST, "company-subdomain.techdesk.com")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + dummyJwt)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertNotNull(capturedRequest[0]);
        String tenantHeader = capturedRequest[0].getHeaders().getFirst("X-Tenant-ID");
        assertEquals("company-subdomain", tenantHeader); // Subdomain takes precedence
    }

    @Test
    public void testNoTenantResolution() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://techdesk.com/api/tickets")
                .header(HttpHeaders.HOST, "techdesk.com")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertNotNull(capturedRequest[0]);
        String tenantHeader = capturedRequest[0].getHeaders().getFirst("X-Tenant-ID");
        assertNull(tenantHeader, "Tenant header should be null if not resolved");
    }
}
