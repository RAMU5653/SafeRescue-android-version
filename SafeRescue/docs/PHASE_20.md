# SafeRescue Phase 20 — Performance + Battery Optimization

## Purpose
Reduce avoidable CPU/UI callback work while preserving emergency reliability and evidence capture.

## Architecture
- `AndroidPerformanceMonitor` reads battery, power-save and thermal state using Android system APIs.
- `PerformancePolicy` maps that state to bounded optional-processing cadences.
- Emergency countdown remains one-second based and is not throttled.
- GPS emergency request remains at the safety baseline of 10 seconds; performance mode does not silently weaken emergency location reliability.
- Voice PCM capture continues continuously; only metric callbacks/risk processing are throttled.

## Data flow
System battery/thermal state → `PerformancePolicy` → optional metric/risk callback cadence.
Raw voice evidence → encrypted app-private evidence store continuously, independent of callback throttling.

## Permissions
No new runtime permissions.

## Security considerations
- No battery data leaves the device.
- No secrets or evidence are stored in performance state.
- No network behavior is disabled for confirmed emergencies.
- No emergency countdown or manual SOS path depends on the optimization layer.

## Failure modes
If battery/thermal information is unavailable, the monitor defaults to a conservative normal profile. The emergency service and evidence paths continue independently.

## Testing
- Unit tests cover normal, power-save, critical battery and thermal-constrained policy selection.
- Verify emergency countdown still ticks every second.
- Verify voice evidence continues to grow even when metric callbacks are less frequent.

## Known limitations
Android OEMs may apply additional background/battery policies outside the app's control. This phase does not request battery-optimization exemption because that would increase battery impact and is not necessary for the emergency foreground-service design.
