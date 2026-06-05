package com.core.fisioaging.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

    // ---- Campos específicos do teste de MARCHA (parametrizam o cálculo do relatório) ----

    @JsonProperty("n_peaks")
    private Integer nPeaks;

    @JsonProperty("cadence_cycles_min")
    private Double cadenceCyclesMin;

    @JsonProperty("peaks_t_s")
    private List<Double> peaksTS;

    // Pode vir como array de números ou como string JSON; tratado de forma defensiva no service.
    @JsonProperty("peaks_value_deg_s")
    private JsonNode peaksValueDegS;

    @JsonProperty("cycles")
    private List<Cycle> cycles;

    @Getter
    @Setter
    public static class Cycle {
        @JsonProperty("w_phoneZ_peak_deg_s")
        private Double wPhoneZPeakDegS;
    }
}
