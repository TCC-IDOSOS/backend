package com.core.fisioaging.data;

import com.core.fisioaging.enums.GenreEnum;
import com.core.fisioaging.enums.ProfileEnum;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserData {

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
