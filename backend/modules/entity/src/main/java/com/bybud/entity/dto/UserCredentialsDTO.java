package com.bybud.entity.dto;

import com.bybud.entity.model.RoleName;
import java.util.Set;

public class UserCredentialsDTO {
    private String id;
    private String username;
    private String hashedPassword;
    private Set<RoleName> roles;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }

    public Set<RoleName> getRoles() { return roles; }
    public void setRoles(Set<RoleName> roles) { this.roles = roles; }
}
