#!/usr/bin/env python3
"""SafeRescue Phase 17 Raspberry Pi gateway reference implementation.

The Pi is a gateway, not an emergency decision-maker. An ESP32 + LoRa radio can
feed authenticated SRL1 packets to this process over USB serial. The gateway
validates HMAC, rejects stale/replayed packets, then optionally forwards a
compact event to an HTTPS SafeRescue backend.

Hardware-specific LoRa/SPI handling remains outside this process because the
radio may be attached to the ESP32 or to a different supported adapter.
"""
from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import re
import time
from dataclasses import dataclass
from typing import Optional

PROTOCOL = "SRL1"
MAX_PACKET_BYTES = 512
MAX_CLOCK_SKEW_MS = 5 * 60 * 1000
NONCE_TTL_SECONDS = 10 * 60
DEVICE_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}$")
NONCE_RE = re.compile(r"^[A-Za-z0-9_-]{8,64}$")


@dataclass(frozen=True)
class Packet:
    message_type: str
    device_id: str
    timestamp_ms: int
    state: str
    battery: Optional[int]
    latitude: Optional[float]
    longitude: Optional[float]
    accuracy: Optional[float]
    nonce: str


def _canonical(fields: list[str]) -> bytes:
    return "|".join(fields).encode("utf-8")


def parse_packet(line: bytes, secret: bytes, now_ms: Optional[int] = None) -> Packet:
    if len(line) > MAX_PACKET_BYTES:
        raise ValueError("packet too large")
    text = line.decode("utf-8").strip()
    fields = text.split("|")
    if len(fields) != 11 or fields[0] != PROTOCOL:
        raise ValueError("invalid packet format")
    if not secret:
        raise ValueError("gateway secret is not configured")
    expected = hmac.new(secret, _canonical(fields[:-1]), hashlib.sha256).digest()
    try:
        received = base64.b64decode(fields[-1], validate=True)
    except ValueError as exc:
        raise ValueError("invalid authentication tag") from exc
    if not hmac.compare_digest(received, expected):
        raise ValueError("authentication failed")

    device_id, nonce = fields[2], fields[9]
    if not DEVICE_RE.fullmatch(device_id) or not NONCE_RE.fullmatch(nonce):
        raise ValueError("invalid device or nonce")
    timestamp_ms = int(fields[3])
    now_ms = int(time.time() * 1000) if now_ms is None else now_ms
    if abs(now_ms - timestamp_ms) > MAX_CLOCK_SKEW_MS:
        raise ValueError("stale packet")

    battery = int(fields[5]) if fields[5] else None
    latitude = float(fields[6]) if fields[6] else None
    longitude = float(fields[7]) if fields[7] else None
    accuracy = float(fields[8]) if fields[8] else None
    if battery is not None and not 0 <= battery <= 100:
        raise ValueError("invalid battery")
    if latitude is not None and not -90 <= latitude <= 90:
        raise ValueError("invalid latitude")
    if longitude is not None and not -180 <= longitude <= 180:
        raise ValueError("invalid longitude")
    if accuracy is not None and accuracy < 0:
        raise ValueError("invalid accuracy")
    return Packet(fields[1], device_id, timestamp_ms, fields[4], battery, latitude, longitude, accuracy, nonce)


class ReplayGuard:
    def __init__(self) -> None:
        self._seen: dict[tuple[str, str], float] = {}

    def accept(self, packet: Packet, now: Optional[float] = None) -> bool:
        now = time.time() if now is None else now
        key = (packet.device_id, packet.nonce)
        # bounded cleanup
        cutoff = now - NONCE_TTL_SECONDS
        self._seen = {k: t for k, t in self._seen.items() if t >= cutoff}
        if key in self._seen:
            return False
        self._seen[key] = now
        return True


def forward_https(packet: Packet, url: str, bearer_token: str) -> None:
    if not url.startswith("https://"):
        raise ValueError("backend URL must use HTTPS")
    if not bearer_token:
        raise ValueError("backend bearer token is not configured")
    # stdlib-only reference. urllib follows no redirects here by policy.
    import urllib.error
    import urllib.request

    body = json.dumps({
        "source": "lora_gateway",
        "protocol": PROTOCOL,
        "messageType": packet.message_type,
        "deviceId": packet.device_id,
        "timestampMillis": packet.timestamp_ms,
        "state": packet.state,
        "batteryPercent": packet.battery,
        "latitude": packet.latitude,
        "longitude": packet.longitude,
        "accuracyMeters": packet.accuracy,
        "nonce": packet.nonce,
    }, separators=(",", ":")).encode()
    request = urllib.request.Request(
        url, data=body, method="POST",
        headers={"Authorization": f"Bearer {bearer_token}", "Content-Type": "application/json", "X-SafeRescue-Source": "lora-gateway"},
    )
    class _NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None

    opener = urllib.request.build_opener(_NoRedirect)
    try:
        with opener.open(request, timeout=8) as response:
            if not 200 <= response.status < 300:
                raise RuntimeError(f"backend returned HTTP {response.status}")
    except urllib.error.URLError as exc:
        raise RuntimeError("backend connection failed") from exc


def main() -> int:
    secret = os.environ.get("SAFERESCUE_LORA_SECRET", "").encode()
    serial_device = os.environ.get("SAFERESCUE_SERIAL_DEVICE", "")
    backend_url = os.environ.get("SAFERESCUE_BACKEND_URL", "")
    bearer = os.environ.get("SAFERESCUE_GATEWAY_TOKEN", "")
    if not secret:
        raise SystemExit("Set SAFERESCUE_LORA_SECRET; never hard-code it.")
    if not serial_device:
        raise SystemExit("Set SAFERESCUE_SERIAL_DEVICE to the ESP32 serial device.")

    try:
        import serial  # type: ignore
    except ImportError as exc:
        raise SystemExit("Install gateway/requirements.txt first.") from exc

    guard = ReplayGuard()
    with serial.Serial(serial_device, baudrate=115200, timeout=1) as port:
        while True:
            line = port.readline()
            if not line:
                continue
            try:
                packet = parse_packet(line, secret)
                if not guard.accept(packet):
                    print("Rejected replayed packet")
                    continue
                print(json.dumps(packet.__dict__, separators=(",", ":")))
                if backend_url:
                    forward_https(packet, backend_url, bearer)
            except (ValueError, RuntimeError) as exc:
                # Do not log secrets or raw authenticated packet material.
                print(f"Rejected/failed gateway packet: {exc}")


if __name__ == "__main__":
    main()
