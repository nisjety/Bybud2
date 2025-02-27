package com.bybud.authgateway.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to import the security module components.
 * This ensures that Spring scans the required packages to find the security beans.
 */
@Configuration
@ComponentScan(basePackages = {
        "com.bybud.security.config",
        "com.bybud.security.service"
})
public class SecurityModuleConfig {
    // The @ComponentScan annotation tells Spring to look for components
    // in the specified packages
}