# Phase 16 Skill Application

## Impeccable
- Safety guidance is visually secondary to manual SOS.
- Clear loading, empty and unavailable states.
- Weather and place cards use compact hierarchy without implying certainty.

## Taste
- No fake map or fabricated place list.
- Live data is fetched only after the user asks for it.
- Existing emergency controls remain untouched.

## Motion
- Phase 16 uses existing controlled UI transitions only.
- No animation implies that police, family or emergency services received a message.

## Cybersecurity
- HTTPS-only external data providers.
- No API secrets in the APK.
- Response size limit to reduce memory abuse.
- Redirect following disabled.
- Network operations run on Dispatchers.IO.
- Exact coordinates are sent only after the user triggers the feature and only to the declared data providers.
- Results are treated as untrusted external data and displayed as guidance, not proof or dispatch status.
