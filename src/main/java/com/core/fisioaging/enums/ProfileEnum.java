package com.core.fisioaging.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProfileEnum {
    
    ADMIN("Admin"),
    PROFISSIONAL("Profissional"),
    PACIENTE("Paciente");

    private final String descricao;

    ProfileEnum(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return this.descricao;
    }
}
