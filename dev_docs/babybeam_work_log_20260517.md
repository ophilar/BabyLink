# BabyLink Work Log - 2026-05-17

## Strategic Intent
Perform a deep code review, ground implementation in design/technical best practices, and identify technical debt (hardcodes, mocks).

## Actions Taken
- Conducted deep review of `SecurityUtil`, `WebRtcManager`, `BabyMonitorViewModel`, and `ListeningScreen`.
- Identified security vulnerability: WebRTC signaling (SDP/ICE) currently bypasses RSA signature verification.
- Research Android 15 16KB page size alignment requirements for WebRTC and LiteRT.
- Audited hardcoded mock data in UI and ViewModel.

## Findings
- **Security**: TOFU is implemented for legacy alerts but not for modern JSON-based signaling.
- **Mocks**: Hardcoded "Parent's Phone" connection and sensor data (Temp/Humidity) in UI.
- **Android 15**: Current WebRTC/LiteRT dependencies need upgrades for 16KB alignment compliance.
- **Key Storage**: Using `SharedPreferences` for RSA keys; should migrate to hardware-backed `AndroidKeyStore`.

## Next Steps
- [ ] Refactor `SecurityUtil` to use `AndroidKeyStore`.
- [ ] Unify signaling protocol to ensure all messages are signed and verified.
- [ ] Remove hardcoded mocks from production code paths.
- [ ] Upgrade native dependencies for 16KB alignment.
