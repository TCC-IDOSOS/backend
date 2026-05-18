# Test Processor (UTT + MARCHA)

Unified AWS Lambda that processes both UTT and MARCHA test captures from S3. Dispatches to the appropriate pipeline based on the test-type segment of the S3 key (`/utt/` or `/marcha/`).

## Pipelines

- **UTT** (`processor_utt.py`) — port of `UTT_01.ipynb`. Linear-acceleration signal, computes per-cycle amplitude / rise time / power / work, and aggregate metrics for sit-to-stand. No ML.
- **MARCHA** (`processor_marcha.py`) — port of `MARCHA.ipynb` (third-party master's-thesis algorithm) + pretrained stack regressor (`lin + hgb -> meta`) loaded from `calibrador_global_SP2VC_features_stack.joblib`. Filters records to `sensor == "giroscopio"` and runs the calibrated peak-velocity pipeline. Produces aggregate metrics + per-cycle features + calibrated angular velocity.

Both pipelines are near copy-paste of the upstream notebooks. Only I/O bridges, plotting/export stripping, and `SystemExit` -> `ValueError` were changed. See `feedback_third_party_research_code.md` in the project's auto-memory.

## Running tests

```bash
cd lambdas/processor
uv sync
uv run pytest -q
```

Runtime deps (`numpy`, `scipy`, `scikit-learn==1.6.1`, `pandas`, `joblib`) and dev deps (`pytest`, `boto3`, `moto`) are declared in `pyproject.toml`; exact versions pinned in `uv.lock`.

`scikit-learn` is pinned to `1.6.1` because the MARCHA `.joblib` was serialized with that version. Newer versions emit `InconsistentVersionWarning` on load and may silently break.

## Dispatch contract

The Lambda determines test type from the S3 key path segment:

- `raw/users/{userId}/utt/{...}.json` -> `process_utt`
- `raw/users/{userId}/marcha/{...}.json` -> `process_marcha`
- anything else -> skipped (logged)

It also sanity-checks `payload["tipo_teste"]` against the key segment; mismatch is a `ValueError` and writes a `.error.json` sibling.

## Output schemas

**UTT** top-level keys: `total_repeticoes, repeticoes_completas, percentual_completas, altura_media, cadencia, amplitude_maxima_oscilacao, tempo_total_execucao, desvio_padrao_aceleracoes, velocidade_media_oscilacao, indice_estabilidade, classificacao, cycles, t_s, displacement_cm, peaks_t_s, peaks_value_cm, valleys_t_s, valleys_value_cm, peak_freq_full_hz, peak_freq_cut_hz, fc_f, nyq_f`.

**MARCHA** top-level keys: `n_peaks, strategy, cadence_cycles_min, vel_ini_deg_s, vel_end_deg_s, slope_deg_s2, vel_mean_deg_s, vel_std_deg_s, cv_vel, vel_max_deg_s, vel_min_deg_s, time_mean_s, time_std_s, cv_time, time_max_s, time_min_s, cycles, t_s, signal_deg_s, detect_signal, peaks_t_s, peaks_value_deg_s, peaks_pred_deg_s, subject_sex, subject_age, subject_id, model_target_mode`.

Both schemas use `null` for fields that would be `NaN`/`inf` (recursively swept before `json.dumps(..., allow_nan=False)`).

## Fixtures

- `fixtures/utt_sample.json` — real UTT capture (2026-05-16, 27 reps detected by the mobile).
- `fixtures/marcha_sample.json` — real MARCHA dual-sensor capture (2026-05-16, 18 reps detected by the mobile).

## Mobile payload contract

UTT records: `{time, x, y, z}` — `time` in ms; `x, y, z` in G from `TYPE_LINEAR_ACCELERATION`.

MARCHA records: `{time, x, y, z, sensor}` — `time` in ms; `sensor` is `"giroscopio"` (rad/s, `TYPE_GYROSCOPE`) or `"acelerometro"` (m/s², `TYPE_LINEAR_ACCELERATION`). The Lambda only consumes the gyroscope subset.

## Explicitly deferred

- Numerical regression tests against the notebook authors' expected outputs.
- `Resultado_Teste` schema migrations to persist aggregates (separate backend task).
- Backend webhook on processing completion.
- Tuning of notebook constants — kept as authored.
- Use of MARCHA bundle's `iso` / `tail` / `use_iso` keys — notebook explicitly uses stack-only.
