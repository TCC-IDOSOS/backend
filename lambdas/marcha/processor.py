"""Port of MARCHA.ipynb to a Lambda-callable function `process_marcha(payload, bundle) -> dict`.

The numerical pipeline is a near copy-paste of the notebook authored by the master's-thesis researcher.
Surgical edits only: input bridge (gyroscope subset of payload registros into the DataFrame the notebook
expects), `SystemExit` -> `ValueError` so the Lambda's error path can react, and stripped plotting/export.
"""

import math
import os
import warnings

import joblib
import numpy as np
import pandas as pd
from scipy.interpolate import CubicSpline
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import skew, kurtosis

warnings.filterwarnings("ignore")

_BUNDLE_PATH = os.path.join(os.path.dirname(__file__), "calibrador_global_SP2VC_features_stack.joblib")
BUNDLE = joblib.load(_BUNDLE_PATH)


def _nan_to_none(value):
    if isinstance(value, float):
        if math.isnan(value) or math.isinf(value):
            return None
        return value
    if isinstance(value, dict):
        return {k: _nan_to_none(v) for k, v in value.items()}
    if isinstance(value, list):
        return [_nan_to_none(v) for v in value]
    return value


def process_marcha(payload: dict, bundle=BUNDLE) -> dict:
    if str(payload.get("tipo_teste", "")).strip().upper() != "MARCHA":
        raise ValueError(f"tipo_teste must be MARCHA, got {payload.get('tipo_teste')!r}")

    registros = payload.get("registros") or []
    gyro = [r for r in registros if r.get("sensor") == "giroscopio"]
    if not gyro:
        raise ValueError("Nenhum registro de giroscópio no payload.")
    if len(gyro) < 10:
        raise ValueError("Sinal X muito curto.")

    df_sp = pd.DataFrame({
        "gyroTimestamp_sinceReboot(s)": [r["time"] / 1000.0 for r in gyro],
        "gyroRotationX(rad/s)":          [r["x"] for r in gyro],
        "gyroRotationY(rad/s)":          [r["y"] for r in gyro],
        "gyroRotationZ(rad/s)":          [r["z"] for r in gyro],
    })

    sexo_sujeito  = payload.get("sexo")
    idade_sujeito = payload.get("idade")
    Sujeito       = str(payload.get("userId") or payload.get("data_hora") or "unknown")

    dt = 1/60
    fs = 1/dt
    corte_s = 0

    # ===========================================================
    # CONFIG (do notebook, célula 2)
    # ===========================================================
    peak_mode   = "height"
    peak_height = 70.0
    peak_prom   = 0.30
    min_dist_s  = 0.5
    use_vector_mag = False
    align_axis      = "X"
    win_s = 0.12

    # ==================== HELPERS ====================
    DT = float(dt); FS = float(fs)
    assert np.isclose(FS, 1.0/DT), "fs e dt inconsistentes"

    def lowpass(x, fs=FS, fc=4.0, order=4):
        b, a = butter(order, fc/(fs/2), "low"); return filtfilt(b, a, x)

    def resample_uniform(t, y, dt=DT):
        t = np.asarray(t); y = np.asarray(y)
        if not np.all(np.diff(t) > 0):
            idx = np.unique(t, return_index=True)[1]; t = t[idx]; y = y[idx]
            srt = np.argsort(t); t = t[srt]; y = y[srt]
        if t.size < 3: return np.array([]), np.array([])
        tn = np.arange(t.min(), t.max(), dt)
        if tn.size < 3: return np.array([]), np.array([])
        try:
            yn = CubicSpline(t, y)(tn)
        except Exception:
            return np.array([]), np.array([])
        return tn, yn

    def peaks_generic(sig, fs=FS, mode="height", height=115.0, prom=0.3, min_dist_s=0.25):
        if len(sig) < 10 or np.allclose(np.std(sig), 0.0): return np.array([], dtype=int)
        dist = max(1, int(round(min_dist_s * fs)))
        if mode == "prom":
            prominence = max(prom * np.std(sig), 1e-9)
            idx, _ = find_peaks(sig, distance=dist, prominence=prominence)
        else:
            idx, _ = find_peaks(sig, distance=dist, height=float(height))
        return idx

    def parabolic_peak(t, y, i):
        i = int(i)
        if i <= 0 or i >= len(y) - 1: return float(t[i]), float(y[i]), int(i)
        y0, y1, y2 = y[i-1], y[i], y[i+1]
        d = (y0 - 2*y1 + y2)
        if d == 0: return float(t[i]), float(y1), int(i)
        delta = 0.5*(y0 - y2)/d
        tpk   = t[i] + delta*(t[i+1]-t[i])
        ypk   = y1 - 0.25*(y0 - y2)*delta
        return float(tpk), float(ypk), int(i)

    def sp_feats(seg, fs=FS):
        x = np.asarray(seg).astype(float); L = len(x); dt = 1.0/fs
        keys = ["f_peak_sp","f_p95_sp","f_p98_sp","f_mean_sp","f_std_sp","f_rms_sp",
                "f_skew_sp","f_kurt_sp","f_width80_s","f_slope_top",
                "f_cycle_dur_s","f_cycle_freq_hz","f_area_pos","f_energy","f_zc","f_ratio_peak_med"]
        if L < 5: return {k: np.nan for k in keys}
        peak = float(x.max()); idx = int(np.argmax(x))
        feats = dict(
            f_peak_sp=peak, f_p95_sp=float(np.percentile(x,95)), f_p98_sp=float(np.percentile(x,98)),
            f_mean_sp=float(np.mean(x)), f_std_sp=float(np.std(x)), f_rms_sp=float(np.sqrt(np.mean(x**2)))
        )
        feats["f_skew_sp"] = float(skew(x,bias=False)) if L>3 else 0.0
        feats["f_kurt_sp"] = float(kurtosis(x,fisher=True,bias=False)) if L>3 else 0.0
        feats.update(
            f_cycle_dur_s=float(L*dt),
            f_cycle_freq_hz=float(1.0/(L*dt)) if L>0 else np.nan,
            f_area_pos=float(np.sum(x[x>0])*dt),
            f_energy=float(np.sum(x**2)*dt),
            f_zc=int(np.where(np.diff(np.sign(x))!=0)[0].size),
            f_ratio_peak_med=float(peak/(np.median(x)+1e-9))
        )
        thr = 0.8*peak; above = np.where(x>=thr)[0]
        feats["f_width80_s"] = float((above[-1]-above[0])*dt) if above.size>1 else 0.0
        half = int(round(0.04*fs)); a = max(0, idx-half); b = min(L, idx+half+1)
        t_loc = np.arange(b-a)*dt; y_loc = x[a:b]
        feats["f_slope_top"] = float(np.polyfit(t_loc, y_loc, 1)[0]) if len(t_loc)>1 else 0.0
        return feats

    def predict_stack_only(bundle, Xrow):
        if isinstance(bundle, dict) and all(k in bundle for k in ["lin","hgb","meta"]):
            lin, hgb, meta = bundle["lin"], bundle["hgb"], bundle["meta"]
            pred_lin = lin.predict(Xrow).astype(float).ravel()
            pred_hgb = hgb.predict(Xrow).astype(float).ravel()
            Z = np.column_stack([pred_lin, pred_hgb])
            target_mode = bundle.get("target_mode","raw")
            if target_mode == "log": return np.expm1(meta.predict(Z)).astype(float)
            return meta.predict(Z).astype(float)
        if hasattr(bundle, "predict"): return bundle.predict(Xrow).astype(float)
        raise RuntimeError("Calibrador não reconhecido (esperado bundle lin/hgb/meta).")

    # ==================== PIPELINE ====================

    feat_cols = bundle.get("feat_cols", None) if isinstance(bundle, dict) else None
    expects_cols = set(feat_cols) if isinstance(feat_cols, list) else None
    need_Y = any(c.startswith("fY_") for c in expects_cols) if expects_cols else False
    need_Z = any(c.startswith("fZ_") for c in expects_cols) if expects_cols else False
    need_XYZ = (need_Y or need_Z) or (expects_cols and (("w_peak_phoneY_deg_s" in expects_cols) or ("w_peak_phoneZ_deg_s" in expects_cols)))

    # ---- 1) Ler CSV e uniformizar ----
    col_t = "gyroTimestamp_sinceReboot(s)"
    col_x = "gyroRotationX(rad/s)"
    col_y = "gyroRotationY(rad/s)"
    col_z = "gyroRotationZ(rad/s)"
    assert col_t in df_sp.columns and col_x in df_sp.columns, "CSV sem colunas obrigatórias."

    have_Y = col_y in df_sp.columns
    have_Z = col_z in df_sp.columns

    tX, x_u = resample_uniform(df_sp[col_t].values, df_sp[col_x].values, DT)
    if tX.size < 10: raise ValueError("Sinal X muito curto.")
    Xf = lowpass(np.rad2deg(x_u))

    if have_Y:
        tY, y_u = resample_uniform(df_sp[col_t].values, df_sp[col_y].values, DT)
        Yf = lowpass(np.rad2deg(y_u)) if tY.size>=10 else None; have_Y = Yf is not None
    else:
        Yf = None

    if have_Z:
        tZ, z_u = resample_uniform(df_sp[col_t].values, df_sp[col_z].values, DT)
        Zf = lowpass(np.rad2deg(z_u)) if tZ.size>=10 else None; have_Z = Zf is not None
    else:
        Zf = None

    t0 = max(tX.min(), (tY.min() if have_Y else tX.min()), (tZ.min() if have_Z else tX.min()))
    t1 = min(tX.max(), (tY.max() if have_Y else tX.max()), (tZ.max() if have_Z else tX.max()))
    t = np.arange(t0, t1, DT)

    X = np.interp(t, tX, Xf)
    Y = np.interp(t, tY, Yf) if have_Y else None
    Z = np.interp(t, tZ, Zf) if have_Z else None

    # ---- 2) Sinal de detecção + CORTE INICIAL (variável) + picos ----
    if use_vector_mag and (Y is not None) and (Z is not None):
        det_sig = np.sqrt(X**2 + Y**2 + Z**2); det_name = "mag"
    else:
        axis = align_axis.upper()
        det_sig = {"X": X, "Y": (Y if Y is not None else X), "Z": (Z if Z is not None else X)}[axis]
        det_name = axis

    try:
        corte_s = float(corte_s)
    except NameError:
        corte_s = 0.0

    if corte_s > 0:
        t_start = t[0] + corte_s
        mask_corte = (t >= t_start)

        t = t[mask_corte]
        X = X[mask_corte]
        if Y is not None: Y = Y[mask_corte]
        if Z is not None: Z = Z[mask_corte]
        det_sig = det_sig[mask_corte]

        if t.size < 10:
            raise ValueError(f"Sinal ficou curto demais após corte inicial de {corte_s:.2f}s.")

    idx_full = peaks_generic(det_sig, FS, peak_mode, peak_height, peak_prom, min_dist_s)
    if idx_full.size == 0: raise ValueError("Nenhum pico detectado.")

    # ---- 3) Zero = último cruzamento (−→+) antes do 1º pico ----
    first_peak_idx = int(idx_full[0])
    pre_win = int(round(2.0 * FS))
    a = max(0, first_peak_idx - pre_win)
    b = first_peak_idx
    local_offset = float(np.mean(det_sig[a:b])) if b > a else float(np.mean(det_sig[:first_peak_idx]))
    zc_sig = det_sig - local_offset
    rising = np.where((zc_sig[:-1] <= 0) & (zc_sig[1:] > 0))[0]
    prev_rising = rising[rising < first_peak_idx]
    if prev_rising.size > 0:
        zero_idx = int(prev_rising[-1] + 1)
    else:
        zero_idx = int(np.argmin(np.abs(zc_sig[:first_peak_idx]))) if first_peak_idx > 0 else 0

    t   = t[zero_idx:]
    X   = X[zero_idx:]
    Y   = (Y[zero_idx:] if Y is not None else None)
    Z   = (Z[zero_idx:] if Z is not None else None)
    det_sig = det_sig[zero_idx:]

    t_rel = t - t[0]

    mask_120 = (t_rel <= 120.0)
    t_rel = t_rel[mask_120]
    X = X[mask_120]
    if Y is not None: Y = Y[mask_120]
    if Z is not None: Z = Z[mask_120]
    det_sig = det_sig[mask_120]

    # ---- 4) (Re)detectar picos na janela 0..120 s ----
    idx = peaks_generic(det_sig, FS, peak_mode, peak_height, peak_prom, min_dist_s)
    if idx.size == 0: raise ValueError("Nenhum pico após recorte 0..120 s.")
    win = int(round(win_s * FS))

    # ---- 5) Loop de picos -> features -> predição ----
    rows = []; t_phone_peaks, y_phone_peaks = [], []
    t_pred_peaks,  y_pred_peaks  = [], []

    feat_cols = bundle.get("feat_cols", None) if isinstance(bundle, dict) else None
    expects_cols = set(feat_cols) if isinstance(feat_cols, list) else None
    need_Y = any(c.startswith("fY_") for c in expects_cols) if expects_cols else False
    need_Z = any(c.startswith("fZ_") for c in expects_cols) if expects_cols else False
    need_XYZ = (need_Y or need_Z) or (expects_cols and (("w_peak_phoneY_deg_s" in expects_cols) or ("w_peak_phoneZ_deg_s" in expects_cols)))

    for k, i_p in enumerate(idx, start=1):
        s = max(i_p - win, 0); e = min(i_p + win + 1, len(t_rel))
        t_win = t_rel[s:e]
        Xw = X[s:e].astype(float)
        Yw = (Y[s:e].astype(float) if Y is not None else None)
        Zw = (Z[s:e].astype(float) if Z is not None else None)
        if len(Xw) < 5: continue

        tpk, ypk, _ = parabolic_peak(t_rel, det_sig, i_p)
        t_phone_peaks.append(float(tpk)); y_phone_peaks.append(float(ypk))

        ipx = int(np.argmax(Xw)); _, ypk_x, _ = parabolic_peak(t_win, Xw, ipx)
        if Yw is not None:
            ipy = int(np.argmax(Yw)); _, ypk_y, _ = parabolic_peak(t_win, Yw, ipy)
        else: ypk_y = np.nan
        if Zw is not None:
            ipz = int(np.argmax(Zw)); _, ypk_z, _ = parabolic_peak(t_win, Zw, ipz)
        else: ypk_z = np.nan

        fX = {f"fX_{k2}": v for k2, v in sp_feats(Xw, FS).items()}
        if need_XYZ:
            fY = {f"fY_{k2}": v for k2, v in sp_feats(Yw if Yw is not None else [0,0,0], FS).items()}
            fZ = {f"fZ_{k2}": v for k2, v in sp_feats(Zw if Zw is not None else [0,0,0], FS).items()}
        else:
            fY, fZ = {}, {}
        feats = {}; feats.update(fX); feats.update(fY); feats.update(fZ)
        feats["w_peak_phoneX_deg_s"] = float(ypk_x)
        if need_XYZ:
            feats["w_peak_phoneY_deg_s"] = float(ypk_y)
            feats["w_peak_phoneZ_deg_s"] = float(ypk_z)

        if feat_cols is not None:
            Xrow = np.array([[feats.get(c, np.nan) for c in feat_cols]], dtype=float)
        else:
            keys = sorted(feats.keys()); Xrow = np.array([[feats[k] for k in keys]], dtype=float)

        y_pred = float(predict_stack_only(bundle, Xrow)[0])
        t_pred_peaks.append(float(tpk)); y_pred_peaks.append(float(y_pred))

        rows.append(dict(
            sujeito=Sujeito, sexo=sexo_sujeito, idade=idade_sujeito,
            peak_idx=int(k), t_peak_phone_s=float(tpk), detect_signal=det_name,
            w_phoneX_peak_deg_s=float(ypk_x),
            w_phoneY_peak_deg_s=float(ypk_y),
            w_phoneZ_peak_deg_s=float(ypk_z),
            w_pred_deg_s=float(y_pred)
        ))

    if not rows: raise ValueError("Nenhum ciclo válido para exportar.")

    # =========================
    # SUMMARY (do notebook, célula 3)
    # =========================
    t_pred = np.array(t_pred_peaks, dtype=float)
    v_pred = np.array(y_pred_peaks, dtype=float)

    if v_pred.size == 0:
        raise ValueError("No predicted peaks to summarize.")

    threshold = 11.0
    t_start, t_end = 0.0, 120.0
    duration = float(t_end - t_start)

    ini_t0, ini_t1 = 0.0, 20.0
    end_t0, end_t1 = 100.0, 120.0

    mask_ini = (t_pred >= ini_t0) & (t_pred <= ini_t1)
    mask_end = (t_pred >= end_t0) & (t_pred <= end_t1)

    Vm_ini = float(np.mean(v_pred[mask_ini])) if np.any(mask_ini) else np.nan
    Vm_end = float(np.mean(v_pred[mask_end])) if np.any(mask_end) else np.nan

    delta_vm = Vm_end - Vm_ini if (not math.isnan(Vm_end) and not math.isnan(Vm_ini)) else np.nan
    if np.isnan(delta_vm):
        strategy = "undefined"
    elif delta_vm > threshold:
        strategy = "ascending"
    elif delta_vm < -threshold:
        strategy = "descending"
    else:
        strategy = "constant"

    tA, tB = 10.0, 110.0
    slope = np.nan
    if (not math.isnan(Vm_ini)) and (not math.isnan(Vm_end)):
        slope = (Vm_end - Vm_ini) / (tB - tA)

    N = int(v_pred.size)
    cadence = N / 2.0
    Vm_total = float(np.mean(v_pred))
    std_vel  = float(np.std(v_pred, ddof=0))
    cv_vel   = float(std_vel / (Vm_total + 1e-9))
    vel_max  = float(np.max(v_pred))
    vel_min  = float(np.min(v_pred))

    if t_pred.size >= 2:
        isi = np.diff(t_pred)
        time_mean = float(np.mean(isi))
        std_time  = float(np.std(isi, ddof=0))
        cv_time   = float(std_time / (time_mean + 1e-9))
        time_max  = float(np.max(isi))
        time_min  = float(np.min(isi))
    else:
        time_mean = std_time = cv_time = time_max = time_min = np.nan

    cycles = [
        {
            "peak_idx": int(r["peak_idx"]),
            "t_peak_s": float(r["t_peak_phone_s"]),
            "w_phoneX_peak_deg_s": float(r["w_phoneX_peak_deg_s"]),
            "w_phoneY_peak_deg_s": float(r["w_phoneY_peak_deg_s"]),
            "w_phoneZ_peak_deg_s": float(r["w_phoneZ_peak_deg_s"]),
            "w_pred_deg_s": float(r["w_pred_deg_s"]),
        }
        for r in rows
    ]

    result = {
        "n_peaks": N,
        "strategy": strategy,
        "cadence_cycles_min": float(cadence),
        "vel_ini_deg_s": Vm_ini,
        "vel_end_deg_s": Vm_end,
        "slope_deg_s2": float(slope) if not (isinstance(slope, float) and math.isnan(slope)) else slope,
        "vel_mean_deg_s": Vm_total,
        "vel_std_deg_s": std_vel,
        "cv_vel": cv_vel,
        "vel_max_deg_s": vel_max,
        "vel_min_deg_s": vel_min,
        "time_mean_s": time_mean,
        "time_std_s": std_time,
        "cv_time": cv_time,
        "time_max_s": time_max,
        "time_min_s": time_min,
        "cycles": cycles,
        "t_s": t_rel.tolist(),
        "signal_deg_s": det_sig.tolist(),
        "detect_signal": det_name,
        "peaks_t_s": list(map(float, t_phone_peaks)),
        "peaks_value_deg_s": list(map(float, y_phone_peaks)),
        "peaks_pred_deg_s": list(map(float, y_pred_peaks)),
        "subject_sex": sexo_sujeito,
        "subject_age": int(idade_sujeito) if idade_sujeito is not None else None,
        "subject_id": Sujeito,
        "model_target_mode": bundle.get("target_mode", "raw") if isinstance(bundle, dict) else "raw",
    }

    return _nan_to_none(result)
