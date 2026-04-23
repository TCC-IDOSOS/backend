import json
import math
import os

# Set AWS env vars before importing lambda_function — it creates a module-level
# boto3 client at import time, which requires a region and credentials.
os.environ.setdefault("AWS_DEFAULT_REGION", "us-east-1")
os.environ.setdefault("AWS_ACCESS_KEY_ID", "testing")
os.environ.setdefault("AWS_SECRET_ACCESS_KEY", "testing")

import boto3  # noqa: E402
from moto import mock_aws  # noqa: E402

from processor import process_utt  # noqa: E402
from lambda_function import lambda_handler  # noqa: E402


FIXTURE_PATH = os.path.join(os.path.dirname(__file__), "fixtures", "utt_sample.json")

EXPECTED_TOP_LEVEL_KEYS = {
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


def _iter_floats(value):
    if isinstance(value, dict):
        for v in value.values():
            yield from _iter_floats(v)
    elif isinstance(value, list):
        for v in value:
            yield from _iter_floats(v)
    elif isinstance(value, float):
        yield value


def test_process_utt_happy_path():
    with open(FIXTURE_PATH) as f:
        payload = json.load(f)

    result = process_utt(payload)

    assert set(result.keys()) == EXPECTED_TOP_LEVEL_KEYS

    for v in _iter_floats(result):
        assert not math.isnan(v), "result contains NaN"

    json.dumps(result, allow_nan=False)


@mock_aws
def test_lambda_handler_s3_event():
    bucket = "test-bucket"
    raw_key = "raw/users/8/utt/42_20260421_205551.json"
    expected_out_key = "processed/users/8/utt/42_20260421_205551.json"

    s3 = boto3.client("s3", region_name="us-east-1")
    s3.create_bucket(Bucket=bucket)

    with open(FIXTURE_PATH, "rb") as f:
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

    event = {
        "Records": [
            {"s3": {"bucket": {"name": bucket}, "object": {"key": raw_key}}}
        ]
    }

    lambda_handler(event, None)

    obj = s3.get_object(Bucket=bucket, Key=expected_out_key)
    body_out = json.loads(obj["Body"].read())
    assert set(body_out.keys()) == EXPECTED_TOP_LEVEL_KEYS

    first_etag = obj["ETag"]
    first_modified = obj["LastModified"]

    # Idempotency: same event again must not overwrite the processed object.
    lambda_handler(event, None)
    obj2 = s3.get_object(Bucket=bucket, Key=expected_out_key)
    assert obj2["ETag"] == first_etag
    assert obj2["LastModified"] == first_modified

    # No error file should have been produced for a happy-path payload.
    listing = s3.list_objects_v2(Bucket=bucket, Prefix="processed/")
    keys = {o["Key"] for o in listing.get("Contents", [])}
    assert keys == {expected_out_key}, f"unexpected processed objects: {keys}"
