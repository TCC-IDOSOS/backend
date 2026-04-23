import numpy as np
from scipy.interpolate import CubicSpline
from scipy.signal import butter, filtfilt, find_peaks
from scipy.integrate import cumulative_trapezoid


def process_utt(payload: dict) -> dict:
    """Port of UTT_01.ipynb: computes cycle metrics and displacement time series from raw accelerometer data."""

    # =====================================================
    # 1. CARREGAMENTO DE DADOS (do payload)
    # =====================================================
    Time_sp = np.array([r["time"] for r in payload["registros"]], dtype=float) / 1000.0  # ms -> s
    # Android sends TYPE_LINEAR_ACCELERATION (gravity already removed) in G.
    # Z is the axis perpendicular to the screen — vertical when the phone is
    # held against the torso during UTT. Confirmed empirically against
    # stationary and real UTT captures (2026-04-22).
    Signal_sp = np.array([r["z"] for r in payload["registros"]], dtype=float) * 9.80665

    # Parâmetros
    fs = 60  # TODO: resampling target (Hz), hardcoded from notebook.
    dt = 1 / fs
    massa_sujeito = payload["massa_kg"]

    # =====================================================
    # 2. INTERPOLAÇÃO DO SINAL DO SMARTPHONE
    # =====================================================
    Time_cumsum_sp = np.cumsum(np.insert(np.diff(Time_sp), 0, 0))

    if not np.all(np.diff(Time_cumsum_sp) > 0):
        _, unique_indices_sp = np.unique(Time_cumsum_sp, return_index=True)
        Time_cumsum_sp = Time_cumsum_sp[unique_indices_sp]
        Signal_sp = Signal_sp[unique_indices_sp]

    New_Time_sp = np.arange(Time_cumsum_sp.min(), Time_cumsum_sp.max(), dt)
    cs_sp = CubicSpline(Time_cumsum_sp, Signal_sp)
    Signal_interpolated_sp = cs_sp(New_Time_sp)

    # =====================================================
    # 3. ANÁLISE DE FREQUÊNCIA (SINAL COMPLETO)
    # =====================================================
    Signal_interpolated_sp_norm = Signal_interpolated_sp - np.mean(Signal_interpolated_sp)
    n = len(Signal_interpolated_sp_norm)
    freqs = np.fft.rfftfreq(n, d=dt)
    fft_values = np.fft.rfft(Signal_interpolated_sp_norm)
    fft_magnitude = np.abs(fft_values)
    peak_freq = freqs[np.argmax(fft_magnitude)]

    # =====================================================
    # 4. FILTRAGEM PASSA-BAIXA
    # =====================================================
    def butter_lowpass_filter(data, cutoff, fs, order=2):
        nyq = 0.5 * fs
        normal_cutoff = cutoff / nyq
        b, a = butter(order, normal_cutoff, btype='low', analog=False)
        return filtfilt(b, a, data)

    # TODO: cutoff=1 Hz, order=2 from notebook.
    Signal_filtered_sp = butter_lowpass_filter(Signal_interpolated_sp, cutoff=1, fs=fs, order=2)

    # =====================================================
    # 5. DETECÇÃO DE EVENTOS E CORTE DO SINAL
    # =====================================================
    # TODO: initial peak height=0.2 from notebook.
    picos_indices, _ = find_peaks(Signal_filtered_sp, height=0.2)

    if len(picos_indices) == 0:
        raise ValueError("Nenhum pico inicial detectado na aceleração filtrada.")

    zc_indices = np.where(np.diff(np.sign(Signal_filtered_sp)))[0]
    zc_times = [
        New_Time_sp[i] - Signal_filtered_sp[i] * (New_Time_sp[i + 1] - New_Time_sp[i])
        / (Signal_filtered_sp[i + 1] - Signal_filtered_sp[i])
        for i in zc_indices
    ]

    tempo_primeiro_pico = New_Time_sp[picos_indices[0]]
    zc_anteriores = [t for t in zc_times if t < tempo_primeiro_pico]

    if not zc_anteriores:
        tempo_inicio = New_Time_sp[0]
    else:
        tempo_inicio = zc_anteriores[-1]

    # Janela de 33 s, conforme a versão autoritativa do notebook (UTT_01.ipynb).
    # TODO: reconcile with HU005/HU006 spec which says 30 s — the clinical authors chose 33 s,
    # likely to include a buffer/transient margin. Confirm with advisor.
    tempo_fim = min(tempo_inicio + 33, New_Time_sp[-1])
    mask_corte = (New_Time_sp >= tempo_inicio) & (New_Time_sp <= tempo_fim)
    New_Time_cortado = New_Time_sp[mask_corte] - tempo_inicio
    Signal_cortado = Signal_filtered_sp[mask_corte]

    # =====================================================
    # 6. ANÁLISE DE FREQUÊNCIA (SINAL CORTADO)
    # =====================================================
    Signal_cortado_norm = Signal_cortado - np.mean(Signal_cortado)
    n_cortado = len(Signal_cortado_norm)
    freqs_cortado = np.fft.rfftfreq(n_cortado, d=dt)
    fft_values_cortado = np.fft.rfft(Signal_cortado_norm)
    fft_magnitude_cortado = np.abs(fft_values_cortado)
    peak_freq2 = freqs_cortado[np.argmax(fft_magnitude_cortado)]

    # =====================================================
    # 7. INTEGRAÇÃO PARA OBTER DESLOCAMENTO (com sinal bruto)
    # =====================================================
    nyq_f = 0.7832 - 0.1054 * peak_freq - 0.2219 * peak_freq2
    fc_f = 0.4082 - 0.0955 * peak_freq + 0.1417 * peak_freq2
    fc_f = max(0.2, min(fc_f, 1.2))
    nyq_f = max(0.3, min(nyq_f, 1.5))

    def highpass_filter(signal, fs, fc=0.5, order=2):
        nyq = nyq_f * fs
        normal_cutoff = fc / nyq
        b, a = butter(order, normal_cutoff, btype='high', analog=False)
        return filtfilt(b, a, signal)

    acel_para_integrar = Signal_interpolated_sp[mask_corte] - np.mean(Signal_interpolated_sp[mask_corte])
    velocidade = cumulative_trapezoid(acel_para_integrar, New_Time_cortado, initial=0)
    velocidade_corrigida = highpass_filter(velocidade, fs=fs, fc=fc_f, order=2)
    deslocamento = cumulative_trapezoid(velocidade_corrigida, New_Time_cortado, initial=0)
    deslocamento_cm = (deslocamento - deslocamento[0]) * 100
    deslocamento_cm -= np.mean(deslocamento_cm)

    # =====================================================
    # 8. DETECÇÃO DE PICOS E VALES NO DESLOCAMENTO FINAL
    # =====================================================
    # TODO: retune thresholds against a clinically validated set of Android UTT captures.
    picos_deslocamento, _ = find_peaks(deslocamento_cm, height=2, prominence=1)
    vales_deslocamento, _ = find_peaks(-deslocamento_cm, prominence=0.15)

    # =====================================================
    # 9. CÁLCULO DE MÉTRICAS DE CICLO (A PARTIR DO 2º PICO)
    # =====================================================
    picos_ordenados = np.sort(picos_deslocamento)
    vales_ordenados = np.sort(vales_deslocamento)

    ciclo_nums = []
    amplitudes = []
    velocidades_subida = []
    tempos_subida = []
    tempos_ciclo = []
    potencias = []
    trabalhos = []

    for i, pico_idx in enumerate(picos_ordenados):
        if i == 0:
            continue

        vales_anteriores = vales_ordenados[vales_ordenados < pico_idx]
        if not vales_anteriores.any():
            continue
        vale_anterior_idx = vales_anteriores[-1]

        vales_seguintes = vales_ordenados[vales_ordenados > pico_idx]
        if not vales_seguintes.any():
            continue
        vale_seguinte_idx = vales_seguintes[0]

        amplitude = deslocamento_cm[pico_idx] - deslocamento_cm[vale_anterior_idx]
        amplitudes.append(amplitude)

        tempo_pico = New_Time_cortado[pico_idx]
        tempo_vale_anterior = New_Time_cortado[vale_anterior_idx]
        tempo_subida = tempo_pico - tempo_vale_anterior
        tempos_subida.append(tempo_subida)

        tempo_vale_seguinte = New_Time_cortado[vale_seguinte_idx]
        tempo_ciclo = tempo_vale_seguinte - tempo_vale_anterior
        tempos_ciclo.append(tempo_ciclo)

        # TODO: rise-slope window 20%-80% of amplitude from notebook.
        y_20_percent = deslocamento_cm[vale_anterior_idx] + 0.2 * amplitude
        y_80_percent = deslocamento_cm[pico_idx] - 0.2 * amplitude

        segmento_subida_mask = (New_Time_cortado >= tempo_vale_anterior) & (New_Time_cortado <= tempo_pico)
        tempo_segmento = New_Time_cortado[segmento_subida_mask]
        sinal_segmento = deslocamento_cm[segmento_subida_mask]

        idx_t1 = np.argmin(np.abs(sinal_segmento - y_20_percent))
        idx_t2 = np.argmin(np.abs(sinal_segmento - y_80_percent))

        t1, y1 = tempo_segmento[idx_t1], sinal_segmento[idx_t1]
        t2, y2 = tempo_segmento[idx_t2], sinal_segmento[idx_t2]

        velocidade_subida_cm_s = (y2 - y1) / (t2 - t1) if (t2 - t1) > 1e-6 else np.nan

        acel_segmento_subida = Signal_cortado[segmento_subida_mask]
        a_media_subida = np.mean(acel_segmento_subida)

        g = 9.80665
        forca_subida = massa_sujeito * (g + a_media_subida)
        potencia_subida = forca_subida * (velocidade_subida_cm_s / 100)  # cm/s -> m/s
        trabalho_subida = potencia_subida * tempo_subida

        ciclo_nums.append(i + 1)
        velocidades_subida.append(velocidade_subida_cm_s)
        potencias.append(potencia_subida)
        trabalhos.append(trabalho_subida)

    if not amplitudes:
        raise ValueError("Nenhuma amplitude de ciclo foi calculada.")

    # =====================================================
    # 10. MONTAGEM DO RESULTADO
    # =====================================================
    total_repeticoes = int(len(picos_deslocamento))
    repeticoes_completas = int(len(ciclo_nums))
    percentual_completas = (
        100.0 * repeticoes_completas / total_repeticoes if total_repeticoes > 0 else 0.0
    )

    cycles = [
        {
            "ciclo": int(ciclo_nums[i]),
            "amplitude_cm": float(amplitudes[i]),
            "velocidade_subida_cm_s": float(velocidades_subida[i]),
            "tempo_subida_s": float(tempos_subida[i]),
            "tempo_ciclo_s": float(tempos_ciclo[i]),
            "potencia_w": float(potencias[i]),
            "trabalho_j": float(trabalhos[i]),
        }
        for i in range(len(ciclo_nums))
    ]

    return {
        "total_repeticoes": total_repeticoes,
        "repeticoes_completas": repeticoes_completas,
        "percentual_completas": float(percentual_completas),
        "altura_media": float(np.mean(amplitudes)),
        "cadencia": float(60.0 / np.mean(tempos_ciclo)),
        "amplitude_maxima_oscilacao": float(np.max(deslocamento_cm) - np.min(deslocamento_cm)),
        "tempo_total_execucao": float(New_Time_cortado[-1] - New_Time_cortado[0]),
        "desvio_padrao_aceleracoes": float(np.std(Signal_cortado)),
        "velocidade_media_oscilacao": float(np.mean(velocidades_subida)),
        "indice_estabilidade": None,
        "classificacao": None,
        "cycles": cycles,
        "t_s": New_Time_cortado.tolist(),
        "displacement_cm": deslocamento_cm.tolist(),
        "peaks_t_s": New_Time_cortado[picos_deslocamento].tolist(),
        "peaks_value_cm": deslocamento_cm[picos_deslocamento].tolist(),
        "valleys_t_s": New_Time_cortado[vales_deslocamento].tolist(),
        "valleys_value_cm": deslocamento_cm[vales_deslocamento].tolist(),
        "peak_freq_full_hz": float(peak_freq),
        "peak_freq_cut_hz": float(peak_freq2),
        "fc_f": float(fc_f),
        "nyq_f": float(nyq_f),
    }
