# Security Policy

## Supported Versions
Security updates are provided for the latest stable release of ClearSpace. Please ensure you are running the newest version available via Google Play or GitHub Releases.

| Version | Supported          |
| ------- | ------------------ |
| 0.3.x   | :white_check_mark: |
| < 0.3   | :x:                |

## Reporting a Vulnerability

ClearSpace takes security and privacy very seriously because it is the core foundation of our application. If you have discovered a vulnerability, we appreciate your help in disclosing it to us in a responsible manner.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, please send an email to `security@ClearSpace.org` (placeholder, contact the developer directly if this is a personal repo).

Please include:
* A description of the vulnerability.
* Steps to reproduce the issue.
* What versions you found it in.

## Security Principles
* **Hybrid Architecture:** Clear Space operates primarily P2P with minimal E2E encrypted Google Play/Firebase traffic for notifications and public moderation.
* **Encryption:** All local storage is encrypted using SQLCipher. 
* **Zero Telemetry:** The app collects zero usage data or crash reports.

We will review your report and aim to address confirmed vulnerabilities immediately.

