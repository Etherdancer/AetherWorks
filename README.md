# Clear Space

**Clear Space** is a decentralized, hybrid content-sharing platform and social network. The user has absolute sovereignty over what is shared, what is kept private, and who they interact with. Privacy is the foundation, but we take a pragmatic approach to ensure global connectivity and Google Play compliance.

> **⚠️ WARNING:** This app operates on a strict *User-at-Own-Risk* model. Because core public data is stored on your physical device and broadcast locally, you are responsible for what you share. The developer assumes zero liability for data loss or the consequences of usage.

## Core Architecture & Features

1. **Hybrid Network Topology**:
   - **Local Public Network**: Public content operates on a zero-server mesh network. It broadcasts locally via Bluetooth Low Energy (BLE), Wi-Fi Direct, and Local LAN (mDNS/NSD).
   - **Global Trusted Sync**: Content shared with Trusted Contacts or Groups is routed globally over the internet.
2. **Minimal Cloud Footprint**: To enable global sync while preserving privacy, we use Firebase Firestore purely as an End-to-End Encrypted "Dead Drop" for text, and direct WebRTC connections for large media.
3. **Hardware-Backed Encryption**: All data is encrypted at rest using SQLCipher (AES-256-GCM) with keys bound to the Android Keystore's secure element (StrongBox/TEE).
4. **Decentralized Reputation**: Content quality is managed via an anonymous, cryptographic token-voting system. Duplicate votes from the same device are mathematically impossible.
5. **Physical Devices Only**: Emulators and virtual devices are strictly blocked to prevent bot networks and fake voting farms.

## Distribution & Compliance

Clear Space is distributed via both **Google Play** and **GitHub Releases**. 

To comply with Google Play's strict User Generated Content (UGC) policies, satisfy laws and regulations regarding content moderation, and ensure GDPR compliance:
- **Mandatory Moderation:** We use a dynamic **Client-Side Consensus** algorithm. Anonymous report tokens are passed through the network, and your device independently deletes abusive, illegal, or policy-violating content once a dynamically scaled threshold of reports is reached.
- **Privacy by Default & User Control:** Your private data is completely isolated and stays entirely on your device. Tracking is kept to the absolute minimum required for this safety moderation and global routing. We use this minimal tracking just to be able to publish on the Google Play store and to satisfy laws and regulations. We do not use analytics for marketing or advertising.

### How to Build

1. Clone the repository.
2. Ensure you have the Android SDK (API 36) installed.
3. Add your `google-services.json` to the `app/` directory if you are building with Firebase features enabled.
4. Build the APK using the standard Gradle wrapper:

```bash
./gradlew assembleDebug
# OR for release:
./gradlew assembleRelease
```

## Contributing

Clear Space balances extreme privacy with pragmatic real-world usability. Please review our [Contributing Guidelines](CONTRIBUTING.md) before submitting PRs. Open an issue to discuss major architectural changes, especially those involving network routing or data storage.

## Acknowledgements / Open Source Credits

Clear Space was built upon and inspired by several excellent open-source projects:
- **[AetherWorksCore](https://github.com/aetherworks):** Much of our underlying P2P architecture and database design evolved from the foundational F-Droid strict codebase of AetherWorks.
- **[CoffeeHouseInfo](https://github.com/coffeehouseinfo):** Our decentralized, anonymous `ReputationAgent` and PoW token voting system is a direct evolution of the reputation mechanisms originally designed for the CoffeeHouseInfo application.
- **Android Proximity Technologies:** We utilize Android's built-in network discovery (mDNS/NSD, Wi-Fi Direct, BLE), inspired by robust open-source `DiscoveryManager` library patterns.
- **[TensorFlow Lite](https://www.tensorflow.org/lite):** Powering our zero-server-cost, on-device AI moderation and NSFW image filtering.
- **[HeliBoard](https://github.com/Helium314/HeliBoard) & [Bitwarden](https://github.com/bitwarden/mobile):** Inspired our strict approaches to incognito keyboard usage and secure local vault storage.
- **Core Security & Networking Libraries:** We gratefully acknowledge the giants whose open-source libraries make secure Android development possible: [Signal Protocol](https://signal.org/), [Tor for Android (Guardian Project)](https://guardianproject.info/apps/org.torproject.android/), [Syncthing](https://syncthing.net/), [SQLCipher](https://www.zetetic.net/sqlcipher/), and [BouncyCastle](https://www.bouncycastle.org/).
- **Media & Utilities:** [ExoPlayer](https://exoplayer.dev/), [ZXing](https://github.com/zxing/zxing), and [OSMDroid](https://github.com/osmdroid/osmdroid).

## License
This project is licensed under the **GNU Affero General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
