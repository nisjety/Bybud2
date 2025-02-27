package com.bybud.entity.dto;

import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * DTO representing user information.
 * Contains a helper method to convert this DTO into a User entity.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private boolean active;
    private LocalDate dateOfBirth;
    private Set<RoleName> roles;


    public UserDTO(String id, String username, String email, String fullName,
                   String phoneNumber, boolean active, LocalDate dateOfBirth, Set<RoleName> roles) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.active = active;
        this.dateOfBirth = dateOfBirth;
        this.roles = roles;
    }


    public UserDTO() {
    }

    /**
     * Converts this DTO into a User entity.
     */
    public User toUser() {
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setActive(active);
        user.setDateOfBirth(dateOfBirth);
        user.setRoles(Objects.requireNonNullElseGet(roles, Set::of));
        return user;
    }

    // Getters and Setters
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

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }
    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }
}
