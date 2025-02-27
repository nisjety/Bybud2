package com.bybud.entity.response;

import com.bybud.entity.model.RoleName;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

/**
 * Response DTO for authentication operations.
 * Used in a reactive controller, wrapped in a Mono.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private Set<RoleName> roles;

    public AuthResponse(String accessToken, String refreshToken, Set<RoleName> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.roles = roles;
    }

    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }
    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }
}
