package com.bybud.entity.mapper;

import com.bybud.entity.dto.CreateUserDTO;
import com.bybud.entity.dto.UpdateUserDTO;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class UserMapper {

    public User toUser(CreateUserDTO dto) {
        User user = new User();
        mapBasicUserInfo(user, dto);
        user.setPassword(dto.getPassword());
        user.setRoles(getDefaultRoles(dto.getRoles()));
        user.setActive(true);
        return user;
    }

    public User updateUserFromDTO(User user, UserDTO dto) {
        mapBasicUserInfo(user, dto);
        user.setId(dto.getId());
        user.setActive(dto.isActive());
        user.setRoles(dto.getRoles());
        return user;
    }

    public User updateUser(User user, UpdateUserDTO dto) {
        if (dto.getPassword() != null) {
            user.setPassword(dto.getPassword());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            user.setRoles(dto.getRoles());
        }
        return user;
    }

    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        mapBasicDTOInfo(dto, user);
        dto.setId(user.getId());
        dto.setActive(user.isActive());
        dto.setRoles(user.getRoles());
        return dto;
    }

    private void mapBasicUserInfo(User user, CreateUserDTO dto) {
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setPhoneNumber(dto.getPhoneNumber());
    }

    private void mapBasicUserInfo(User user, UserDTO dto) {
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setPhoneNumber(dto.getPhoneNumber());
    }

    private void mapBasicDTOInfo(UserDTO dto, User user) {
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setPhoneNumber(user.getPhoneNumber());
    }

    private Set<RoleName> getDefaultRoles(Set<RoleName> roles) {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>(Collections.singletonList(RoleName.CUSTOMER));
        }
        return roles;
    }
}
