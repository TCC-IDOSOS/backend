package com.core.fisioaging.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestResultAwsResponse {

    @JsonProperty("total_repeticoes")
    private Integer totalRepeticoes;

    @JsonProperty("repeticoes_completas")
    private Integer repeticoesCompletas;

    @JsonProperty("percentual_completas")
    private Double percentualCompletas;

    @JsonProperty("altura_media")
    private Double alturaMedia;

    @JsonProperty("cadencia")
    private Double cadencia;

    @JsonProperty("amplitude_maxima_oscilacao")
    private Double amplitudeMaximaOscilacao;

    @JsonProperty("tempo_total_execucao")
    private Double tempoTotalExecucao;

    @JsonProperty("desvio_padrao_aceleracoes")
    private Double desvioPadraoAceleracoes;

    @JsonProperty("velocidade_media_oscilacao")
    private Double velocidadeMediaOscilacao;

    @JsonProperty("indice_estabilidade")
    private Double indiceEstabilidade;

    @JsonProperty("classificacao")
    private String classificacao;
}
