package com.bybud.authgateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Debug filter that logs detailed information about CORS requests and responses
 * Place this filter BEFORE your CORS filter to see the full request details
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE - 1) // Run just before the CORS filter
public class CorsDebugFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(CorsDebugFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Log request details
        if (logger.isDebugEnabled()) {
            logger.debug("========= CORS DEBUG START =========");
            logger.debug("Request URI: {}", exchange.getRequest().getURI());
            logger.debug("Request Method: {}", exchange.getRequest().getMethod());
            logger.debug("Request Headers:");
            exchange.getRequest().getHeaders().forEach((key, value) ->
                    logger.debug("  {}: {}", key, value));

            // Check if it's a CORS request
            String origin = exchange.getRequest().getHeaders().getOrigin();
            if (origin != null) {
                logger.debug("This is a CORS request from origin: {}", origin);

                // Check if it's a preflight request
                if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)) {
                    logger.debug("This is a PREFLIGHT request");
                    logger.debug("  Access-Control-Request-Method: {}",
                            exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD));
                    if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS)) {
                        logger.debug("  Access-Control-Request-Headers: {}",
                                exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS));
                    }
                }
            }
        }

        return chain.filter(exchange)
                .doOnSuccess(v -> {
                    if (logger.isDebugEnabled()) {
                        // Log response details
                        logger.debug("Response Status: {}", exchange.getResponse().getStatusCode());
                        logger.debug("Response Headers:");
                        exchange.getResponse().getHeaders().forEach((key, value) ->
                                logger.debug("  {}: {}", key, value));
                        logger.debug("========= CORS DEBUG END =========");
                    }
                })
                .doOnError(e -> {
                    logger.error("Error processing request: {}", e.getMessage());
                    logger.debug("========= CORS DEBUG END (WITH ERROR) =========");
                });
    }
}