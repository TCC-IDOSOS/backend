package com.core.fisioaging.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "health_unit")
public class HealthUnitModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String cnpj;
    private String phone;
    private String email;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private AddressModel address;
}
