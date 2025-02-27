package com.bybud.security.config;

import com.bybud.security.filter.AuthTokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

/**
 * Common Web Security Configuration for reactive applications using Spring WebFlux Security.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class CommonWebSecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(CommonWebSecurityConfig.class);

    /**
     * The reactive filter that handles JWT token extraction and validation.
     */
    private final AuthTokenFilter authTokenFilter;

    /**
     * Security properties containing excluded paths configuration.
     */
    private final SecurityProperties securityProperties;

    public CommonWebSecurityConfig(
            @NonNull AuthTokenFilter authTokenFilter,
            @NonNull SecurityProperties securityProperties) {
        this.authTokenFilter = authTokenFilter;
        this.securityProperties = securityProperties;
        logger.info("CommonWebSecurityConfig initialized with excluded paths: {}",
                securityProperties.getExcludedPaths());
    }

    /**
     * Defines the reactive SecurityWebFilterChain.
     * - Disables CSRF, HTTP Basic, and form login for a stateless JWT scenario.
     * - Uses NoOpServerSecurityContextRepository to avoid storing security contexts in session.
     * - Sets up role-based route restrictions.
     * - Adds the AuthTokenFilter at the AUTHENTICATION order.
     */
    @Bean
    @NonNull
    public SecurityWebFilterChain securityWebFilterChain(@NonNull ServerHttpSecurity http) {
        String[] excludedPaths = securityProperties.getExcludedPaths() != null ?
                securityProperties.getExcludedPaths().toArray(new String[0]) :
                new String[0];

        logger.debug("Configuring security with excluded paths: {}", String.join(", ", excludedPaths));

        // Disable CORS in Spring Security (will be handled by CorsGatewayConfiguration)
        http.cors(ServerHttpSecurity.CorsSpec::disable);

        // Disable CSRF for REST APIs
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        // Disable basic auth and form login
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        // Don't store security context (stateless)
        http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        // Authorization rules using non-deprecated approach
        http.authorizeExchange(exchanges -> {
            // Always allow OPTIONS requests for CORS
            exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();

            // Allow excluded paths
            exchanges.pathMatchers(excludedPaths).permitAll();

            // Require authentication for everything else
            exchanges.anyExchange().authenticated();
        });

        // Add JWT filter
        http.addFilterAt(authTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    /**
     * Configures a BCryptPasswordEncoder bean for secure password hashing.
     */
    @Bean
    @NonNull
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}