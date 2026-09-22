# Phase 7 — Skill Application

## Impeccable
- Kept the camera surface visually simple and focused on the emergency task.
- Used clear camera state, capture feedback and an explicit optional-permission path.

## Taste
- Reused SafeRescue Material 3 card language and restrained motion.
- Avoided adding decorative controls that could distract during an emergency.

## Motion
- Existing dashboard transitions remain intact.
- Camera interactions are immediate; no animation is allowed to delay capture or emergency controls.

## Cybersecurity
- Least-privilege CAMERA permission.
- No public media storage.
- No sensitive camera logging.
- Camera is an optional evidence subsystem and cannot become a single point of failure for SOS.
- Production encryption/upload/deletion semantics are intentionally deferred to the dedicated evidence phase.
