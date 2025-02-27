package com.bybud.entity.dto;

import com.bybud.entity.model.RoleName;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * DTO used to create a new user.
 * This class is fully compatible with reactive flows as it only serves as a data carrier.
 */
public class CreateUserDTO {

    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    private String password;

    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "Phone number must be between 7 and 15 digits.")
    private String phoneNumber;

    @NotNull(message = "Role is required.")
    private Set<RoleName> roles;

    // Getters and Setters
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

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
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

    public Set<RoleName> getRoles() {
        return roles;
    }
    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }
}
