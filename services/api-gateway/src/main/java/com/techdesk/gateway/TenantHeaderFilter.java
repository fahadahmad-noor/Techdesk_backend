package com.techdesk.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TenantHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantHeaderFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tenantId = null;

        // 1. Try resolving from subdomain
        String host = request.getHeaders().getFirst(HttpHeaders.HOST);
        if (host != null) {
            tenantId = extractTenantFromHost(host);
        }

        // 2. Try resolving from Authorization JWT if not found in subdomain
        if (tenantId == null) {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tenantId = extractTenantFromJwt(token);
            }
        }

        if (tenantId != null) {
            log.info("Resolved tenant ID: {} for path: {}", tenantId, request.getPath());
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Tenant-ID", tenantId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }

    private String extractTenantFromHost(String host) {
        String cleanHost = host.split(":")[0]; // remove port if present
        String[] parts = cleanHost.split("\\.");
        
        // E.g. company-a.techdesk.com -> parts = ["company-a", "techdesk", "com"] (length = 3)
        // E.g. company-a.localhost -> parts = ["company-a", "localhost"] (length = 2)
        if (parts.length >= 2) {
            String firstPart = parts[0];
            // Filter out default hosts/domains
            if (!firstPart.equalsIgnoreCase("www") && 
                !firstPart.equalsIgnoreCase("api") && 
                !firstPart.equalsIgnoreCase("localhost") &&
                !firstPart.equalsIgnoreCase("127") &&
                !firstPart.equalsIgnoreCase("techdesk")) {
                return firstPart;
            }
        }
        return null;
    }

    private String extractTenantFromJwt(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length >= 2) {
                String payloadBase64 = parts[1];
                byte[] decodedBytes = Base64.getUrlDecoder().decode(payloadBase64);
                String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
                
                JsonNode node = objectMapper.readTree(payloadJson);
                if (node.has("tenant")) {
                    return node.get("tenant").asText();
                } else if (node.has("tenantId")) {
                    return node.get("tenantId").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract tenant from JWT", e);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
