package com.bybud.authgateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the Gateway
 * This filter runs before security to handle CORS preflight requests
 */
@Configuration
public class CorsGatewayConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(CorsGatewayConfiguration.class);

    // List of allowed origins must match exactly what's in the request
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://production-frontend.com"
    );

    private static final String ALLOWED_HEADERS = "x-requested-with, authorization, content-type, credential, X-AUTH-TOKEN, X-CSRF-TOKEN, X-USER-ID, X-USER-NAME, X-USER-FULL-NAME, X-USER-ROLES";
    private static final String ALLOWED_METHODS = "GET, PUT, POST, DELETE, OPTIONS";
    private static final String MAX_AGE = "3600";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            if (CorsUtils.isCorsRequest(request)) {
                HttpHeaders headers = response.getHeaders();
                String origin = request.getHeaders().getOrigin();

                logger.debug("CORS request detected. Origin: {} Method: {}", origin, request.getMethod());

                if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
                    headers.setAccessControlAllowOrigin(origin);
                    headers.setAccessControlAllowCredentials(true);
                    headers.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList(ALLOWED_METHODS.split(", ")));
                    headers.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, Arrays.asList(ALLOWED_HEADERS.split(", ")));
                    headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, MAX_AGE);
                    headers.addAll(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                            Arrays.asList("Authorization", "Content-Type", "X-USER-ID", "X-USER-NAME", "X-USER-ROLES"));

                    logger.debug("CORS headers set for origin: {}", origin);

                    if (request.getMethod() == HttpMethod.OPTIONS) {
                        response.setStatusCode(HttpStatus.OK);
                        logger.debug("Handling OPTIONS preflight request with 200 OK");
                        return Mono.empty();
                    }
                } else {
                    logger.debug("Origin not allowed: {}", origin);
                }
            }

            return chain.filter(exchange);
        };
    }
}