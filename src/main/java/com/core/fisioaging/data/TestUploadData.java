package com.core.fisioaging.data;

import com.core.fisioaging.enums.GenreEnum;
import lombok.Data;

import java.util.List;

@Data
public class TestUploadData {

    private String tipo_teste;
    private String data_hora;
    private String sensor;
    private Integer frequencia;
    private Integer total_repeticoes_app;
    private GenreEnum sexo;
    private Integer idade;
    private Double massa_kg;
    private List<TestRecordData> registros;
}
