#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
fail=0
pass() { printf '[PASS] %s\n' "$1"; }
failcheck() { printf '[FAIL] %s\n' "$1"; fail=1; }
if grep -q 'android:usesCleartextTraffic="false"' app/src/main/AndroidManifest.xml; then pass 'cleartext traffic disabled'; else failcheck 'cleartext traffic disabled'; fi
if grep -q 'android:exported="false"' app/src/main/AndroidManifest.xml; then pass 'emergency service is not exported'; else failcheck 'emergency service is not exported'; fi
if ! grep -RInE "baseUrl[[:space:]]*=[[:space:]]*[\"']http://" app/src/main/java >/dev/null 2>&1; then pass 'no HTTP backend URL in source'; else failcheck 'no HTTP backend URL in source'; fi
if ! grep -RInE '(X509TrustManager|HostnameVerifier|setSSLSocketFactory)' app/src/main/java >/dev/null 2>&1; then pass 'no custom trust-manager bypass'; else failcheck 'no custom trust-manager bypass'; fi
if ! grep -RInE '(sk-[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{20,}|ghp_[A-Za-z0-9]{20,})' app/src/main >/dev/null 2>&1; then pass 'no hard-coded production API key patterns'; else failcheck 'no hard-coded production API key patterns'; fi
if grep -A8 '^        release {' app/build.gradle.kts | grep -q 'DEMO_LOGIN_ENABLED.*false'; then pass 'release demo login disabled'; else failcheck 'release demo login disabled'; fi
if grep -q 'url.protocol != "https"' app/src/main/java/com/saferescue/app/core/backend/SecureBackendClient.kt; then pass 'backend enforces HTTPS in client'; else failcheck 'backend enforces HTTPS in client'; fi
if grep -q 'code == 401 || code == 403 -> BackendUploadResult(UploadOutcome.FATAL' app/src/main/java/com/saferescue/app/core/backend/SecureBackendClient.kt; then pass 'backend 401/403 are fatal'; else failcheck 'backend 401/403 are fatal'; fi
exit "$fail"
