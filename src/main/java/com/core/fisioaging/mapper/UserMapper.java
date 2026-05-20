package com.core.fisioaging.mapper;

import com.core.fisioaging.data.UserData;
import com.core.fisioaging.model.AddressModel;
import com.core.fisioaging.model.HealthUnitModel;
import com.core.fisioaging.model.UserModel;

import static java.util.Objects.nonNull;
import static java.util.Objects.isNull;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserData toData(final UserModel model) {
        if (isNull(model)) {
            return null;
        }

        final UserData data = new UserData();
        data.setId(model.getId());
        data.setName(model.getName());
        data.setCpf(model.getCpf());
        data.setGenre(model.getGenre());
        data.setBirthDate(model.getBirthDate());
        data.setEmail(model.getEmail());
        data.setPhone(model.getPhone());
        data.setProfile(model.getProfile());
        data.setAddress(AddressMapper.toData(model.getAddress()));
        data.setHealthUnit(HealthUnitMapper.toData(model.getHealthUnit()));

        return data;
    }

    public static UserModel toModel(final UserData data) {
        if (isNull(data)) {
            return null;
        }

        final UserModel model = new UserModel();
        model.setId(data.getId());
        model.setName(data.getName());
        model.setCpf(data.getCpf());
        model.setGenre(data.getGenre());
        model.setBirthDate(data.getBirthDate());
        model.setEmail(data.getEmail());
        model.setPhone(data.getPhone());
        model.setPassword(data.getPassword());
        model.setProfile(data.getProfile());
        model.setAddress(AddressMapper.toModel(data.getAddress()));
        model.setHealthUnit(HealthUnitMapper.toModel(data.getHealthUnit()));

        return model;
    }

    public static void updateModel(final UserModel model, final UserData data) {
        model.setName(data.getName());
        model.setCpf(data.getCpf());
        model.setGenre(data.getGenre());
        model.setBirthDate(data.getBirthDate());
        model.setEmail(data.getEmail());
        model.setPhone(data.getPhone());
        model.setProfile(data.getProfile());

        if (nonNull(data.getAddress())) {
            AddressModel address = model.getAddress();
            if (nonNull(address)) {
                address = new AddressModel();
                model.setAddress(address);
            }
            AddressMapper.updateModel(address, data.getAddress());
        } else {
            model.setAddress(null);
        }
    }
}
