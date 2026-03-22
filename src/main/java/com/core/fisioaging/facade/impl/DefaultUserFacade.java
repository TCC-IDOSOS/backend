package com.core.fisioaging.facade.impl;


import com.core.fisioaging.data.UserData;
import com.core.fisioaging.facade.UserFacade;
import com.core.fisioaging.mapper.UserMapper;
import com.core.fisioaging.model.UserModel;
import com.core.fisioaging.service.UserService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DefaultUserFacade implements UserFacade {

    private final UserService userService;

    public DefaultUserFacade(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserData create(final UserData userData) {
        final UserModel userModel = UserMapper.toModel(userData);
        final UserModel savedUser = userService.create(userModel);
        return UserMapper.toData(savedUser);
    }

    @Override
    public UserData update(final Long id, final UserData userData) {
        final UserModel userModel = UserMapper.toModel(userData);
        final UserModel updatedUser = userService.update(id, userModel);
        return UserMapper.toData(updatedUser);
    }

    @Override
    public UserData getById(final Long id) {
        return UserMapper.toData(userService.findById(id));
    }

    @Override
    public List<UserData> getAll() {
        return userService.findAll()
                .stream()
                .map(UserMapper::toData)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final Long id) {
        userService.delete(id);
    }
}
