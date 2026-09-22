# SafeRescue Phase 16 — Safe Places + Weather

## Scope
Phase 16 adds contextual safety guidance using the latest location already collected by the emergency location subsystem.

### Live data sources
- Open-Meteo HTTPS API for current weather.
- OpenStreetMap Overpass HTTPS API for mapped police stations, hospitals, fire stations, community centres and libraries within 3 km.

## Privacy
The feature is user-triggered from the Safety tab. It does not independently request location permission. When refreshed, the current location coordinates are sent over HTTPS to the external data providers so they can return nearby/coordinate-specific data.

No location is sent when the user has no location fix or has not triggered the feature.

## Safety wording
Mapped places are guidance only. OpenStreetMap mapping is not a safety certification, and SafeRescue does not claim that a place is currently staffed, open, secure, or an emergency dispatch endpoint.

Weather is informational and can be stale or model-derived; it must not be treated as a guarantee of conditions at the user's exact position.

## Failure behavior
- Network failure does not affect the SOS state machine.
- Empty results are represented honestly.
- Remote responses are size-limited.
- HTTP redirects are not followed automatically.
- Only HTTPS endpoints are used.
- No API keys or backend secrets are embedded.

## Phase boundary
This phase does not implement navigation, emergency dispatch, police API integration, solo travel, or AI interpretation of weather.
