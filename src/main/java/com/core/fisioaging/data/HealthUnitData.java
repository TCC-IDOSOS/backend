package com.core.fisioaging.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HealthUnitData {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String name;
    private String cnpj;
    private String phone;
    private String email;
    private AddressData address;
}
