package com.core.fisioaging.model;

import com.core.fisioaging.enums.GenreEnum;
import com.core.fisioaging.enums.ProfileEnum;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "users")
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String cpf;

    @Enumerated(EnumType.STRING)
    private GenreEnum genre;

    private LocalDate birthDate;

    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private ProfileEnum profile;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private AddressModel address;
}
