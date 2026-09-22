#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD="$ROOT/app/build.gradle.kts"
MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"
FAIL=0
pass(){ echo "[PASS] $1"; }
fail(){ echo "[FAIL] $1"; FAIL=1; }

grep -q 'versionName = "1.0.0"' "$BUILD" && pass "release version is 1.0.0" || fail "release version is not 1.0.0"
grep -q 'buildConfigField("boolean", "DEMO_LOGIN_ENABLED", "false")' "$BUILD" && pass "release demo login disabled" || fail "release demo login is enabled/missing"
grep -q 'usesCleartextTraffic="false"' "$MANIFEST" && pass "cleartext traffic disabled" || fail "cleartext traffic policy missing"
grep -q 'android:exported="false"' "$MANIFEST" && pass "emergency service not exported" || fail "emergency service export check failed"

grep -n 'BACKEND_BASE_URL' "$BUILD" | grep -q 'release' || true
if grep -RInE 'AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{20,}' "$ROOT/app/src" >/dev/null 2>&1; then
  fail "obvious credential pattern found in app source"
else
  pass "no obvious credential pattern in app source"
fi

if find "$ROOT/app/src/main" -type f \( -name '*.kt' -o -name '*.java' \) -print0 | xargs -0 grep -nE 'http://[^/[:space:]]+' >/dev/null 2>&1; then
  fail "HTTP URL found in app source"
else
  pass "no HTTP URL found in app source"
fi

if [[ $FAIL -ne 0 ]]; then
  echo "Release preflight FAILED"
  exit 1
fi
echo "Release preflight PASSED"
