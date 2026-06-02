# AetherWorks

**AetherWorks** is a decentralized, serverless content-sharing platform and social network built for Android. All data lives on-device. The user has absolute sovereignty over what is shared, what is kept private, and who they interact with. Privacy is not a feature — it is the foundation.

> **Zero Servers. Zero Tracking. Zero Backdoors.**

## 🛡️ Core Principles

1. **Zero Servers:** There is no cloud. There is no backend. Every device is sovereign and communicates strictly peer-to-peer (P2P).
2. **Privacy by Default:** Profiles are fictional personas. The app never asks for personal data and starts in an isolated, offline mode every time it is opened.
3. **Informed Consent:** Every action that shares data requires an explicit, un-skippable confirmation. Once public, always public.
4. **Physical Devices Only:** The app actively detects and blocks emulators, VM farms, and bot networks to protect the integrity of the peer-to-peer network.
5. **No Law Enforcement Backdoors:** The app operates using end-to-end encryption with hardware-backed keys. It is mathematically and architecturally impossible to implement a backdoor.

## ✨ Key Features

- **The Gatekeeper (Encrypted Access):** Access is guarded by a master password enforced via a custom in-app secure keyboard (to defeat system keyloggers). Data is encrypted at rest using AES-256-GCM and SQLCipher, locked behind Android Keystore hardware-backed keys.
- **The Content Vault:** A three-tiered local storage system:
  - *Private Library:* A fully offline vault for passwords, 2FA codes, and Obsidian-style notes.
  - *Trusted-Only Library:* Content encrypted and shared exclusively with verified cryptographic contacts.
  - *Public Library:* Broadcasted to all connected peers over the local mesh.
- **Omni-Transport Discovery:** Seamlessly discovers nearby peers using Android Network Service Discovery (mDNS/NSD), Bluetooth Low Energy (BLE), Wi-Fi Direct, and local sockets — wrapped in **TLS 1.3** to defeat network eavesdropping (OWASP M5 mitigation).
- **Decentralized Reputation:** An anonymous token-voting system ensures community moderation without centralized servers. Spam is mitigated via low-difficulty Proof-of-Work nonces and token cardinality caps.
- **Cryptographic Personas:** Profiles are fictional. Relationships are verified out-of-band using Ed25519 public key cryptography (similar to Briar or Signal).

## ⚠️ Liability & Distribution

The developer assumes **zero liability** for the use of this application. Users explicitly assume all liability for the distribution of content (including pirated or illegal material). Users operate this app entirely at their own risk.

**F-Droid Compatibility**
AetherWorks is strictly built for F-Droid and GitHub Releases:
- **No Google Play Services** (No GMS dependencies).
- **No Proprietary Code** (100% Free Software).
- **No Telemetry** (No crash reporters, analytics, or tracking).

## 📄 License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License v3.0** as published by the Free Software Foundation. This program is distributed **WITHOUT ANY WARRANTY**; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports

Please send bug reports to: `etherdancer.zero553@aleeas.com`

## ⚡ Support & Donations

If you'd like to support the continued development of AetherWorks, you can leave a tip via the Lightning Network:

**Lightning Address / LNURL:** `lightning:your_lightning_address_here@domain.com`
