package com.core.fisioaging.mapper;

import com.core.fisioaging.data.HealthUnitData;

import com.core.fisioaging.model.AddressModel;
import com.core.fisioaging.model.HealthUnitModel;


import java.util.Objects;

public class HealthUnitMapper {

    public HealthUnitMapper() {
    }

    public static HealthUnitData toData(final HealthUnitModel model) {
        if (Objects.isNull(model)) {
            return null;
        }

        final HealthUnitData data = new HealthUnitData();
        data.setId(model.getId());
        data.setName(model.getName());
        data.setCnpj(model.getCnpj());
        data.setPhone(model.getPhone());
        data.setEmail(model.getEmail());
        data.setAddress(AddressMapper.toData(model.getAddress()));

        return data;
    }

    public static HealthUnitModel toModel(final HealthUnitData data) {
        if (Objects.isNull(data)) {
            return null;
        }

        final HealthUnitModel model = new HealthUnitModel();
        model.setId(data.getId());
        model.setName(data.getName());
        model.setCnpj(data.getCnpj());
        model.setEmail(data.getEmail());
        model.setPhone(data.getPhone());
        model.setAddress(AddressMapper.toModel(data.getAddress()));

        return model;
    }

    public static void updateModel(final HealthUnitModel model, final HealthUnitData data) {
        model.setId(data.getId());
        model.setName(data.getName());
        model.setCnpj(data.getCnpj());
        model.setEmail(data.getEmail());
        model.setPhone(data.getPhone());
        model.setAddress(AddressMapper.toModel(data.getAddress()));

        if (data.getAddress() != null) {
            AddressModel address = model.getAddress();
            if (address == null) {
                address = new AddressModel();
                model.setAddress(address);
            }
            AddressMapper.updateModel(address, data.getAddress());
        } else {
            model.setAddress(null);
        }
    }
}
