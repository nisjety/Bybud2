package com.bybud.entity.dto;

import com.bybud.entity.model.RoleName;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserDTO {

    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    private String phoneNumber;

    private Set<RoleName> roles;

    private String email;

    public UpdateUserDTO() {}

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
