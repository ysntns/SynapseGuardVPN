# SynapseGuard VPN 1.1.0

## Highlights
- Added CI- and Gradle-friendly release signing configuration that can read keystore credentials from `gradle.properties` or environment variables.
- Improved release signing validation with explicit missing-property diagnostics and CI guardrails when secrets are absent.
- Improved onboarding with explicit VPN and notification permission prompts on the home screen.
- Persisted split tunneling selections using DataStore so bypassed apps survive restarts.
- Stabilized VPN service state propagation so UI surfaces live connection status and traffic metrics from the foreground service.

## Privacy
The app requests VPN permission to create the secure tunnel and POST_NOTIFICATIONS (Android 13+) to surface important connection status updates. No personal data is collected beyond what is required to operate the VPN connection and user-selected preferences.
