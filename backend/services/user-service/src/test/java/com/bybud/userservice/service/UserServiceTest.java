package com.bybud.userservice.service;

import com.bybud.common.exception.UserNotFoundException;
import com.bybud.entity.dto.CreateUserDTO;
import com.bybud.entity.dto.UpdateUserDTO;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.mapper.UserMapper;
import com.bybud.entity.repository.UserRepository;
import com.bybud.kafka.handler.UserEventHandler;
import com.bybud.entity.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventHandler eventHandler;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Test for successful user creation
    @Test
    public void testCreateUser_Success() {
        CreateUserDTO createUserDTO = new CreateUserDTO();
        createUserDTO.setUsername("testuser");
        createUserDTO.setPassword("rawpassword");
        createUserDTO.setEmail("test@example.com");

        // Simulate password encoding and existence checks
        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedPassword");
        when(userRepository.existsByUsername("testuser")).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(Mono.just(false));

        // Simulate mapping to user entity and saving
        User dummyUser = new User();
        dummyUser.setId("user1");
        dummyUser.setUsername("testuser");
        dummyUser.setEmail("test@example.com");
        dummyUser.setPassword("encodedPassword");

        when(userMapper.toUser(createUserDTO)).thenReturn(dummyUser);
        when(userRepository.save(dummyUser)).thenReturn(Mono.just(dummyUser));

        // Simulate mapping to user DTO
        UserDTO dummyUserDTO = new UserDTO();
        dummyUserDTO.setId("user1");
        dummyUserDTO.setUsername("testuser");
        dummyUserDTO.setEmail("test@example.com");

        when(userMapper.toUserDTO(dummyUser)).thenReturn(dummyUserDTO);

        Mono<UserDTO> resultMono = userService.createUser(createUserDTO);

        StepVerifier.create(resultMono)
                .expectNext(dummyUserDTO)
                .verifyComplete();

        // Await until the asynchronous publishUserCreated call is observed
        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventHandler, times(1))
                        .publishUserCreated(any(UserEventHandler.UserCreatedEvent.class)));
    }

    // Test for user creation when username already exists
    @Test
    public void testCreateUser_UsernameExists() {
        CreateUserDTO createUserDTO = new CreateUserDTO();
        createUserDTO.setUsername("existingUser");
        createUserDTO.setPassword("rawpassword");
        createUserDTO.setEmail("test@example.com");

        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedPassword");
        when(userRepository.existsByUsername("existingUser")).thenReturn(Mono.just(true));

        Mono<UserDTO> resultMono = userService.createUser(createUserDTO);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                                "Username is already in use".equals(((ResponseStatusException) throwable).getReason())
                )
                .verify();
    }

    // Test for user creation when email already exists
    @Test
    public void testCreateUser_EmailExists() {
        CreateUserDTO createUserDTO = new CreateUserDTO();
        createUserDTO.setUsername("newuser");
        createUserDTO.setPassword("rawpassword");
        createUserDTO.setEmail("existing@example.com");

        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedPassword");
        when(userRepository.existsByUsername("newuser")).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(Mono.just(true));

        Mono<UserDTO> resultMono = userService.createUser(createUserDTO);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                                "Email is already in use".equals(((ResponseStatusException) throwable).getReason())
                )
                .verify();
    }

    // Test for successful user profile update
    @Test
    public void testUpdateUserProfile_Success() {
        String userId = "user1";
        UpdateUserDTO updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setPassword("newPassword");
        updateUserDTO.setEmail("new@example.com");

        // Existing user in repository
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("testuser");
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("oldPassword");

        when(userRepository.findById(userId)).thenReturn(Mono.just(existingUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // Simulate updating the user
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("new@example.com");
        updatedUser.setPassword("encodedNewPassword");

        when(userMapper.updateUser(existingUser, updateUserDTO)).thenReturn(updatedUser);
        when(userRepository.save(updatedUser)).thenReturn(Mono.just(updatedUser));

        UserDTO updatedUserDTO = new UserDTO();
        updatedUserDTO.setId(userId);
        updatedUserDTO.setUsername("testuser");
        updatedUserDTO.setEmail("new@example.com");

        when(userMapper.toUserDTO(updatedUser)).thenReturn(updatedUserDTO);

        Mono<UserDTO> resultMono = userService.updateUserProfile(userId, updateUserDTO);

        StepVerifier.create(resultMono)
                .expectNext(updatedUserDTO)
                .verifyComplete();

        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventHandler, times(1))
                        .publishUserUpdated(any(UserEventHandler.UserUpdatedEvent.class)));
    }

    // Test for update when the user is not found
    @Test
    public void testUpdateUserProfile_UserNotFound() {
        String userId = "nonexistent";
        UpdateUserDTO updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setPassword("newPassword");
        updateUserDTO.setEmail("new@example.com");

        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        Mono<UserDTO> resultMono = userService.updateUserProfile(userId, updateUserDTO);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof UserNotFoundException &&
                                Objects.equals(((ResponseStatusException) throwable).getReason(), "User not found with ID: " + userId)
                )
                .verify();
    }

    // Test for retrieving all users
    @Test
    public void testGetAllUsers() {
        User user1 = new User();
        user1.setId("user1");
        user1.setUsername("testuser1");

        User user2 = new User();
        user2.setId("user2");
        user2.setUsername("testuser2");

        when(userRepository.findAll()).thenReturn(Flux.just(user1, user2));

        UserDTO userDTO1 = new UserDTO();
        userDTO1.setId("user1");
        userDTO1.setUsername("testuser1");

        UserDTO userDTO2 = new UserDTO();
        userDTO2.setId("user2");
        userDTO2.setUsername("testuser2");

        when(userMapper.toUserDTO(user1)).thenReturn(userDTO1);
        when(userMapper.toUserDTO(user2)).thenReturn(userDTO2);

        Mono<List<UserDTO>> resultMono = userService.getAllUsers();

        StepVerifier.create(resultMono)
                .expectNextMatches(list -> list.size() == 2 &&
                        list.contains(userDTO1) && list.contains(userDTO2))
                .verifyComplete();
    }

    // Test for retrieving a user by ID successfully
    @Test
    public void testGetUserById_Success() {
        String userId = "user1";
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setUsername("testuser");

        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

        Mono<UserDTO> resultMono = userService.getUserById(userId);

        StepVerifier.create(resultMono)
                .expectNext(userDTO)
                .verifyComplete();
    }

    // Test for retrieving a user by ID when not found
    @Test
    public void testGetUserById_NotFound() {
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        Mono<UserDTO> resultMono = userService.getUserById(userId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof UserNotFoundException &&
                                Objects.equals(((ResponseStatusException) throwable).getReason(), "User not found with ID: " + userId)
                )
                .verify();
    }

    // Test for retrieving user details by username (when found by username)
    @Test
    public void testGetUserDetails_Success_ByUsername() {
        String username = "testuser";
        User user = new User();
        user.setId("user1");
        user.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(userRepository.findByEmail(username)).thenReturn(Mono.empty());

        UserDTO userDTO = new UserDTO();
        userDTO.setId("user1");
        userDTO.setUsername(username);

        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

        Mono<UserDTO> resultMono = userService.getUserDetails(username);

        StepVerifier.create(resultMono)
                .expectNext(userDTO)
                .verifyComplete();
    }

    // Test for retrieving user details by email (when username lookup fails)
    @Test
    public void testGetUserDetails_Success_ByEmail() {
        String email = "test@example.com";
        when(userRepository.findByUsername(email)).thenReturn(Mono.empty());

        User user = new User();
        user.setId("user1");
        user.setUsername("testuser");
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));

        UserDTO userDTO = new UserDTO();
        userDTO.setId("user1");
        userDTO.setUsername("testuser");
        userDTO.setEmail(email);

        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

        Mono<UserDTO> resultMono = userService.getUserDetails(email);

        StepVerifier.create(resultMono)
                .expectNext(userDTO)
                .verifyComplete();
    }

    // Test for retrieving user details when no user is found
    @Test
    public void testGetUserDetails_NotFound() {
        String identifier = "unknown";
        when(userRepository.findByUsername(identifier)).thenReturn(Mono.empty());
        when(userRepository.findByEmail(identifier)).thenReturn(Mono.empty());

        Mono<UserDTO> resultMono = userService.getUserDetails(identifier);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof UserNotFoundException &&
                                Objects.equals(((ResponseStatusException) throwable).getReason(), "User not found with username or email: " + identifier)
                )
                .verify();
    }

    // New tests for getUserCredentials

    @Test
    public void testGetUserCredentials_Success() {
        String usernameOrEmail = "testuser";
        User user = new User();
        user.setId("user1");
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        // Assume roles are set as needed, e.g., Set.of(RoleName.CUSTOMER)

        when(userRepository.findByUsername(usernameOrEmail)).thenReturn(Mono.just(user));
        when(userRepository.findByEmail(usernameOrEmail)).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserCredentials(usernameOrEmail))
                .assertNext(credentials -> {
                    assertEquals("user1", credentials.getId());
                    assertEquals("testuser", credentials.getUsername());
                    assertEquals("encodedPassword", credentials.getHashedPassword());
                    // Optionally, verify roles if applicable
                })
                .verifyComplete();
    }

    @Test
    public void testGetUserCredentials_NotFound() {
        String identifier = "unknown";
        when(userRepository.findByUsername(identifier)).thenReturn(Mono.empty());
        when(userRepository.findByEmail(identifier)).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserCredentials(identifier))
                .expectErrorMatches(throwable ->
                        throwable instanceof UserNotFoundException &&
                                Objects.equals(((ResponseStatusException) throwable).getReason(), "User not found with username or email: " + identifier)
                )
                .verify();
    }
}
