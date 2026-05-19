package com.core.fisioaging.data;

import com.core.fisioaging.enums.GenreEnum;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class TestUploadData {

    private String tipo_teste;
    private String data_hora;
    private String sensor;
    private Integer frequencia;
    private Integer total_repeticoes_app;
    private Integer id_profissional;
    private String email_profissional;
    private GenreEnum sexo;
    private Integer idade;

    // TODO: remove peso alias once mobile migrates to massa_kg
    @JsonAlias({"peso"})
    private Double massa_kg;

    private List<TestRecordData> registros;
    private String unidadeSaudeCnpj;
}
