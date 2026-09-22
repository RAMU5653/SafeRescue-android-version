# Phase 1 — Secure Android Foundation

## Current status

Implemented as a clean starter project because no existing SafeRescue source tree was attached to this conversation.

## Baseline

- Kotlin: 2.1.10 plugin
- Gradle/AGP: Android Gradle Plugin 8.8.2
- compileSdk: 35
- targetSdk: 35
- minSdk: 26
- applicationId: `com.saferescue.app`
- UI: Jetpack Compose + Material 3
- Architecture direction: UI → future ViewModel/Use Case/Repository modules
- Database: intentionally not added in Phase 1
- Network: intentionally not added in Phase 1
- Emergency service: intentionally not added in Phase 1

## Security review

- Cleartext HTTP disabled.
- Backup disabled for the application foundation.
- No API keys, tokens, passwords, or OTPs are stored.
- No sensitive runtime permissions are requested yet.
- Demo credentials are gated behind `BuildConfig.DEMO_LOGIN_ENABLED` and are false in release builds.
- The demo login is local UI scaffolding only; it is not production authentication.

## Design review

The supplied SafeRescue background is used as the login visual. The first supplied infographic is preserved as a project reference under `docs/system-reference.png` and informs the dashboard information hierarchy.

Design direction:
- high-contrast safety status
- calm dark emergency-oriented foundation
- strong hierarchy around SAFE state
- large touch targets
- restrained motion for future emergency flows
- no decorative interaction that could distract from emergency controls

## Skills used as design/security references

- Impeccable — interface hierarchy, accessibility, audit/hardening mindset, and anti-pattern avoidance.
- Taste Skill — deliberate typography, spacing, layout variance, interaction polish, and anti-generic UI direction.
- Motion Skills — motion should be purposeful, bounded, and never compete with emergency actions.
- Anthropic Cybersecurity Skills — threat-oriented development, validation, least privilege, secure handling, and security testing direction.

These skills guide implementation; they are not runtime libraries or security controls by themselves.
