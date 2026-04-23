# UTT Processor (MVP)

Minimal port of `UTT_01.ipynb` to a Lambda-callable function `process_utt(payload: dict) -> dict`.

## Running the test

```bash
cd lambdas/utt
uv sync
uv run pytest -q
```

Runtime deps (`numpy`, `scipy`) and the dev dep (`pytest`) are declared in `pyproject.toml`; exact versions are pinned in `uv.lock`.

## What `process_utt` returns

Top-level keys of the returned dict:

- `total_repeticoes`, `repeticoes_completas`, `percentual_completas` — repetition counts.
- `altura_media` (cm), `cadencia` (cycles/min), `amplitude_maxima_oscilacao` (cm) — aggregate metrics.
- `tempo_total_execucao` (s), `desvio_padrao_aceleracoes` (m/s²), `velocidade_media_oscilacao` (cm/s).
- `indice_estabilidade`, `classificacao` — reserved, always `None` in this version.
- `cycles` — list of per-cycle dicts: `ciclo, amplitude_cm, velocidade_subida_cm_s, tempo_subida_s, tempo_ciclo_s, potencia_w, trabalho_j`.
- `t_s`, `displacement_cm` — displacement time series for the chart.
- `peaks_t_s`, `peaks_value_cm`, `valleys_t_s`, `valleys_value_cm` — event markers for the chart.
- `peak_freq_full_hz`, `peak_freq_cut_hz`, `fc_f`, `nyq_f` — diagnostic frequency-domain parameters.

The 11 `resultado_teste` DB fields are promoted to top-level so the backend webhook can map the dict straight to the `Resultado_Teste` row without a translation layer.

## Explicitly deferred (`# TODO:` in the code)

- Resampling target `fs = 60` Hz — hardcoded from notebook.
- Low-pass filter `cutoff = 1` Hz, `order = 2` — hardcoded from notebook.
- Initial acceleration peak `height = 0.2` — hardcoded from notebook.
- Cut window 33 s — from authoritative notebook; HU005/HU006 spec says 30 s; confirm with advisor.
- Displacement peak (`height = 2`, `prominence = 1`) and valley (`prominence = 0.15`) thresholds — from notebook; retune against a clinically validated set of Android captures.
- Rise-slope window `20%–80%` of cycle amplitude — hardcoded from notebook.

Deferred by product scope (not marked with `# TODO:` but absent):
- `indice_estabilidade` formula — field is present in the output and always `None`.
- `classificacao` rule — field is present in the output and always `None`.
- Input validation beyond what the notebook does.
- Lambda handler, S3 wiring, webhook signing, error structured logging.
- Unit tests for internal steps (only an end-to-end happy-path test exists).

## Fixture notes

`fixtures/utt_sample.json` is a copy of the real Android capture `UTT_20260421_220422_8_CristhianRodrigues.json` with `massa_kg: 88` added (the original payload lacked it — the mobile team will start sending it). The value 88 matches the hardcoded mass the notebook was tuned against, which keeps "run notebook on this JSON" a valid future comparison path.
