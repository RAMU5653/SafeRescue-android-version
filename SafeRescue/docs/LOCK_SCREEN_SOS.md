# SafeRescue Lock-Screen SOS

SafeRescue now exposes an Android-supported emergency access surface from the phone lock screen.

## User flow

1. SafeRescue posts an ongoing `Emergency Access` notification when notification permission is available.
2. If the device permits lock-screen notifications, the notification can appear on the secure lock screen.
3. The notification contains an `SOS` action.
4. Tapping `SOS` opens `LockScreenSosActivity`, which uses Android's `showWhenLocked` / `turnScreenOn` APIs.
5. The user holds the small side-positioned SOS control for 5 seconds.
6. SafeRescue starts the existing emergency foreground service.

## Security boundary

- The feature does **not** unlock the phone.
- It does **not** bypass PIN, password, pattern, or biometric authentication.
- `LockScreenSosActivity` is `exported=false`.
- No sensitive personal data is shown in the lock-screen notification.
- The emergency service remains `exported=false`.
- The existing emergency workflow remains responsible for the 2-minute countdown, monitoring, evidence, and cancellation behavior.

## Device behavior

Android and device manufacturers control whether notifications/actions are visible on a secure lock screen. The app cannot force a notification to appear if the user has disabled lock-screen notifications or notification permission.

## Accessibility

The lock-screen SOS control exposes a TalkBack custom action that routes to the existing five-second accessible SOS delay. This preserves an emergency-safe non-touch path without bypassing the safety delay.


## Compact side-positioned control

The lock-screen emergency surface now uses a compact 96dp circular SOS control anchored to the bottom-end (side) of the available screen. The five-second continuous hold and accessibility action are preserved. The surrounding surface is intentionally translucent so the control does not dominate the lock screen.
