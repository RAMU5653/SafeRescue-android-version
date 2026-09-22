# Phase 2 Skill Application

## Impeccable
- Preserve the SafeRescue visual identity instead of replacing the supplied brand image.
- Treat login, registration, OTP, success and error as distinct states.
- Keep primary actions prominent and touch-friendly.
- Avoid decorative motion or UI clutter around authentication.

## Taste Skill
- Use a restrained type hierarchy and spacing rhythm.
- Keep the debug account notice visually subordinate to the main authentication action.
- Avoid a generic dashboard-template look by giving the auth flow a clear safety-oriented visual identity.

## Motion Skills
- Phase 2 uses only a short deterministic crossfade between authentication states.
- No bounce, looping animation or attention-grabbing motion is used in an emergency product context.

## Cybersecurity Skills
- Passwords are hashed rather than stored as plaintext.
- OTP challenges are time-limited and attempt-limited.
- Session material is protected with Android Keystore-backed encryption.
- Debug behavior is isolated from the production architecture.
- The real backend/SMS boundary is explicit so the app does not fake production authentication.
