# MARCHA Processor

Wrapper Lambda over `MARCHA.ipynb` (third-party master's-thesis algorithm) plus the pretrained `calibrador_global_SP2VC_features_stack.joblib` stack regressor (`lin + hgb -> meta`).

`process_marcha(payload: dict, bundle=BUNDLE) -> dict` is a near copy-paste of the notebook with surgical edits only:
- Input bridge: filters `payload["registros"]` by `sensor == "giroscopio"` and builds the DataFrame with the column names the notebook expects (`gyroTimestamp_sinceReboot(s)`, `gyroRotationX/Y/Z(rad/s)`).
- `SystemExit(...)` -> `ValueError(...)` (messages preserved) so the Lambda's `.error.json` path can react.
- Plotting (plotly + matplotlib) and CSV/XLSX export removed.
- Output dict assembled at the end from the same variables that fed `df_pred` and `df_xlsx`.

## Running the tests

```bash
cd lambdas/marcha
uv sync
uv run pytest -q
```

Runtime deps (`numpy`, `scipy`, `scikit-learn==1.6.1`, `pandas`, `joblib`) and dev deps (`pytest`, `boto3`, `moto`) are declared in `pyproject.toml`; exact versions are pinned in `uv.lock`.

`scikit-learn` is pinned to `1.6.1` because the `.joblib` was serialized with that version. Newer versions emit `InconsistentVersionWarning` on load and may silently break.

## What `process_marcha` returns

Top-level keys (`NaN`/`inf` are converted to `None` recursively before serialization):

- Aggregate metrics from the notebook's summary cell: `n_peaks`, `strategy` (`ascending` / `descending` / `constant` / `undefined`), `cadence_cycles_min`, `vel_ini_deg_s`, `vel_end_deg_s`, `slope_deg_s2`, `vel_mean_deg_s`, `vel_std_deg_s`, `cv_vel`, `vel_max_deg_s`, `vel_min_deg_s`, `time_mean_s`, `time_std_s`, `cv_time`, `time_max_s`, `time_min_s`.
- `cycles` — list of per-peak dicts: `peak_idx, t_peak_s, w_phoneX/Y/Z_peak_deg_s, w_pred_deg_s`.
- `t_s`, `signal_deg_s`, `detect_signal` — detection signal time series for the chart.
- `peaks_t_s`, `peaks_value_deg_s`, `peaks_pred_deg_s` — event markers (raw and calibrated values).
- `subject_sex`, `subject_age`, `subject_id`, `model_target_mode` — diagnostic / metadata.

## Fixtures

- `fixtures/marcha_stationary.json` — real stationary capture (`MARCHA_20260507_111058_*`). Used by the error-path test: gyroscope std on the dominant axis is ~0.009 rad/s (vs ~0.5–2 rad/s for walking) so the pipeline correctly raises `ValueError("Nenhum pico detectado.")`.
- `fixtures/marcha_sample.json` — **missing**: needs a fresh ~120 s walking capture from the dual-sensor mobile build. Until it exists, `test_process_marcha_happy_path` and `test_lambda_handler_s3_event` are skipped.

## Mobile payload contract

Each element of `registros[]` carries a `sensor` field (`"giroscopio"` or `"acelerometro"`); the mobile records both interleaved (~58 Hz each). The Lambda only consumes the gyroscope subset; the accelerometer subset is dropped.

Time is in **ms** since session start. Gyroscope axes are in **rad/s** (Android `TYPE_GYROSCOPE` raw); the Lambda converts to deg/s internally via `np.rad2deg`.

## Explicitly deferred

- Real walking-capture fixture (blocked on mobile team).
- Numerical regression test against the notebook author's expected output (needs the walking fixture + a notebook run on the same input as a baseline).
- `Resultado_Teste` schema migrations to persist MARCHA aggregates (separate backend task).
- Backend webhook on processing completion (same TODO as UTT).
- Tuning of notebook constants (`peak_height=70`, `min_dist_s=0.5`, `win_s=0.12`, summary threshold `11.0`, etc.) — kept as authored.
- Use of the bundle's `iso` / `tail` / `use_iso` keys — the notebook explicitly uses stack-only.
