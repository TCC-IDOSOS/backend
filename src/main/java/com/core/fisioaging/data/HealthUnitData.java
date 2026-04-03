package com.core.fisioaging.data;

import lombok.Data;

@Data
public class HealthUnitData {
    private Long id;
    private String name;
    private String cnpj;
    private String phone;
    private String email;
    private AddressData address;
}
