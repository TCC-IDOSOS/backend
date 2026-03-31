package com.core.fisioaging.service;

import com.core.fisioaging.data.UserData;

import java.util.List;

public interface UserService {

    UserData create(UserData userData);

    UserData update(Long id, UserData userData);

    UserData findById(Long id);

    List<UserData> findAll();

    void delete(Long id);
}
