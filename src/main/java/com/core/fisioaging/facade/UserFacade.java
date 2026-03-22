package com.core.fisioaging.facade;

import com.core.fisioaging.data.UserData;

import java.util.List;

public interface UserFacade {

    UserData create(UserData userData);

    UserData update(Long id, UserData userData);

    UserData getById(Long id);

    List<UserData> getAll();

    void delete(Long id);
}
