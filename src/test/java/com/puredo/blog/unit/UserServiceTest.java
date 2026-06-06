package com.puredo.blog.unit;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.User.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserServiceImpl userService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setEmail("alice@test.com");
        alice.setPassword("encoded-password");
    }

    // ---- registerUser ----

    @Test
    void registerUser_newUser_returnsPublicDTO() {
        UserDTO.Request.Create request = new UserDTO.Request.Create("alice", "password", "alice@test.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(alice);

        Optional<UserDTO.Response.UsuarioPublico> result = userService.registerUser(request);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_usernameAlreadyExists_returnsEmpty() {
        UserDTO.Request.Create request = new UserDTO.Request.Create("alice", "password", "new@test.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThat(userService.registerUser(request)).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_emailAlreadyExists_returnsEmpty() {
        UserDTO.Request.Create request = new UserDTO.Request.Create("newuser", "password", "alice@test.com");
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));

        assertThat(userService.registerUser(request)).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void registerUser_blankEmail_returnsEmpty(String email) {
        UserDTO.Request.Create request = new UserDTO.Request.Create("newuser", "password", email);
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        assertThat(userService.registerUser(request)).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_nullEmail_returnsEmpty() {
        UserDTO.Request.Create request = new UserDTO.Request.Create("newuser", "password", null);
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        assertThat(userService.registerUser(request)).isEmpty();
        verify(userRepository, never()).save(any());
    }

    // ---- updateUser ----

    @Test
    void updateUser_userFound_updatesAndReturns() {
        UserDTO.Request.Update request = new UserDTO.Request.Update(1L, "alice2", "newpass");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded");
        when(userRepository.save(any())).thenReturn(alice);

        Optional<UserDTO.Response.UsuarioPrivado> result = userService.updateUser(1L, request);

        assertThat(result).isPresent();
        verify(userRepository).save(alice);
    }

    @Test
    void updateUser_userNotFound_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.updateUser(99L, new UserDTO.Request.Update(99L, "x", "y"))).isEmpty();
        verify(userRepository, never()).save(any());
    }

    // ---- updateAvatar ----

    @Test
    void updateAvatar_userFound_setsUrlAndReturns() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(any())).thenReturn(alice);

        Optional<String> result = userService.updateAvatar(1L, "https://s3/avatar.png");

        assertThat(result).isPresent().contains("https://s3/avatar.png");
        assertThat(alice.getAvatarUrl()).isEqualTo("https://s3/avatar.png");
    }

    @Test
    void updateAvatar_userNotFound_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.updateAvatar(99L, "url")).isEmpty();
    }

    // ---- removeAvatar ----

    @Test
    void removeAvatar_userFound_clearsUrlAndReturnsOld() {
        alice.setAvatarUrl("https://s3/old.png");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(any())).thenReturn(alice);

        Optional<String> result = userService.removeAvatar(1L);

        assertThat(result).isPresent().contains("https://s3/old.png");
        assertThat(alice.getAvatarUrl()).isNull();
    }

    @Test
    void removeAvatar_userNotFound_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.removeAvatar(99L)).isEmpty();
    }

    // ---- deleteUserById ----

    @Test
    void deleteUserById_delegatesToRepository() {
        userService.deleteUserById(1L);

        verify(userRepository).deleteById(1L);
    }

    // ---- findByUserName ----

    @Test
    void findByUserName_userFound_returnsUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        Optional<User> result = userService.findByUserName("alice");

        assertThat(result).isPresent().contains(alice);
    }

    @Test
    void findByUserName_userNotFound_returnsEmpty() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(userService.findByUserName("ghost")).isEmpty();
    }

    // ---- searchUsers ----

    @ParameterizedTest
    @ValueSource(strings = {"ali", "Alice", "a"})
    void searchUsers_withQuery_returnsFilteredList(String query) {
        when(userRepository.findByUsernameContainingIgnoreCase(query)).thenReturn(List.of(alice));

        List<UserDTO.Response.UsuarioPublico> result = userService.searchUsers(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void searchUsers_blankQuery_returnsAll(String query) {
        when(userRepository.findAll()).thenReturn(List.of(alice));

        List<UserDTO.Response.UsuarioPublico> result = userService.searchUsers(query);

        assertThat(result).hasSize(1);
        verify(userRepository).findAll();
        verify(userRepository, never()).findByUsernameContainingIgnoreCase(any());
    }

    @Test
    void searchUsers_nullQuery_returnsAll() {
        when(userRepository.findAll()).thenReturn(List.of(alice));

        List<UserDTO.Response.UsuarioPublico> result = userService.searchUsers(null);

        assertThat(result).hasSize(1);
        verify(userRepository).findAll();
    }

    // ---- getUserById / getAllUsers ----

    @Test
    void getUserById_found_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThat(userService.getUserById(1L)).isPresent().contains(alice);
    }

    @Test
    void getUserById_notFound_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.getUserById(99L)).isEmpty();
    }

    @Test
    void getAllUsers_returnsAllUsersFromRepository() {
        when(userRepository.findAll()).thenReturn(List.of(alice));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(1).contains(alice);
    }
}
