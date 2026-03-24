package com.core.fisioaging.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GenreEnum {
    
    MASCULINO("Masculino"),
    FEMININO("Feminino"),
    OUTRO("Outro"),
    PREFIRO_NAO_INFORMAR("Prefiro não informar");

    private final String descricao;

    GenreEnum(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return this.descricao;
    }
}
