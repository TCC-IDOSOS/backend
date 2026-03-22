package com.core.fisioaging.service;

import com.core.fisioaging.model.UserModel;

import java.util.List;

public interface UserService {

    UserModel create(UserModel user);

    UserModel update(Long id, UserModel user);

    UserModel findById(Long id);

    List<UserModel> findAll();

    void delete(Long id);
}
