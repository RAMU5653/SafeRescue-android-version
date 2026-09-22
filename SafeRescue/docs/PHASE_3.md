# SafeRescue Phase 3 — Dashboard + Navigation

## Current phase
Phase 3 implements the authenticated dashboard and bottom navigation foundation.

## Implemented
- Home, Safety, Evidence and Me tabs.
- Dashboard hierarchy inspired by the supplied SafeRescue reference design.
- SAFE status card without claiming live sensor state.
- Clear placeholder states for later phases.
- Profile/session surface with logout.
- Navigation state isolated in `HomeViewModel`.
- UI logic kept out of the authentication layer.
- Accessible labels for major icons and navigation controls.
- Deterministic, restrained tab transition and subtle layout animation.

## Deliberately not implemented
SOS, countdown, camera, microphone, GPS, AI, risk engine, evidence capture, backend, Bluetooth and LoRa remain disabled until their specified phases.

The SOS button is visibly disabled and explicitly marked as Phase 5 rather than silently doing nothing.

## Security review
- No new permissions.
- No sensor starts from the dashboard.
- No sensitive authentication fields are displayed in the dashboard.
- No network calls or secrets added.
- No fake live location, AI, evidence or emergency status.
- Logout delegates to the existing secure session layer.

## Design skill application
- Impeccable: hierarchy, states and explicit disabled/coming-soon treatment.
- Taste: reduced card clutter, consistent spacing and purposeful typography.
- Motion: deterministic tab fade and restrained layout animation; no motion is required to understand an emergency state.
- Cybersecurity skills: least privilege, truthful status presentation, no sensitive data exposure and phase-gated security boundaries.

## Test plan
- Unit-test default Home tab.
- Unit-test tab selection.
- Verify login reaches dashboard.
- Verify all four tabs render.
- Verify logout returns to login.
- Verify SOS is disabled and cannot trigger an emergency in Phase 3.
