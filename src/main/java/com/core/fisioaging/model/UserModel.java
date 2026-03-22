package com.core.fisioaging.model;

import com.core.fisioaging.enums.GenreEnum;
import com.core.fisioaging.enums.ProfileEnum;

import java.util.Date;

public class UserModel {

    private Long id;
    private String name;
    private String cpf;
    private GenreEnum genre;
    private Date birthDate;
    private String email;
    private String password;
    private ProfileEnum profile;
    private AddressModel address;


    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }


    public String getCpf() {
        return cpf;
    }

    public void setCpf(final String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public GenreEnum getGenre() {
        return genre;
    }

    public void setGenre(GenreEnum genre) {
        this.genre = genre;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ProfileEnum getProfile() {
        return profile;
    }

    public void setProfile(ProfileEnum profile) {
        this.profile = profile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public AddressModel getAddress() {
        return address;
    }

    public void setAddress(AddressModel address) {
        this.address = address;
    }
}