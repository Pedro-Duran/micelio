package com.puredo.blog.Service.User;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<UserDTO.Response.UsuarioPublico> registerUser(UserDTO.Request.Create request);
    Optional<UserDTO.Response.UsuarioPrivado> updateUser(Long id, UserDTO.Request.Update request);
    void deleteUserById(Long id);
    Optional<User> findByUserName(String username);
    Optional<User> getUserById(Long id);
    List<User> getAllUsers();
}
