# Phase 4 — Skill Application

## Impeccable
No emergency business logic was placed in Composables. State names are explicit and user-facing UI can later render distinct states instead of relying on ambiguous booleans.

## Taste
The state model avoids UI-driven architecture and reserves clear semantics for active, cancellation, confirmed and failure states.

## Motion
No motion is introduced into the emergency engine. Later UI motion should be tied to state transitions, remain deterministic, and never be required to understand whether an emergency is active.

## Cybersecurity skills
Threat-model thinking is applied to invalid transitions, cancellation/upload separation, manual-SOS priority, fail-closed transition handling, and future persistence/network boundaries.
