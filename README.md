# AetherWorks

**AetherWorks** is a decentralized, zero-server content-sharing platform and social network. All data lives strictly on-device. The user has absolute sovereignty over what is shared, what is kept private, and who they interact with.

Privacy is not a feature — it is the foundation.

> **⚠️ WARNING:** This app operates on a strict *User-at-Own-Risk* model. Because there are no central servers, all data is stored on your physical device. Once you mark content as "Public" and the Sharing toggle is active, your data is visible to nearby peers. The developer assumes zero liability for data loss or the consequences of usage.

## Core Architecture & Features

1. **Zero Servers**: There is no "cloud" or backend. Every physical device is a sovereign node in a decentralized mesh network.
2. **Omni-Transport Discovery**: The app automatically negotiates P2P connections over:
   - Wi-Fi Direct (High bandwidth)
   - Local Network (mDNS/NSD)
   - Bluetooth Low Energy (BLE) / Classic (Fallback)
3. **Hardware-Backed Encryption**: All data is encrypted at rest using SQLCipher (AES-256-GCM) with keys bound to the Android Keystore's secure element (StrongBox/TEE).
4. **Decentralized Reputation**: Content quality is managed via an anonymous, cryptographic token-voting system. Duplicate votes from the same device are mathematically impossible, and "likes" are federated through a set-union algorithm.
5. **Physical Devices Only**: Emulators and virtual devices are strictly blocked to prevent bot networks and fake voting farms.

## F-Droid Compatibility & Build Requirements

AetherWorks is built specifically with the [F-Droid Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/) in mind. 
- **No Google Play Services (GMS)**
- **No Proprietary Dependencies**
- **No Telemetry or Crashlytics**
- **100% Free Software under GPL-3.0**

### How to Build

1. Clone the repository.
2. Ensure you have the Android SDK (API 36) installed.
3. Build the APK using the standard Gradle wrapper:

```bash
./gradlew assembleDebug
# OR for release:
./gradlew assembleRelease
```

## Contributing

Because AetherWorks adheres to strict privacy and F-Droid guidelines, any pull requests introducing closed-source libraries, cloud analytics, or GMS dependencies will be automatically rejected. Please open an issue to discuss major architectural changes before submitting a PR.

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
