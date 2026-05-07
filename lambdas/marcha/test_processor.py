import json
import math
import os

os.environ.setdefault("AWS_DEFAULT_REGION", "us-east-1")
os.environ.setdefault("AWS_ACCESS_KEY_ID", "testing")
os.environ.setdefault("AWS_SECRET_ACCESS_KEY", "testing")

import boto3  # noqa: E402
import pytest  # noqa: E402
from moto import mock_aws  # noqa: E402

from processor import process_marcha  # noqa: E402
from lambda_function import lambda_handler  # noqa: E402
import processor as processor_module  # noqa: E402


FIXTURES = os.path.join(os.path.dirname(__file__), "fixtures")
SAMPLE_PATH = os.path.join(FIXTURES, "marcha_sample.json")
STATIONARY_PATH = os.path.join(FIXTURES, "marcha_stationary.json")

EXPECTED_TOP_LEVEL_KEYS = {
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


def test_bundle_loaded_at_import():
    assert processor_module.BUNDLE is not None
    assert isinstance(processor_module.BUNDLE, dict)
    assert "lin" in processor_module.BUNDLE
    assert "hgb" in processor_module.BUNDLE
    assert "meta" in processor_module.BUNDLE


def test_process_marcha_rejects_non_marcha_payload():
    with pytest.raises(ValueError, match="tipo_teste must be MARCHA"):
        process_marcha({"tipo_teste": "UTT", "registros": []})


def test_process_marcha_rejects_payload_without_gyroscope():
    payload = {
        "tipo_teste": "MARCHA",
        "registros": [
            {"time": 0, "x": 0.0, "y": 0.0, "z": 0.0, "sensor": "acelerometro"},
            {"time": 16, "x": 0.0, "y": 0.0, "z": 0.0, "sensor": "acelerometro"},
        ],
    }
    with pytest.raises(ValueError, match="Nenhum registro de giroscópio"):
        process_marcha(payload)


def test_process_marcha_rejects_stationary_capture():
    with open(STATIONARY_PATH) as f:
        payload = json.load(f)
    with pytest.raises(ValueError, match="Nenhum pico"):
        process_marcha(payload)


@pytest.mark.skipif(not os.path.exists(SAMPLE_PATH), reason="awaiting real walking capture")
def test_process_marcha_happy_path():
    with open(SAMPLE_PATH) as f:
        payload = json.load(f)

    result = process_marcha(payload)

    assert set(result.keys()) == EXPECTED_TOP_LEVEL_KEYS

    for v in _iter_floats(result):
        assert not math.isnan(v), "result contains NaN"

    json.dumps(result, allow_nan=False)

    assert result["n_peaks"] > 0
    assert len(result["cycles"]) > 0
    assert result["strategy"] in {"ascending", "descending", "constant", "undefined"}

    cycle_keys = {"peak_idx", "t_peak_s", "w_phoneX_peak_deg_s", "w_phoneY_peak_deg_s",
                  "w_phoneZ_peak_deg_s", "w_pred_deg_s"}
    for c in result["cycles"]:
        assert set(c.keys()) == cycle_keys

    assert len(result["t_s"]) == len(result["signal_deg_s"]) > 0
    assert len(result["peaks_t_s"]) == len(result["peaks_value_deg_s"]) == len(result["peaks_pred_deg_s"])


@pytest.mark.skipif(not os.path.exists(SAMPLE_PATH), reason="awaiting real walking capture")
@mock_aws
def test_lambda_handler_s3_event():
    bucket = "test-bucket"
    raw_key = "raw/users/8/marcha/42_20260507_120000.json"
    expected_out_key = "processed/users/8/marcha/42_20260507_120000.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)

    with open(SAMPLE_PATH, "rb") as f:
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
    assert set(body_out.keys()) == EXPECTED_TOP_LEVEL_KEYS

    first_etag = obj["ETag"]
    first_modified = obj["LastModified"]

    lambda_handler(event, None)
    obj2 = s3.get_object(Bucket=bucket, Key=expected_out_key)
    assert obj2["ETag"] == first_etag
    assert obj2["LastModified"] == first_modified

    listing = s3.list_objects_v2(Bucket=bucket, Prefix="processed/")
    keys = {o["Key"] for o in listing.get("Contents", [])}
    assert keys == {expected_out_key}, f"unexpected processed objects: {keys}"


@mock_aws
def test_lambda_handler_writes_error_json_for_bad_payload():
    bucket = "test-bucket"
    raw_key = "raw/users/8/marcha/99_20260507_111058.json"
    expected_out_key = "processed/users/8/marcha/99_20260507_111058.json"
    expected_err_key = expected_out_key + ".error.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)

    with open(STATIONARY_PATH, "rb") as f:
        body = f.read()
    s3.put_object(
        Bucket=bucket,
        Key=raw_key,
        Body=body,
        ContentType="application/json",
        Metadata={
            "user-email": "cristhian@example.com",
            "test-type": "MARCHA",
            "test-execution-id": "99",
        },
    )

    event = {"Records": [{"s3": {"bucket": {"name": bucket}, "object": {"key": raw_key}}}]}

    lambda_handler(event, None)

    with pytest.raises(s3.exceptions.NoSuchKey):
        s3.get_object(Bucket=bucket, Key=expected_out_key)

    err_obj = s3.get_object(Bucket=bucket, Key=expected_err_key)
    err_body = json.loads(err_obj["Body"].read())
    assert "error" in err_body
    assert "Nenhum pico" in err_body["error"]
    assert err_body["raw_key"] == raw_key


@mock_aws
def test_lambda_handler_skips_non_marcha_keys():
    bucket = "test-bucket"
    raw_key = "raw/users/8/utt/1_20260507_120000.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)
    s3.put_object(Bucket=bucket, Key=raw_key, Body=b"{}", ContentType="application/json")

    event = {"Records": [{"s3": {"bucket": {"name": bucket}, "object": {"key": raw_key}}}]}
    lambda_handler(event, None)

    listing = s3.list_objects_v2(Bucket=bucket, Prefix="processed/")
    assert "Contents" not in listing
