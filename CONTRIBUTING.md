# Contributing to AetherWorks

Thank you for your interest in contributing to AetherWorks! AetherWorks is a zero-server, decentralized, privacy-first social platform.

## F-Droid Compatibility is Mandatory
Any code submitted **must** strictly adhere to the F-Droid Inclusion Policy.
- **No Google Play Services.** No dependencies on `com.google.android.gms` or Firebase.
- **No Proprietary Binary Blobs.** All code must be open-source and GPL-3.0 compatible.
- **No Telemetry.** Do not introduce analytics, crash reporting, or tracking.

## Getting Started
1. Fork the repository on GitHub.
2. Clone your fork locally.
3. Build the project using `./gradlew assembleDebug`. Ensure there are no build errors.

## Code Style
- We use standard Kotlin formatting (ktlint).
- Keep code clean, plain, and well-documented.
- Emphasize simple, readable solutions over overly clever abstractions.

## Submitting Pull Requests
1. Create a feature branch (`git checkout -b feature/your-feature-name`).
2. Make your changes, ensuring they don't break existing offline features or introduce server dependencies.
3. Write clear commit messages.
4. Push to your branch and open a Pull Request against `core`.
5. Ensure all GitHub checks pass.
