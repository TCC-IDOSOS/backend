"""
AWS Lambda handler for UTT signal processing.

Triggered by S3 ObjectCreated events on raw/users/**/*.json (configured outside
the code). For each record:
  1. Skip if /utt/ is not a path segment of the key (cheap key-based filter).
  2. Idempotency: skip if the swapped processed key already exists.
  3. Read object metadata, GET payload, sanity-check tipo_teste matches the key.
  4. Run process_utt; on ValueError write a sibling .error.json (do not raise);
     on any other exception, log the traceback and re-raise so Lambda retries.
  5. Write the result JSON to the swapped processed key.

# TODO: HU-XX (post-MVP) — notify backend when processing completes so it can
# populate TestExecutionModel.processedS3Key. Options: HTTPS webhook, or a
# second S3 event notification on processed/ triggering a backend endpoint.

# TODO: when a MARCHA Lambda is added, both lambdas will receive all
# raw/users/ events. Current key-based filter handles this, but consider
# splitting buckets or using S3 Object Tags + EventBridge rules if traffic
# grows.
"""

import json
import logging
import time
from urllib.parse import unquote_plus

import boto3
from botocore.exceptions import ClientError

from processor import process_utt


logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

s3 = boto3.client("s3")

RAW_PREFIX = "raw/"
PROCESSED_PREFIX = "processed/"
UTT_SEGMENT = "utt"


def _swap_to_processed(raw_key: str) -> str:
    if not raw_key.startswith(RAW_PREFIX):
        raise ValueError(f"key does not start with {RAW_PREFIX!r}: {raw_key}")
    return PROCESSED_PREFIX + raw_key[len(RAW_PREFIX):]


def _key_is_utt(raw_key: str) -> bool:
    parts = raw_key.lower().split("/")
    return UTT_SEGMENT in parts


def _exists(bucket: str, key: str) -> bool:
    try:
        s3.head_object(Bucket=bucket, Key=key)
        return True
    except ClientError as e:
        code = e.response.get("Error", {}).get("Code", "")
        if code in ("404", "NoSuchKey", "NotFound"):
            return False
        raise


def _put_json(bucket: str, key: str, body: dict) -> None:
    s3.put_object(
        Bucket=bucket,
        Key=key,
        Body=json.dumps(body, allow_nan=False).encode("utf-8"),
        ContentType="application/json",
    )


def _process_record(bucket: str, raw_key: str) -> None:
    log_extra = {"bucket": bucket, "raw_key": raw_key}

    if not _key_is_utt(raw_key):
        logger.info("skipping non-utt object", extra=log_extra)
        return

    out_key = _swap_to_processed(raw_key)
    err_key = out_key + ".error.json"
    log_extra["out_key"] = out_key

    if _exists(bucket, out_key):
        logger.info("skipping already-processed key", extra=log_extra)
        return

    head = s3.head_object(Bucket=bucket, Key=raw_key)
    metadata = head.get("Metadata", {}) or {}
    log_extra.update(
        user_email=metadata.get("user-email"),
        test_type=metadata.get("test-type"),
        test_execution_id=metadata.get("test-execution-id"),
    )

    obj = s3.get_object(Bucket=bucket, Key=raw_key)
    payload = json.loads(obj["Body"].read())

    payload_test_type = str(payload.get("tipo_teste", "")).strip().upper()
    t0 = time.perf_counter()
    try:
        if payload_test_type != "UTT":
            raise ValueError(
                f"key has /utt/ segment but payload tipo_teste={payload_test_type!r}"
            )
        result = process_utt(payload)
    except ValueError as e:
        elapsed_ms = int((time.perf_counter() - t0) * 1000)
        logger.warning(
            "process_utt rejected payload",
            extra={**log_extra, "elapsed_ms": elapsed_ms, "error": str(e)},
        )
        _put_json(bucket, err_key, {"error": str(e), "raw_key": raw_key})
        return

    elapsed_ms = int((time.perf_counter() - t0) * 1000)
    _put_json(bucket, out_key, result)
    logger.info(
        "processed",
        extra={
            **log_extra,
            "elapsed_ms": elapsed_ms,
            "cycles": len(result["cycles"]),
            "total_repeticoes": result["total_repeticoes"],
        },
    )


def lambda_handler(event, context):
    for record in event.get("Records", []):
        bucket = record["s3"]["bucket"]["name"]
        raw_key = unquote_plus(record["s3"]["object"]["key"])
        try:
            _process_record(bucket, raw_key)
        except Exception:
            logger.exception(
                "unexpected failure",
                extra={"bucket": bucket, "raw_key": raw_key},
            )
            raise
    return {"ok": True}
