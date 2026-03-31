package com.core.fisioaging.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "addresses")
public class AddressModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String streetName;
    private String streetNumber;
    private String complement;
    private String city;
    private String state;
    private String zipCode;
}
