package com.core.fisioaging.mapper;

import com.core.fisioaging.data.AddressData;
import com.core.fisioaging.model.AddressModel;

import java.util.Objects;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static AddressData toData(AddressModel model) {
        if (Objects.isNull(model)) {
            return null;
        }

        AddressData data = new AddressData();
        data.setStreetName(model.getStreetName());
        data.setStreetNumber(model.getStreetNumber());
        data.setComplement(model.getComplement());
        data.setCity(model.getCity());
        data.setState(model.getState());
        data.setZipCode(model.getZipCode());

        return data;
    }

    public static AddressModel toModel(AddressData data) {
        if (Objects.isNull(data)) {
            return null;
        }

        AddressModel model = new AddressModel();
        model.setStreetName(data.getStreetName());
        model.setStreetNumber(data.getStreetNumber());
        model.setComplement(data.getComplement());
        model.setCity(data.getCity());
        model.setState(data.getState());
        model.setZipCode(data.getZipCode());

        return model;
    }

    public static void updateModel(AddressModel model, AddressData data) {
        model.setStreetName(data.getStreetName());
        model.setStreetNumber(data.getStreetNumber());
        model.setComplement(data.getComplement());
        model.setCity(data.getCity());
        model.setState(data.getState());
        model.setZipCode(data.getZipCode());
    }
}
