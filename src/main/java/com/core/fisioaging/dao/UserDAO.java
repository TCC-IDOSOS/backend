package com.core.fisioaging.dao;

import com.core.fisioaging.model.UserModel;

import java.util.List;
import java.util.Optional;

public interface UserDAO {

    UserModel save(UserModel user);

    Optional<UserModel> findById(Long id);

    List<UserModel> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);
}