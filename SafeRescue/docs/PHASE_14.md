# SafeRescue — Phase 14: Trusted Contacts + Notifications

## Scope
Phase 14 adds a local trusted-contact manager and Android notification surface without changing the emergency state machine or claiming external delivery.

## Implemented
- Up to 3 trusted contacts.
- Add, edit, remove and explicit local verification/unverification.
- Contact name and phone number stored in an encrypted AES-GCM document protected by Android Keystore.
- Phone numbers masked in the UI.
- Duplicate phone-number prevention and basic input bounds.
- Android 13+ `POST_NOTIFICATIONS` permission flow.
- Dedicated SafeRescue alerts notification channel.
- Local test notification for a verified contact.
- Emergency-status notification text that explicitly says external contact delivery is pending backend integration.
- Safety tab now contains the Phase 14 contact manager.

## Security notes
- No SMS, WhatsApp, police, or other external message is fabricated by this phase.
- Local "verified" means the user explicitly confirmed the contact on this device; it is not remote identity/OTP verification.
- No contact phone numbers are written to logs by the Phase 14 code.
- Contact data remains encrypted at rest in app-private storage.
- Notification content avoids exposing the phone number.

## Deferred
- Server-side contact storage and authorization.
- Real OTP verification of a trusted contact.
- Actual SMS/push/police endpoint delivery and delivery receipts.
- Contact notification retry/idempotency tied to the Phase 13 backend contract.

## Build status
The project does not include a Gradle wrapper and this environment does not provide the Android/Gradle toolchain, so an Android APK build cannot be honestly claimed here. Source-level checks are performed on the delivered project.
