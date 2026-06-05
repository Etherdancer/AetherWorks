# Contributing to Clear Space

Thank you for your interest in contributing to Clear Space! Clear Space is a decentralized, privacy-first social platform utilizing a hybrid architecture.

## Hybrid Architecture & Dependencies
While we prioritize on-device storage and local P2P mesh networking for public content, we pragmatically use cloud services (Firebase) for essential features:
- **Global Sync:** Trusted and Group content is routed through Firestore as an encrypted "dead drop".
- **Media Streaming:** Large media is streamed directly via WebRTC data channels.
- **Moderation:** Firebase is used for lightweight analytics and content hash blacklists to comply with Google Play's strict UGC policies.

When submitting code, ensure that:
1. Public content remains strictly offline/P2P.
2. Any data hitting Firestore is End-to-End Encrypted (E2E) on the client side before transmission.
3. Telemetry is kept to the absolute minimum required for crash reporting and store compliance.

## Getting Started
1. Fork the repository on GitHub.
2. Clone your fork locally.
3. Obtain a valid `google-services.json` (or use the mock config for local testing) and place it in the `app/` directory.
4. Build the project using `./gradlew assembleDebug`. Ensure there are no build errors.

## Code Style
- We use standard Kotlin formatting (ktlint).
- Keep code clean, plain, and well-documented.
- Emphasize simple, readable solutions over overly clever abstractions.

## Submitting Pull Requests
1. Create a feature branch (`git checkout -b feature/your-feature-name`).
2. Make your changes, ensuring they align with the privacy and moderation architectures.
3. Write clear commit messages.
4. Push to your branch and open a Pull Request against `core`.
5. Ensure all GitHub checks pass.

