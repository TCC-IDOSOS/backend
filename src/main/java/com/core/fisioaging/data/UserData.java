package com.core.fisioaging.data;

import com.core.fisioaging.enums.GenreEnum;
import com.core.fisioaging.enums.ProfileEnum;
import com.core.fisioaging.model.AddressModel;

import java.util.Date;
import java.util.Locale;

public class UserData {

    private Long id;
    private String name;
    private String cpf;
    private GenreEnum genre;
    private Date birthDate;
    private String email;
    private String password;
    private ProfileEnum profile;
    private AddressData address;


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GenreEnum getGenre() {
        return genre;
    }

    public void setGenre(GenreEnum genre) {
        this.genre = genre;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
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

    public AddressData getAddress() {
        return address;
    }

    public void setAddress(AddressData address) {
        this.address = address;
    }
}
