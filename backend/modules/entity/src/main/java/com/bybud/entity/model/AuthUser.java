package com.bybud.entity.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Set;

@Document(collection = "auth")
public class AuthUser {
    @Id
    private String id;
    private String username;
    private String password;
    private Set<RoleName> roles;

    // Getters and setters...

        public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Set<RoleName> getRoles() {
        return roles;
    }
    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }
}