# SafeRescue Phase 23 — Performance & Battery Optimization (Deepening Pass)

## Purpose
Phase 23 strengthens the existing performance/battery work from Phase 20. It does not reduce the reliability of the manual SOS path. Optional processing is adaptive, while emergency capture and countdown behavior remain outside the throttling policy.

## Changes
- Added a short 2-second cache to `AndroidPerformanceMonitor`.
- The cache uses `SystemClock.elapsedRealtime()` so wall-clock changes cannot distort performance decisions.
- Added `invalidateCache()` for lifecycle or explicit refresh points.
- Reduced repeated Android battery/power/thermal framework queries from the high-frequency voice loop.
- Expanded performance policy regression coverage for both voice and risk cadences.
- Added invariant tests ensuring every policy produces positive optional-work intervals.
- Version advanced to `0.15.0` / versionCode `15`.

## Emergency-safety invariant
Performance optimization is allowed to throttle optional metrics and risk recalculation only. It must not:
- disable manual SOS
- shorten or bypass the five-second SOS safety delay
- stop the two-minute emergency countdown
- disable encrypted evidence capture
- disable emergency location collection
- make camera/microphone failures cancel an emergency
- require network, AI, BLE or LoRa for manual SOS

## Why the cache matters
The voice repository processes audio continuously and previously queried the Android performance state repeatedly. A bounded cache avoids unnecessary framework work while keeping a two-second freshness window, which is appropriate for battery/thermal policy decisions rather than emergency-state timing.

## Test plan
1. Run unit tests with the Android/Gradle toolchain.
2. Verify normal, power-save, critical-battery and thermal policies.
3. Confirm manual SOS timing is unchanged.
4. Confirm voice capture remains continuous while metric callbacks become less frequent under constrained power.
5. Exercise screen-off and charging/discharging transitions on a physical device.
6. Measure battery drain during a 2-minute emergency with optional sensors enabled.

## Known limitations
- Full Android Gradle compilation/device battery profiling was not available in this environment.
- The 2-second cache is a performance optimization, not a source of emergency-state truth.
- Exact battery impact must be measured on representative physical devices because OEM thermal and power-management behavior varies.
