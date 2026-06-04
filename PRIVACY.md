# Privacy Policy for Clear Space

**Plain language:** This app is designed to maximize your privacy. It operates primarily offline and peer-to-peer. We do not collect your personal data, and we do not track you.

## 1. Data Collection and Usage
- **No Personal Data:** The app does not ask for or collect your real name, email, phone number, or location. Any profile you create is a fictional persona and remains entirely on your device unless you explicitly choose to share it.
- **Firebase Cloud Messaging (FCM):** To preserve battery life while allowing peers to connect, the app uses Firebase Cloud Messaging to send lightweight "wake-up" pings to devices. This requires generating an anonymous device token. This token is not linked to your identity or any personal information.
- **Content Moderation:** To comply with Google Play's User Generated Content (UGC) policies, the app occasionally connects to a Firebase database solely to download a cryptographic hash-blacklist of abusive or illegal content, ensuring it is removed from the local network.

## 2. Data Storage and Encryption
- **Encrypted Local Vault:** All your content, including private notes and passwords, is stored locally in an encrypted database (SQLCipher). The key is hardware-backed (Android Keystore) and secured by a password only you know. We cannot access your data.
- **No Cloud Backups:** We explicitly disable Android system backups to ensure your private database never leaves your physical device without your active consent.

## 3. Data Sharing (Peer-to-Peer)
- **Local Network & Bluetooth:** When you turn the "Sharing Toggle" ON, the app communicates with nearby devices via Bluetooth and Wi-Fi Direct. No persistent identifiers (MAC addresses, IMEIs) are exchanged.
- **Explicit Consent:** Every feature that changes content visibility to "Public" or "Trusted" triggers a mandatory confirmation dialog. You are always in control of what is shared.
- **End-to-End Encryption:** Any private messages sent to trusted users are end-to-end encrypted using hardware-backed keys.

## 4. Third-Party Services
- **Google Play Services:** Used strictly for battery-efficient push notifications (FCM) and core functionality. 
- **No Advertising:** We do not use advertising SDKs, and we do not share your data with advertisers.

## 5. User Responsibility
By using the app, you acknowledge that you are doing so entirely at your own risk. The developer is not responsible for any loss of data, privacy breaches, or consequences of local network sharing.
