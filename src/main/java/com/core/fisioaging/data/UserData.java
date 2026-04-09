package com.core.fisioaging.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.core.fisioaging.enums.GenreEnum;
import com.core.fisioaging.enums.ProfileEnum;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserData {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String name;
    private String cpf;
    private GenreEnum genre;
    private LocalDate birthDate;
    private String email;
    private String password;
    private ProfileEnum profile;
    private AddressData address;
}
