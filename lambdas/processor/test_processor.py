import json
import math
import os

os.environ.setdefault("AWS_DEFAULT_REGION", "us-east-1")
os.environ.setdefault("AWS_ACCESS_KEY_ID", "testing")
os.environ.setdefault("AWS_SECRET_ACCESS_KEY", "testing")

import boto3  # noqa: E402
import pytest  # noqa: E402
from moto import mock_aws  # noqa: E402

from processor_utt import process_utt  # noqa: E402
from processor_marcha import process_marcha  # noqa: E402
import processor_marcha as processor_marcha_module  # noqa: E402
from lambda_function import lambda_handler, _identify_test_type, TEST_DISPATCH  # noqa: E402


FIXTURES = os.path.join(os.path.dirname(__file__), "fixtures")
UTT_SAMPLE = os.path.join(FIXTURES, "utt_sample.json")
MARCHA_SAMPLE = os.path.join(FIXTURES, "marcha_sample.json")

UTT_EXPECTED_KEYS = {
    "total_repeticoes",
    "repeticoes_completas",
    "percentual_completas",
    "altura_media",
    "cadencia",
    "amplitude_maxima_oscilacao",
    "tempo_total_execucao",
    "desvio_padrao_aceleracoes",
    "velocidade_media_oscilacao",
    "indice_estabilidade",
    "classificacao",
    "cycles",
    "t_s",
    "displacement_cm",
    "peaks_t_s",
    "peaks_value_cm",
    "valleys_t_s",
    "valleys_value_cm",
    "peak_freq_full_hz",
    "peak_freq_cut_hz",
    "fc_f",
    "nyq_f",
}

MARCHA_EXPECTED_KEYS = {
    "n_peaks",
    "strategy",
    "cadence_cycles_min",
    "vel_ini_deg_s",
    "vel_end_deg_s",
    "slope_deg_s2",
    "vel_mean_deg_s",
    "vel_std_deg_s",
    "cv_vel",
    "vel_max_deg_s",
    "vel_min_deg_s",
    "time_mean_s",
    "time_std_s",
    "cv_time",
    "time_max_s",
    "time_min_s",
    "cycles",
    "t_s",
    "signal_deg_s",
    "detect_signal",
    "peaks_t_s",
    "peaks_value_deg_s",
    "peaks_pred_deg_s",
    "subject_sex",
    "subject_age",
    "subject_id",
    "model_target_mode",
}


def _iter_floats(value):
    if isinstance(value, dict):
        for v in value.values():
            yield from _iter_floats(v)
    elif isinstance(value, list):
        for v in value:
            yield from _iter_floats(v)
    elif isinstance(value, float):
        yield value


# --- dispatcher --------------------------------------------------------------

def test_dispatch_identifies_utt_key():
    test_type, processor = _identify_test_type("raw/users/8/utt/123_2026.json")
    assert test_type == "UTT"
    assert processor is process_utt


def test_dispatch_identifies_marcha_key():
    test_type, processor = _identify_test_type("raw/users/8/marcha/123_2026.json")
    assert test_type == "MARCHA"
    assert processor is process_marcha


def test_dispatch_returns_none_for_unknown_segment():
    test_type, processor = _identify_test_type("raw/users/8/other/123.json")
    assert test_type is None
    assert processor is None


def test_dispatch_table_only_contains_known_types():
    assert set(TEST_DISPATCH.keys()) == {"utt", "marcha"}


# --- processor_marcha bundle -------------------------------------------------

def test_marcha_bundle_loaded_at_import():
    assert processor_marcha_module.BUNDLE is not None
    assert isinstance(processor_marcha_module.BUNDLE, dict)
    for k in ("lin", "hgb", "meta"):
        assert k in processor_marcha_module.BUNDLE


# --- happy paths -------------------------------------------------------------

def test_process_utt_happy_path():
    with open(UTT_SAMPLE) as f:
        payload = json.load(f)
    result = process_utt(payload)

    assert set(result.keys()) == UTT_EXPECTED_KEYS
    for v in _iter_floats(result):
        assert not math.isnan(v), "result contains NaN"
    json.dumps(result, allow_nan=False)

    assert result["total_repeticoes"] > 0
    assert len(result["cycles"]) > 0


def test_process_marcha_happy_path():
    with open(MARCHA_SAMPLE) as f:
        payload = json.load(f)
    result = process_marcha(payload)

    assert set(result.keys()) == MARCHA_EXPECTED_KEYS
    for v in _iter_floats(result):
        assert not math.isnan(v), "result contains NaN"
    json.dumps(result, allow_nan=False)

    assert result["n_peaks"] > 0
    assert len(result["cycles"]) > 0
    assert result["strategy"] in {"ascending", "descending", "constant", "undefined"}

    cycle_keys = {
        "peak_idx", "t_peak_s",
        "w_phoneX_peak_deg_s", "w_phoneY_peak_deg_s", "w_phoneZ_peak_deg_s",
        "w_pred_deg_s",
    }
    for c in result["cycles"]:
        assert set(c.keys()) == cycle_keys
    assert len(result["t_s"]) == len(result["signal_deg_s"]) > 0


# --- synthetic error paths ---------------------------------------------------

def test_process_marcha_rejects_non_marcha_payload():
    with pytest.raises(ValueError, match="tipo_teste must be MARCHA"):
        process_marcha({"tipo_teste": "UTT", "registros": []})


def test_process_marcha_rejects_payload_without_gyroscope():
    payload = {
        "tipo_teste": "MARCHA",
        "registros": [
            {"time": 0,  "x": 0.0, "y": 0.0, "z": 0.0, "sensor": "acelerometro"},
            {"time": 16, "x": 0.0, "y": 0.0, "z": 0.0, "sensor": "acelerometro"},
        ],
    }
    with pytest.raises(ValueError, match="Nenhum registro de giroscópio"):
        process_marcha(payload)


# --- handler / S3 integration ------------------------------------------------

@mock_aws
def test_lambda_handler_utt_s3_event():
    bucket = "test-bucket"
    raw_key = "raw/users/8/utt/42_20260516_165517.json"
    expected_out_key = "processed/users/8/utt/42_20260516_165517.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)

    with open(UTT_SAMPLE, "rb") as f:
        body = f.read()
    s3.put_object(
        Bucket=bucket,
        Key=raw_key,
        Body=body,
        ContentType="application/json",
        Metadata={
            "user-email": "cristhian@example.com",
            "test-type": "UTT",
            "test-execution-id": "42",
        },
    )

    event = {"Records": [{"s3": {"bucket": {"name": bucket}, "object": {"key": raw_key}}}]}
    lambda_handler(event, None)

    obj = s3.get_object(Bucket=bucket, Key=expected_out_key)
    body_out = json.loads(obj["Body"].read())
    assert set(body_out.keys()) == UTT_EXPECTED_KEYS

    first_etag = obj["ETag"]
    lambda_handler(event, None)
    obj2 = s3.get_object(Bucket=bucket, Key=expected_out_key)
    assert obj2["ETag"] == first_etag


@mock_aws
def test_lambda_handler_marcha_s3_event():
    bucket = "test-bucket"
    raw_key = "raw/users/8/marcha/42_20260516_165427.json"
    expected_out_key = "processed/users/8/marcha/42_20260516_165427.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)

    with open(MARCHA_SAMPLE, "rb") as f:
        body = f.read()
    s3.put_object(
        Bucket=bucket,
        Key=raw_key,
        Body=body,
        ContentType="application/json",
        Metadata={
            "user-email": "cristhian@example.com",
            "test-type": "MARCHA",
            "test-execution-id": "42",
        },
    )

    event = {"Records": [{"s3": {"bucket": {"name": bucket}, "object": {"key": raw_key}}}]}
    lambda_handler(event, None)

    obj = s3.get_object(Bucket=bucket, Key=expected_out_key)
    body_out = json.loads(obj["Body"].read())
    assert set(body_out.keys()) == MARCHA_EXPECTED_KEYS

    first_etag = obj["ETag"]
    lambda_handler(event, None)
    obj2 = s3.get_object(Bucket=bucket, Key=expected_out_key)
    assert obj2["ETag"] == first_etag


@mock_aws
def test_lambda_handler_writes_error_json_for_bad_payload():
    bucket = "test-bucket"
    raw_key = "raw/users/8/marcha/99_20260516_000000.json"
    expected_out_key = "processed/users/8/marcha/99_20260516_000000.json"
    expected_err_key = expected_out_key + ".error.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)

    bad_payload = {
        "tipo_teste": "MARCHA",
        "data_hora": "20260516_000000",
        "sexo": "M",
        "idade": 60,
        "massa_kg": 70,
        "registros": [],
    }
    s3.put_object(
        Bucket=bucket,
        Key=raw_key,
        Body=json.dumps(bad_payload).encode("utf-8"),
        ContentType="application/json",
        Metadata={"user-email": "x@example.com", "test-type": "MARCHA", "test-execution-id": "99"},
    )

    event = {"Records": [{"s3": {"bucket": {"name": bucket}, "object": {"key": raw_key}}}]}
    lambda_handler(event, None)

    with pytest.raises(s3.exceptions.NoSuchKey):
        s3.get_object(Bucket=bucket, Key=expected_out_key)

    err_obj = s3.get_object(Bucket=bucket, Key=expected_err_key)
    err_body = json.loads(err_obj["Body"].read())
    assert "error" in err_body
    assert err_body["raw_key"] == raw_key


@mock_aws
def test_lambda_handler_skips_non_test_keys():
    bucket = "test-bucket"
    raw_key = "raw/users/8/other/foo.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)
    s3.put_object(Bucket=bucket, Key=raw_key, Body=b"{}", ContentType="application/json")

    event = {"Records": [{"s3": {"bucket": {"name": bucket}, "object": {"key": raw_key}}}]}
    lambda_handler(event, None)

    listing = s3.list_objects_v2(Bucket=bucket, Prefix="processed/")
    assert "Contents" not in listing
