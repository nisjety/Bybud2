package com.bybud.entity.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User extends BaseEntity {

    private String username;
    private String email;
    @JsonProperty("hashedPassword")
    private String password;
    private String fullName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String roles;  // Stored as CSV in DB
    private boolean active = true;

    public User() {}

    // Getters and setters

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<RoleName> getRoles() {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .map(RoleName::valueOf)
                .collect(Collectors.toSet());
    }

    public void setRoles(Set<RoleName> rolesSet) {
        this.roles = rolesSet == null || rolesSet.isEmpty()
                ? ""
                : rolesSet.stream()
                .map(RoleName::name)
                .collect(Collectors.joining(","));
    }
}
