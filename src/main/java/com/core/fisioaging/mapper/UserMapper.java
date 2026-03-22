package com.core.fisioaging.mapper;

import com.core.fisioaging.data.UserData;
import com.core.fisioaging.model.UserModel;

import java.util.Objects;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserData toData(final UserModel model) {
        if (Objects.isNull(model)) {
            return null;
        }

        final UserData data = new UserData();
        data.setId(model.getId());
        data.setName(model.getName());
        data.setCpf(model.getCpf());
        data.setGenre(model.getGenre());
        data.setBirthDate(model.getBirthDate());
        data.setEmail(model.getEmail());
        data.setPassword(model.getPassword());
        data.setProfile(model.getProfile());
        data.setAddress(AddressMapper.toData(model.getAddress()));

        return data;
    }

    public static UserModel toModel(final UserData data) {
        if (Objects.isNull(data)) {
            return null;
        }

        final UserModel model = new UserModel();
        model.setId(data.getId());
        model.setName(data.getName());
        model.setCpf(data.getCpf());
        model.setGenre(data.getGenre());
        model.setBirthDate(data.getBirthDate());
        model.setEmail(data.getEmail());
        model.setPassword(data.getPassword());
        model.setProfile(data.getProfile());
        model.setAddress(AddressMapper.toModel(data.getAddress()));

        return model;
    }

    public static void updateModel(final UserModel model, final UserData data) {
        model.setName(data.getName());
        model.setCpf(data.getCpf());
        model.setGenre(data.getGenre());
        model.setBirthDate(data.getBirthDate());
        model.setEmail(data.getEmail());
        model.setPassword(data.getPassword());
        model.setProfile(data.getProfile());
        model.setAddress(AddressMapper.toModel(data.getAddress()));
    }
}
