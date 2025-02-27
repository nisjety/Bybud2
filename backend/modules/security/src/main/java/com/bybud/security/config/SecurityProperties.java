package com.bybud.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "bybud.security")
public class SecurityProperties {
    private boolean enabled;
    private String jwtSecret;
    private String jwtRefreshSecret;
    private int jwtExpirationMs;
    private int jwtRefreshExpirationMs;
    private List<String> excludedPaths;

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public String getJwtRefreshSecret() { return jwtRefreshSecret; }
    public void setJwtRefreshSecret(String jwtRefreshSecret) { this.jwtRefreshSecret = jwtRefreshSecret; }

    public int getJwtExpirationMs() { return jwtExpirationMs; }
    public void setJwtExpirationMs(int jwtExpirationMs) { this.jwtExpirationMs = jwtExpirationMs; }

    public int getJwtRefreshExpirationMs() { return jwtRefreshExpirationMs; }
    public void setJwtRefreshExpirationMs(int jwtRefreshExpirationMs) { this.jwtRefreshExpirationMs = jwtRefreshExpirationMs; }

    public List<String> getExcludedPaths() { return excludedPaths; }
    public void setExcludedPaths(List<String> excludedPaths) { this.excludedPaths = excludedPaths; }
}
