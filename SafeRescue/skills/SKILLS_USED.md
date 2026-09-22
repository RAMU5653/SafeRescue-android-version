# Skills applied to SafeRescue

This project follows the requested skill families for design and security work.

## Impeccable
Official repository: https://github.com/pbakaus/impeccable

Applied to:
- product/design documentation
- hierarchy and spacing review
- accessibility-minded UI
- anti-pattern avoidance
- hardening/audit mindset

## Taste Skill
Official repository: https://github.com/Leonxlnx/taste-skill

Applied to:
- non-generic visual hierarchy
- deliberate spacing and typography
- restrained card usage
- visual rhythm
- interaction polish

## Motion Skills
Official repository: https://github.com/iart-ai/motion-skills

Applied as a rule for future motion:
- motion must communicate state/progress
- emergency interactions must remain calm and deterministic
- no distracting animation around safety-critical actions

## Cybersecurity Skills
Reference: https://github.com/mukul975/Anthropic-Cybersecurity-Skills

Applied to:
- least privilege
- threat modeling
- validation mindset
- secure defaults
- security testing planning
- separation of demo/staging/release behavior

**Note:** these are engineering guidance/agent skills, not runtime security libraries. Actual security comes from the controls implemented in code and verified during testing.


## Phase 17
- Impeccable/Taste: keep optional hardware status honest and unobtrusive.
- Motion: hardware state never controls emergency timing.
- Cybersecurity skills: HMAC authentication, replay/staleness checks, bounded parsing, HTTPS-only gateway forwarding, secret injection, and fail-safe hardware isolation.
