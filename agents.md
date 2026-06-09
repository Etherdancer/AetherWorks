# Clear Space — Agent Architecture & Behavioral Specification

**Clear Space** is a decentralized content-sharing platform and social network utilizing a "Hybrid Architecture." While the vast majority of data and logic lives on-device and relies on peer-to-peer connections, the application employs minimal server infrastructure (such as Firebase) for essential tasks like battery-efficient background wake-ups and community moderation to comply with Google Play policies. The user retains vast sovereignty over their private data. Privacy is deeply foundational, but balanced with pragmatism and store compliance.

> **License:** This program is free software: you can redistribute it and/or modify it under the terms of the **GNU Affero General Public License v3.0** as published by the Free Software Foundation. This program is distributed **WITHOUT ANY WARRANTY**; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [LICENSE](LICENSE) file for details.

> **⚠️ DISTRIBUTION TARGETS: GOOGLE PLAY AND GITHUB ("HYBRID" APPROACH)**
> This application is designed for release on **Google Play** and **GitHub Releases**. While the core philosophy emphasizes on-device processing and peer-to-peer data sharing, it MUST comply with Google Play's Developer Policies. Specifically:
> - **Google Play Services and Firebase ARE Allowed.** Firebase is used for battery-efficient push notifications (FCM) to wake up peers, and other Google services can be used if they significantly improve the user experience or comply with store policies.
> - **User Generated Content (UGC) Moderation.** The app MUST include mechanisms to report and remove objectionable content (e.g., via hash-based blacklists managed on Firebase) to comply with Google Play's strict UGC rules. Absolute immutability ("Once Public, Always Public") is superseded by the ability to moderate illegal or abusive content.
> - **Pragmatic Privacy.** While privacy remains a strong focus, it is not "at all costs." If a feature requires a lightweight server assist (like moderation or peer wake-ups) to function reliably on modern Android devices, it is permitted.
> - **No tracking, analytics, or advertising** (unless explicitly approved by the user). We still aim for a clean, user-respecting experience.
>
> **If you are an agent working on this project, ensure that your architectural decisions balance decentralization with Google Play compliance.**

---

## Writing Style for All User-Facing Text

- **Plain language always.** Write like you're explaining something to a curious 10-year-old.
- Avoid jargon. If a technical term is unavoidable, explain it in plain words immediately after.
- Short sentences. Short paragraphs. One idea at a time. No buzzwords.
- When describing how something works, lead with an accurate technical statement, then follow it immediately with a plain-language version:

> **Technical:** All data is encrypted at rest using AES-256-GCM with hardware-backed keys from the Android Keystore.
> **Plain:** Everything you save in the app is scrambled with a lock that only your phone's secure chip can open — even if someone takes your phone apart, they can't read it.

---

## Core Principles

1. **Hybrid Architecture (Absolute Minimal Servers).** While 99.9% of logic and data remains on-device and peer-to-peer, minimal server infrastructure (like Firebase) is used for essential "traffic cop" tasks (e.g., wake-up pings, moderation blacklists) to ensure battery efficiency and store compliance. **CRITICAL RULE: Server usage must be kept to an absolute minimum.** Do not offload computation, storage, or normal data exchange to the cloud. Always default to local processing and P2P exchange first. It is crucial to keep the server as small as possible and to keep the network manageable and moderated by one single person, this is essential.
2. **Privacy by Default, but Pragmatic.** The safest choice is always the one that collects the least. The app never asks for personal data. Profiles are fictional personas.
3. **Informed Consent at Every Step.** Every action that sends or receives data requires explicit user consent. The user is told exactly what will happen before it happens.
4. **Community Guidelines & Moderation.** The app strictly adheres to Google Play UGC policies. Abusive, illegal, or policy-violating content will be moderated and purged from the network using hash-based remote blacklists.
5. **Physical Devices Only.** The app runs exclusively on physical Android devices. Emulators and virtual devices are detected and blocked.
6. **No Law Enforcement Backdoors.** The app uses end-to-end encryption for private messaging.
7. **Moderated Public Square.** Content shared publicly propagates via P2P, but is subject to central moderation blacklists to prevent the spread of abusive material.
8. **Default Private & Mandatory Confirmation.** Every single feature, setting, and piece of content must default to **Private**. Any action that changes visibility to Public or shares data must trigger a mandatory confirmation dialog.
9. **User Satisfaction First.** The sole objective of this platform is user privacy, security, and satisfaction. It is a strictly non-commercial project, and no design decisions will ever be made for the sake of monetization. The developer accepts voluntary donations (currently in cryptocurrency, with real currency potentially supported in the future), but core functionality will never be paywalled.

---

## 1. The Gatekeeper Agent (Authentication & App Access)

Every time the app is opened, access is guarded.

* **Role:** Controls entry to the app via password, PIN, or biometric authentication.
* **Responsibilities:**
    * **Mandatory Authentication:** The app requires a password (or PIN / biometric) every single time it is opened — no "stay logged in" option.
    * **Secure Credential Storage:** The password hash is stored using Argon2id (or bcrypt as fallback) and never in plain text. Keys are hardware-backed via the Android Keystore (StrongBox / TEE).
    * **Incognito Input (Keyboard Tracking Prevention):** Enforces `android:imeOptions="flagNoPersonalizedLearning"` and `InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` on all sensitive fields to force standard keyboards (like Gboard) into Incognito Mode. A custom in-app keypad is provided for PINs to bypass system keyboards entirely (HeliBoard style).
    * **Brute-Force Protection:** After 5 failed attempts, enforce escalating lockout timers (30s → 60s → 5min → 15min). After 20 consecutive failures, wipe the app's encryption keys (data becomes irrecoverable).
    * **Emulator & Virtual Device Detection:** On every launch, the Gatekeeper runs a battery of environment checks:
        * `Build.FINGERPRINT`, `Build.MODEL`, `Build.MANUFACTURER`, `Build.PRODUCT`, `Build.HARDWARE` — check for known emulator strings (`generic`, `sdk`, `goldfish`, `ranchu`, `vbox`, `nox`, `bluestacks`, `genymotion`).
        * Check for `/dev/qemu_pipe`, `/system/lib/libc_malloc_debug_qemu.so`, `/sys/qemu_trace`.
        * Check `SensorManager` — emulators typically return 0 or 1 sensor; physical devices have 5+.
        * Check for telephony — `TelephonyManager` returns empty or fake IMEI on emulators.
        * If any of the above strict checks trigger, **the app refuses to run** and displays: *"This app can only be used on a physical device."*
    * **Custom ROM & Root Detection:** Detect custom ROMs (check `Build.TAGS` for `test-keys`) and rooted devices (check for `su` binary, Magisk, Xposed). Because many privacy-conscious users run custom ROMs, do **not** lock them out. Instead, warn the user that security cannot be guaranteed on a rooted or uncertified OS. The app continues to run, but displays a persistent warning banner.
    * **First-Time Onboarding:** On first launch, present a clear, plain-language disclaimer:
        * The app does not collect personal data.
        * Profiles are fictional — do not use your real name or identity.
        * The developer is not responsible for anything that happens from using this app.
        * The user accepts all risk.
        * The user must create a strong password to continue.

---

## 2. The Sharing Toggle Agent (Global Data Transmission Control)

A persistent, always-visible toggle on every screen.

* **Role:** Acts as the master switch for all data exchange with other devices.
* **Responsibilities:**
    * **Always Visible:** Rendered as a floating action element (FAB or persistent toolbar icon) that is present on every screen of the app without exception.
    * **Default State:** OFF. The app starts in fully offline, isolated mode every time.
    * **Enable Flow:** When the user taps the toggle to enable sharing:
        1. Display a consent dialog: *"You are about to enable data sharing. 'Public' content will be shared with nearby devices AND securely synced over the internet with your Remote Trusted Users. 'Trusted' content will securely sync over the internet only with authorized remote contacts. Your private data is completely isolated and stays entirely on your device. Do you want to continue?\n\nNote: Once public, content propagates to others and cannot be recalled. We use Firebase to receive background wake-up pings and for a centralized moderation blacklist. This minimal tracking is strictly necessary to publish on the Google Play Store and to satisfy laws and regulations regarding content moderation."*
        2. Present the risk disclosure: *"This app communicates with nearby devices over local networks (Bluetooth/Wi-Fi Direct) and syncs both Public and Trusted content globally over the internet with your Remote Contacts. On public networks, other people may detect that you are running this app. Because we use Firebase for essential background connectivity and moderation, Google servers will know your device is running this app.\n\nWe use minimal tracking solely for legal and store compliance. You retain complete control over what leaves your device. You are responsible for the content you choose to share. You use this feature at your own risk."*
        3. Only after explicit confirmation: start the Discovery Agent and P2P services.
    * **Disable Flow:** Immediately stops all discovery, closes all connections, unregisters all network services, and stops the background service. No lingering background activity.
    * **Visual Indicator:** When sharing is ON, a persistent colored indicator (e.g., pulsing green dot) is visible across all screens. Because sharing continues in the background, Android requires a persistent system notification. This notification must clearly state "Clear Space Sharing is Active" and provide a "Stop Sharing" button directly in the notification shade so the user always knows their sharing state.

---

## 3. The Discovery Agent (Peer Detection)

A background service that operates **only** when the Sharing Toggle is ON.

* **Role:** Find other Clear Space instances on nearby devices using **all available Android proximity technologies.**
* **Responsibilities:**
    * **Omni‑Transport Discovery & Connection:** The app relies on Android framework APIs and Firebase Cloud Messaging (FCM) to discover and wake up peers. The implementation cycles through and upgrades connections using:
        1. **Bluetooth Low Energy (BLE)** – continuous low‑power background discovery.
        2. **Bluetooth Classic** – medium‑range fallback data transfer.
        3. **Wi‑Fi Direct (Wi‑Fi P2P)** – high‑bandwidth direct connections.
        4. **Wi‑Fi Aware (NAN)** – continuous discovery on modern devices without an access point.
        5. **Wi‑Fi Hotspot (SoftAP)** – one device temporarily acts as a router for bulk transfers.
        6. **Local Wi‑Fi LAN (mDNS/NSD)** – peers on the same router communicate via standard local sockets.
        The discovery logic is encapsulated in a lightweight, open‑source `DiscoveryManager` library under the Apache‑2.0 license.
    * **Fallback: Manual NSD Discovery:** If any of the framework discovery methods fail, fall back to raw `NsdManager` (mDNS) + TCP sockets with TLS, as implemented in the legacy CoffeeHouseInfo's `DiscoveryAgent` and `SecureP2PManager`.
    * **Zero-Identifier Policy:** No device IDs, MAC addresses, IMEIs, or usernames are exchanged during discovery. The only broadcast data is: app service type, available content categories, and connection port.
    * **Presence Packet:** Broadcast a lightweight `PresencePacket` containing:
        * Available content category flags (from the category system)
        * Whether the peer has a profile (boolean, no profile data)
        * Connection endpoint info (transient, not stored)
    * **Background Execution (Foreground Service):** To allow continuous discovery and sharing while the user is not actively looking at the app, the Discovery Agent runs as an Android "Foreground Service" (`Service` with `startForeground()`). This allows the app to maintain socket connections and framework discovery even when the app is minimized or the screen is off, as long as the Sharing Toggle remains ON.

---

## 4. The Content Vault Agent (Local Storage & Privacy)

All user data lives in an encrypted local database that never leaves the device without explicit action.

* **Role:** Manage the on-device encrypted database and enforce the boundary between private and shareable data.
* **Responsibilities:**
    * **Encrypted Database:** Room (SQLite) database encrypted with SQLCipher. The decryption key is derived from the user's password + a hardware-backed key from the Android Keystore.
    * **Three Storage Tiers:**
        1. **Private Library (Offline Knowledge Base & Passwords):** Content the user creates or saves but does not share. Completely inaccessible to the network layer. Not included in any index broadcast. Contains:
            * **Password Vault:** Securely stores passwords and 2FA codes locally (Bitwarden style).
            * **Obsidian-Style Notes:** Robust offline Markdown editor supporting tags, bidirectional `[[linking]]`, and a local Graph View to connect private thoughts.
        2. **Trusted-Only Library** — Content shared exclusively with verified Trusted Users via encrypted channels.
        3. **Public Library** — Content available to all nearby Clear Space users when sharing is enabled.
    * **Content Creation Flow:** When a user creates new content, they must choose one of three visibility levels before saving:
        * 🔒 **Private** — Stored locally, never shared.
        * 🤝 **Trusted Users Only** — Shared exclusively with verified trusted contacts.
        * 🌐 **Public** — Available to all connected devices.
    * **Save to Device Storage:** The user can export individual content items from their Private Library to the device's standard file system (e.g., Downloads folder). This is a one-way copy — the exported file is a standard format (text, image, etc.) and is no longer protected by the app's encryption.
    * **Encrypted File Sharing (Cryptomator / Tresorit Style):** Users can export or share files directly. When shared with an acquaintance, the file is encrypted locally with an AES-256-GCM passphrase chosen by the user. It transfers P2P, and the receiver must enter the passphrase (sent out-of-band) to decrypt it.
    * **Data Isolation:** The Private Library is stored in a separate encrypted database file from the Public/Trusted libraries. Even if the Public database is somehow extracted, no Private data is accessible.
    * **Storage Quota & Eviction:** To prevent storage exhaustion attacks (malicious peers filling up your device), the Public Library has a strict maximum size quota (e.g., 500MB). When the quota is reached, the system automatically deletes the oldest or lowest-rated public content (LRU/Lowest-Reputation eviction).
    * **`android:allowBackup="false"`** — Prevent system backups from leaking app data.

---

## 5. The Content Indexer Agent (Browsing & Search)

Organizes and presents received content indexes for efficient, filterable browsing.

* **Role:** Provide a searchable, categorized view of content available from connected peers.
* **Responsibilities:**
    * **Mastodon-Style Microblogging UI:** In addition to long-form content, the Indexer supports a chronological feed of short "Notes" (Toots) broadcast over the P2P indexer. Includes support for Content Warnings (CWs) and hashtags.
    * **Lightweight Index Transfer:** When peers connect, only `ContentHeader` objects are exchanged initially — not full content. Headers contain: title, category flags, emotion flags, reputation score (as raw integer counts), import count, timestamp, content hash, author alias, and source trust level.
    * **Unverified Claims:** Because headers only contain integer scores (to save bandwidth) rather than the full cryptographic token sets, these scores are treated as **unverified claims**. The UI visually distinguishes these from verified local scores until the full content (and its token set) is imported and cryptographically verified.
    * **Content Categories (Category Flags):** A comprehensive set of content type flags. The user selects one or more when creating content. Used for filtering when browsing:
        * `Politics`, `Religion`, `Sports`, `Music`, `News`, `Science`, `Technology`, `Art`, `Literature`, `History`, `Philosophy`, `Health`, `Food`, `Travel`, `Nature`, `Comedy`, `Education`, `DIY`, `Finance`, `Fashion`, `Gaming`, `Movies`, `TV`, `Photography`, `Podcasts`, `Automotive`, `Pets`, `Parenting`, `Relationships`, `Career`, `Fitness`, `Mental Health`, `Culture`, `Language`, `Architecture`, `Gardening`, `Crafts`, `Collecting`, `Volunteering`, `Local Events`, `Other`
    * **Emotion Flags (How It Made You Feel):** A wide range of human emotions. Users can tag content with one or more. Used for filtering:
        * `Happy`, `Sad`, `Cheerful`, `Angry`, `Inspired`, `Anxious`, `Calm`, `Excited`, `Nostalgic`, `Amused`, `Hopeful`, `Frustrated`, `Grateful`, `Confused`, `Proud`, `Disgusted`, `Surprised`, `Moved`, `Bored`, `Scared`, `Empowered`, `Lonely`, `Peaceful`, `Curious`, `Overwhelmed`, `Determined`, `Melancholic`, `Playful`, `Tender`, `Rebellious`
    * **Flag Selection UI:** Both category and emotion flags are presented in an **expandable/collapsible menu** that takes minimal screen space when collapsed (shows only a summary like "3 categories, 2 emotions selected") and expands into a scrollable chip/tag selector.
    * **Filter Logic Toggle:** A prominent AND/OR switch:
        * **AND mode:** Show content that matches ALL selected flags.
        * **OR mode:** Show content that matches ANY selected flag.
    * **Source Filters (Toggle-Based):**
        * 👥 **All Public** — Show all public content (default).
        * 🤝 **Acquaintances Only** — Show only content from users added as acquaintances.
        * 🔐 **Trusted Users Only** — Show only content from verified trusted users.
    * **Sorting Options** (carried over and extended from CoffeeHouseInfo):
        * **Reputation Score** — `(Likes - Dislikes)`, highest first.
        * **Popularity** — By import/save count.
        * **Chronological** — Newest first.
        * **Alphabetical** — A–Z by title.

---

## 6. The Reputation Agent (Decentralized Anonymous Rating System)

A fully anonymous, decentralized quality-control mechanism — directly evolved from CoffeeHouseInfo's `ReputationAgent`.

* **Role:** Track the reputation of content through community feedback without ever identifying who voted.
* **Responsibilities:**
    * **Like / Dislike Buttons:** Every piece of content has a Like (👍) and Dislike (👎) button.
    * **Anonymous Token Voting (from CoffeeHouseInfo):**
        * Each device has a unique, randomly generated `voterSecret` stored in the Android Keystore (upgraded from CoffeeHouseInfo's SharedPreferences storage for security).
        * When a user votes, a token is generated: `SHA-256(contentHash + voterSecret)`.
        * This token is added to the content's `likeTokens` or `dislikeTokens` set.
        * Since the token is deterministic per device + content, the same device can only produce one token per content item — **one unique anonymous vote per device.**
        * There is no way to reverse the token back to the device or user.
    * **Flag Voting:** Category and Emotion flags follow the same token mechanism — one anonymous flag vote per device per content item. A flag vote token: `SHA-256(contentHash + flagName + voterSecret)`.
    * **Anti-Exploitation Measures (improvements over CoffeeHouseInfo):**
        * **Proof of Work:** Every new content unit and every vote must include a valid PoW nonce. `SHA-256(data + nonce)` must have `N` leading zeros. To prevent severe battery drain and thermal throttling on mobile devices, the difficulty must be kept very low (e.g., default 1 or 2), relying primarily on local rate limiting to stop spam.
        * **Token Set Deduplication:** Tokens are stored as `Set<String>` — duplicate tokens from the same device are mathematically impossible to create and structurally impossible to insert.
        * **Emulator Blocking:** Since the app does not run on emulators/virtual devices, attackers cannot spin up VM farms to generate fake voter secrets.
        * **Rate Limiting:** Maximum 50 votes per hour per device. Enforced locally.
        * **Token Cardinality Caps:** If a single content item arrives with more than 10,000 tokens in any set, the excess tokens are trimmed (oldest inferred by order) to prevent memory/storage attacks.
        * **Content Hash Integrity:** Every content unit has a `SHA-256(title + content)` hash. If the hash doesn't match the payload after sanitization, the unit is rejected.
    * **Reputation Merging (from CoffeeHouseInfo):** When the same content is seen from multiple peers, token sets are **unioned** (set merge). This produces a consensus count without double-counting. See `ReputationAgent.mergeReputation()` in CoffeeHouseInfo for the reference implementation.

---

## 7. The Persona Agent (Fictional Profile System)

Profiles are **optional**, **fictional**, and **never represent real identity**.

* **Role:** Manage the user's optional fictional persona for social interactions.
* **Responsibilities:**
    * **Optional Profile:** The app works fully without a profile. Anonymous users can browse and rate public content. A profile is only needed for social features (acquaintances, trusted users, private messages).
    * **Fictional Identity:** The profile consists of:
        * Display alias (any name — encouraged to be fictional).
        * Avatar (generated or selected from a built-in set — no camera/gallery access for avatar).
        * Short bio (max 200 characters).
        * A locally-generated cryptographic identity keypair (Ed25519) for signing and verification.
    * **No Personal Data:** The app never asks for real name, email, phone number, location, birthday, gender, or any other personal identifier. The onboarding screen explicitly states: *"Your profile is a character you create. It does not represent you. Do not use your real name or personal information."*
    * **Profile Visibility:** Profile data is only shared with connected peers when:
        * The Sharing Toggle is ON, AND
        * The user is in the Social section of the app, AND
        * The user has explicitly enabled "Show my profile to nearby users."
    * **Cryptographic Identity:** The Ed25519 keypair is generated on first profile creation and stored in the Android Keystore. The public key serves as the stable identity for the acquaintance and trusted user systems. The private key never leaves the device.

---

## 8. The Social Discovery Agent (Nearby Profiles & Acquaintances)

The social section where users can see fictional profiles of nearby devices.

* **Role:** Display nearby user profiles and manage the acquaintance system.
* **Responsibilities:**
    * **Nearby Profiles View:** When the Sharing Toggle is ON and the user navigates to the Social section, display a list of nearby profiles (alias + avatar only — no full profile until tapped).
    * **Acquaintance System:** A user can add a nearby profile as an **Acquaintance** — a lightweight, unverified social connection.
        * Adding an acquaintance stores the other user's public key and alias locally.
        * Acquaintances can see each other's public content with an "Acquaintance" badge.
        * Acquaintance content can be filtered in the browse section.
        * Becoming an acquaintance does NOT grant access to trusted-only content or private messages.
    * **Anonymous Users:** Users without a profile can browse the Social section (see nearby profiles) but cannot be added as acquaintances or interact socially.

---

## 9. The Trust Verification Agent (Briar-Style Trust Verification)

Trusted Users can be verified **in person** (highest security) or **remotely via link exchange** (convenient but requires caution) — inspired by Briar's dual approach.

* **Role:** Manage the process of upgrading an acquaintance to a Trusted User through cryptographic verification.
* **Responsibilities:**

    ### Method A: In-Person QR Code Exchange (Highest Security)
    * **Physical Presence Required:** Two users must be in the same physical location.
    * **Mutual QR Code Exchange (Briar Model):**
        1. User A opens the "Add Trusted User" screen → the app displays a QR code containing User A's public key fingerprint + a one-time challenge nonce.
        2. User A scans User B's QR code with the in-app camera.
        3. User B simultaneously scans User A's QR code.
        4. Both devices verify the mutual exchange: each device confirms it received the correct public key from the other and that the challenge nonces match.
        5. Upon mutual verification, both devices store each other's public key as a Trusted User.
    * **Security Level:** ✅ **Maximum.** Physical co-presence guarantees no man-in-the-middle attack is possible.

    ### Method B: Remote Link Exchange (Briar-Style "Add Contact at a Distance")
    * **How It Works (Bramble Rendezvous Protocol):**
        1. User A taps "Add Trusted User → Remote" and the app generates a unique `ClearSpace://` link. This link contains User A's **public key fingerprint** and a **one-time rendezvous token**.
        2. User A sends this link to User B through **any external channel** (e.g., Signal, email, SMS, a handwritten note — the app itself does not send it).
        3. User B receives the link, opens it in Clear Space, and the app generates User B's own `ClearSpace://` link in return.
        4. User B sends their link back to User A through the same or another external channel.
        5. Both devices now have each other's public key fingerprints. Each device connects to the other via **Tor hidden services** (using the rendezvous tokens) to complete the **Bramble Handshake Protocol** — deriving a shared secret and establishing the trust relationship.
        6. Upon successful handshake, both devices store each other's public key as a Trusted User.
    * **Security Level:** ⚠️ **Moderate.** This method is vulnerable to a **man-in-the-middle (MITM) attack** if the external channel used to exchange links is compromised. The app must display a clear, plain-language warning before every remote exchange:
        > *"You are about to add a trusted contact remotely. This is less secure than meeting in person. If the link was intercepted or tampered with by a third party, an attacker could read your future messages. Only use this method if you trust the channel you are using to share the link (e.g., an end-to-end encrypted messenger like Signal). The developer is not responsible for any consequences."*
    * **MITM Mitigation — Safety Number Verification:** After the remote exchange completes, both users are shown a **Safety Number** (a short numeric code derived from both public keys, identical to Signal's Safety Numbers). The app prompts: *"To make sure nobody intercepted your link, compare this number with your contact via a phone call or video chat. Do the numbers match?"* If the user confirms, the trust is marked as **Verified**. If skipped, the trust is marked as **Unverified (Remote)** with a persistent yellow warning badge.
    * **Tor Rendezvous:** The actual handshake connection is made over Tor, so neither device's IP address is exposed to the other or to any observer.

    ### Trust Properties (Both Methods)
    * Trusted Users can exchange **Trusted-Only** content.
    * Trusted Users can send **Private Messages** (see Messenger Agent).
    * Trust is bilateral — both parties must verify each other.
    * Trust can be revoked unilaterally at any time (the revoking user deletes the trusted key; the other user will see the trust as "broken" on next contact).
    * **Trust is Non-Transferable:** Being trusted by User A does not automatically make you trusted by User A's other trusted contacts.
    * **Trust Display:** The UI always shows the verification method used:
        * 🟢 **Verified (In-Person)** — QR code exchange. Highest confidence.
        * 🟡 **Verified (Remote)** — Link exchange + Safety Number confirmed. Moderate confidence.
        * 🟠 **Unverified (Remote)** — Link exchange, Safety Number NOT confirmed. Low confidence — the user is warned that MITM is possible.

---

## 10. The Messenger Agent (Briar-Style Private Messaging)

End-to-end encrypted private messaging between Trusted Users — modeled after Briar's approach.

* **Role:** Enable absolutely private communication between mutually verified Trusted Users using every available transport.
* **Responsibilities:**
    * **End-to-End Encryption:** All messages are encrypted using the Signal Protocol (Double Ratchet Algorithm with X3DH key exchange), adapted for the decentralized context. Keys are derived from the Ed25519 identity keypairs established during trust verification.
    * **Multi-Transport Delivery (Briar Model):** Messages are delivered through whatever path is available, in order of preference:
        1. **Direct — Wi-Fi / Bluetooth / NSD:** If both users are on the same local network or in Bluetooth range, messages are delivered directly via the existing P2P connection.
        2. **Relay via Tor:** If direct connection is unavailable but internet is available, each device runs a Tor hidden service. Messages are routed through Tor onion services, ensuring that neither the sender's nor receiver's IP address is exposed, and no observer can determine who is communicating with whom.
        3. **Store-and-Forward via Mutual Contacts:** If neither direct nor Tor is available, messages can be relayed through other Trusted Users who are in contact with both parties. The relaying device cannot read the message. To prevent storage bloat on relay devices, all relayed messages have a strict **Time-To-Live (TTL) of 48 hours**, after which they are automatically deleted from the relay's storage if undelivered.
        4. **Offline Queue:** If no transport is available, messages are queued locally and delivered when a connection becomes available.
    * **Private Video/Audio Calls (Jitsi Meet Style):** Initiates 1-on-1 encrypted video/audio calls using WebRTC (Apache 2.0). The WebRTC signaling occurs securely over the existing Briar/Tor P2P transport. Once signaling is complete, the WebRTC data channel handles the video stream peer-to-peer.
    * **Message Privacy Guarantees:**
        * Messages are encrypted end-to-end. No intermediary (including relay nodes) can read them.
        * Messages are stored encrypted on-device in the Private Library database.
        * Messages are never included in any content index or broadcast.
        * No message metadata (timestamps, sender/receiver aliases) is transmitted in the clear.
        * Forward secrecy: Compromising a key does not reveal past messages.
    * **Tor Integration:**
        * Bundle the Tor client library (e.g., `tor-android` from Guardian Project).
        * Each device generates a unique `.onion` address for receiving messages.
        * The `.onion` address is exchanged during trust verification (embedded in the QR code payload).
        * Tor is used ONLY for messaging between trusted users — never for public content sharing or discovery.

---

## 11. The Security & Consent Guard (OWASP Compliance)

The critical layer between the network and local storage, extended with comprehensive OWASP compliance.

* **Role:** Protect the user from malicious data, enforce informed consent, and prevent all OWASP Top 10 vulnerabilities.
* **Responsibilities:**

### Consent & Disclosure
* **Risk Disclosure:** Before any sharing activity, display mandatory warnings in plain language about the nature of P2P sharing.
* **Manual Import Trigger:** No data is saved to local storage unless the user explicitly selects an item and confirms "Save to Library."
* **Data Consent:** Ask for explicit consent to send and receive data separately. The user can choose to only receive (browse mode) or both send and receive.

### Content Sanitization
* Strip all HTML/script tags from incoming content (as in CoffeeHouseInfo's `SecurityGuard` and `SecurityProvider.sanitize()`).
* Truncate all string fields to defined maximum lengths (title: 200 chars, content: 50,000 chars, author: 50 chars).
* Re-encode all incoming images to WebP to strip EXIF/GPS metadata (from CoffeeHouseInfo's `SecurityProvider.scrubImageMetadata()`).
* Validate and reject malformed JSON payloads.

### OWASP Top 10 Web (2017, 2021, 2025) Compliance
*Since Clear Space has no server, many web OWASP items don't apply directly, but the spirit of each is addressed:*

| OWASP Risk | Mitigation |
|:---|:---|
| **A01: Broken Access Control** | Three-tier storage with cryptographic enforcement. Private data is in a separate encrypted DB. Trust levels enforced by key verification. |
| **A02: Cryptographic Failures** | AES-256-GCM for data at rest (hardware-backed keys). TLS 1.3 for data in transit. Signal Protocol for messages. No weak ciphers. |
| **A03: Injection** | All incoming text is sanitized (HTML stripped, truncated, escaped). No SQL built from user input (Room parameterized queries). No eval/exec of incoming data. |
| **A04: Insecure Design** | Privacy by design. Threat modeling documented. Defense in depth (encryption + isolation + consent). |
| **A05: Security Misconfiguration** | `allowBackup=false`, no exported components unless required, strict `android:exported` declarations, ProGuard/R8 obfuscation. |
| **A06: Vulnerable Components** | Dependency scanning in CI. Minimal third-party libraries. Pin library versions. Audit Tor and crypto dependencies. |
| **A07: Auth Failures** | Mandatory password on every launch. Brute-force lockout. Hardware-backed key storage. No "remember me." |
| **A08: Data Integrity Failures** | SHA-256 content hashing. PoW for anti-spam. Signature verification for trusted user content. Reject units with mismatched hashes. |
| **A09: Logging & Monitoring** | No sensitive data in logs. Structured local audit log for security events (failed auth, rejected content, trust changes). Logs encrypted. |
| **A10: SSRF** | Minimal server usage limits SSRF risk. Outbound HTTP requests are restricted to Firebase (wake-ups/moderation) and Tor (trusted messaging). |

### OWASP Mobile Top 10 (2024) Compliance

| Mobile Risk | Mitigation |
|:---|:---|
| **M1: Improper Credential Usage** | No hardcoded secrets. Voter secret in Keystore. Password hash via Argon2id. |
| **M2: Inadequate Supply Chain Security** | Pinned dependency versions. Hash verification for Tor binary. Minimal dependencies. |
| **M3: Insecure Auth/AuthZ** | Password + Keystore on every launch. No session tokens. No server-side auth to bypass. |
| **M4: Insufficient Input Validation** | All inputs sanitized, truncated, type-checked. Regex stripping of HTML/script. JSON schema validation for P2P payloads. |
| **M5: Insecure Communication** | TLS 1.3 for local P2P. Tor for remote messaging. Certificate pinning for TLS sessions. No plaintext transmission. |
| **M6: Inadequate Privacy Controls** | Zero PII collection. Fictional profiles. Anonymous voting tokens. EXIF stripping. No analytics/tracking. |
| **M7: Insufficient Binary Protections** | R8/ProGuard obfuscation. Root detection with warnings. Emulator blocking. Tamper detection via APK signature verification. |
| **M8: Security Misconfiguration** | `allowBackup=false`. No world-readable files. Strict exported component declarations. |
| **M9: Insecure Data Storage** | SQLCipher encrypted database. Hardware-backed encryption keys. Private files only. No data in SharedPreferences (except encrypted). |
| **M10: Insufficient Cryptography** | AES-256-GCM (not AES-ECB). SHA-256 for hashing (not MD5/SHA1). Ed25519 for signatures. Proper IV/nonce generation via `SecureRandom`. |

---

## 12. The Data Export/Import Agent (Device Transfer)

Enables the user to transfer all their app data to another device via an encrypted export file.

* **Role:** Create and consume encrypted data bundles for device migration.
* **Responsibilities:**
    * **Export Flow:**
        1. User selects "Export All Data" from settings.
        2. App prompts: *"You are about to create an encrypted backup of all your app data (content, profiles, trusted users, messages, settings). This file will be protected with a password you choose. If you forget this password, the data cannot be recovered. The developer cannot help you recover it."*
        3. User creates a strong password (minimum 12 characters, must contain uppercase, lowercase, number, and symbol — enforced).
        4. App encrypts all data using AES-256-GCM with a key derived from the password via Argon2id (with high memory/iteration parameters).
        5. The encrypted file is saved with a custom extension (`.aethex`) to the user's chosen location.
    * **Import Flow:**
        1. User selects "Import Data" from settings.
        2. User selects the `.aethex` file.
        3. User enters the export password.
        4. App decrypts and validates the data. If the password is wrong, display: *"Incorrect password. The file cannot be decrypted."*
        5. If valid, user chooses: **Replace** (overwrite all current data) or **Merge** (combine with existing data, deduplicating by content hash).
    * **Export File Format:** The `.aethex` file contains:
        * File format version header (for forward compatibility).
        * Argon2id salt + parameters.
        * AES-256-GCM encrypted blob containing: all databases, keystore-exportable keys, profile data, trusted user list, message history, settings.
        * HMAC-SHA256 integrity tag over the entire file.
    * **Security:** The export file is completely inaccessible without the user's chosen password. There is no recovery mechanism. There is no backdoor.

---

## 13. The Host Protection Agent (Device Security)

Prevents the app from being used as an attack vector against the host device.

* **Role:** Ensure that P2P connectivity does not expose the host phone to exploitation.
* **Responsibilities:**
    * **Sandboxed Networking:** All network operations are strictly scoped to the app's service types. No raw socket access beyond the defined P2P protocol. No port scanning. No arbitrary connection attempts.
    * **Connection Rate Limiting (from CoffeeHouseInfo):** Maximum 5 concurrent connections. New connections are dropped if the limit is reached.
    * **Payload Size Limits:** Maximum incoming payload: 10 MB. Any payload exceeding this is rejected without processing.
    * **Socket Timeouts:** All sockets have a 5-second read timeout and 10-second connection timeout.
    * **No Dynamic Code Loading:** The app never loads or executes code received from the network. All executable logic is bundled in the APK.
    * **Intent Security:** All app components have `android:exported="false"` unless explicitly required. No implicit intent receivers for sensitive operations.
    * **Strict TLS:** TLS 1.3 minimum for all P2P connections. No fallback to TLS 1.1/1.0. No trust-all certificate managers in production (improve upon CoffeeHouseInfo's testing-only `trustAllCerts`). Use self-signed certificates with pin verification against known peer public keys.
    * **Memory Safety:** Clear sensitive data (passwords, decrypted content, keys) from memory immediately after use. Use `ByteArray.fill(0)` patterns.

---

## 14. The Content Renderer Agent (Safe Display)

Safely renders content received from untrusted peers.

* **Role:** Display content without introducing XSS, injection, or rendering vulnerabilities.
* **Responsibilities:**
    * **App Isolation (Shelter Style):** To protect the host application from zero-day parsing vulnerabilities, the Renderer runs in a separate Android `android:isolatedProcess` sandbox.
    * **Text-Only Rendering:** Content is rendered as plain text or sanitized Markdown. No HTML rendering. No WebView for user content.
    * **Image Rendering:** Images are decoded using `BitmapFactory` with `inJustDecodeBounds` pre-check to prevent memory bombs. Maximum image dimensions: 4096x4096. All images re-encoded to WebP on import (strips metadata).
    * **No External URLs:** Content must not contain clickable external URLs. Any URL-like strings in content are rendered as plain text, not hyperlinks.
    * **Content Warnings:** If content is flagged with certain emotion tags (e.g., `Angry`, `Sad`, `Scared`, `Disgusted`) by a significant number of voters, show a soft content warning: *"Multiple users have flagged this content as potentially upsetting. View anyway?"*

---

## 15. The Sync Agent (Syncthing-Style Background Sync)

Synchronizes the Private Library across multiple physical devices owned by the same user.

* **Role:** Keep passwords, offline notes, and private content identical across phones/tablets/desktops.
* **Responsibilities:**
    * **P2P Sync:** Uses the Syncthing Block Exchange Protocol (BEP). Devices connect directly via local network or Tor.
    * **Automated Background Task:** Syncs automatically when devices see each other, ensuring the Password Vault and Obsidian-style notes are always up to date without ever touching Google Drive or Dropbox.

---

## 16. The Proxy Agent (Tor Routing & Anonymity)

Provides network anonymity and anonymized external routing.

* **Role:** Prevent IP leakage and allow safe external access.
* **Responsibilities:**
    * **Tor Daemon Integration:** Embeds the Orbot Tor backend.
    * **Tor Proxy Mode (VPN Alternative):** Routes all app traffic through the Onion network, mimicking the privacy of commercial VPNs (like Mullvad/Proton) but via decentralized relays.
    * **Anonymized Link Routing (LibreTube Style):** If a user clicks a YouTube/external link in a post, the Proxy Agent intercepts it, rewrites it to a random Invidious/Piped instance, and routes the request exclusively over Tor. This allows media consumption without Google tracking.

---

## 17. The Utility Agent (Fossify Local Tools)

Provides offline, ad-free basic utilities replacing the need for external Google/Samsung apps.

* **Role:** Manage internal utilities (Calendar, Gallery, File Manager, Tasks) using the encrypted vault.
* **Responsibilities:**
    * **Fossify Suite Integration:** 
        * **Calendar:** An offline event planner stored inside the encrypted Room database.
        * **Gallery:** A secure image viewer for media inside the Private Vault, preventing export to tracked Android gallery apps.
        * **File Manager:** A simple interface to browse, import, and export files to/from the encrypted Clear Space database.
    * **Habits & Tasks:** A Loop Habit Tracker / Tasks.org style checklist module that syncs automatically across devices via the Sync Agent (BEP).

---

## 18. The Media Agent (Offline Player & Feeds)

Handles all media consumption and news aggregation privately.

* **Role:** Play encrypted media and fetch external RSS feeds securely.
* **Responsibilities:**
    * **Internal Media Player:** A lightweight mpv/ExoPlayer integration that streams media directly from the encrypted database without writing to unencrypted storage.
    * **RSS Reader (Feeder/AntennaPod style):** Fetches external news and podcast RSS feeds exclusively over Tor. Integrates these seamlessly alongside internal P2P social feeds.

---

## Agent Interaction Workflows

### Workflow A: Anonymous Content Browsing
```
1. [Gatekeeper] → User authenticates with password.
2. [Sharing Toggle] → User enables sharing (consent dialog + risk disclosure).
3. [Discovery Agent] → Finds nearby peers via BLE / Bluetooth Classic / Wi-Fi Direct / Wi-Fi Aware / NSD.
4. [Content Indexer] → Receives lightweight content headers from peers.
5. [Content Indexer] → User filters by category flags, emotion flags, AND/OR toggle.
6. [Reputation Agent] → Content sorted by reputation score (likes - dislikes).
7. [Security Guard] → User selects item → "Save to Library?" consent dialog.
8. [Content Vault] → Full content transferred, sanitized, hash-verified, and stored locally.
9. [Reputation Agent] → User votes (Like/Dislike) and/or applies flags → anonymous token generated.
```

### Workflow B: Social Interaction & Trust Building
```
1. [Persona Agent] → User creates fictional profile with alias + avatar.
2. [Social Discovery] → User browses nearby profiles in the Social section.
3. [Social Discovery] → User adds a nearby profile as Acquaintance (stores public key + alias).
4. [Trust Verification] → Users meet in person → mutual QR code scan → Trusted User established.
   OR
4b. [Trust Verification] → Users exchange ClearSpace:// links remotely → Tor handshake → Safety Number verification → Trusted User established.
5. [Content Vault] → Trusted-only content becomes visible between trusted users.
6. [Messenger Agent] → Private messages can now be exchanged via direct / Tor / relay.
```

### Workflow C: Data Migration
```
1. [Data Export/Import] → User selects "Export All Data."
2. [Data Export/Import] → User creates strong password → encrypted .aethex file generated.
3. [Data Export/Import] → On new device: import .aethex file → enter password → data restored.
```

---

## Technical Build Constraints

* **Language:** Kotlin 2.1+ (K2 Compiler).
* **UI:** Jetpack Compose with Material 3 and mandatory Edge-to-Edge support.
* **Min SDK:** 26 (Android 8.0) — required for proper Keystore, SQLCipher, BLE, and Wi-Fi Direct support.
* **Target SDK:** 36 (Android 16 "Baklava").
* **Database:** Room + SQLCipher. DataStore for non-sensitive preferences only.
* **Networking:** Android framework P2P APIs (BLE, Bluetooth Classic, Wi-Fi Direct, Wi-Fi Aware, NSD/mDNS) + TLS Sockets, Firebase/Google Play Services (for FCM wake-ups and moderation), Tor (messaging + remote trust verification), Syncthing BEP, and WebRTC.
* **Cryptography:** Android Keystore (StrongBox preferred), AES-256-GCM, SHA-256, Ed25519, Argon2id, Signal Protocol (Double Ratchet + X3DH).
* **Build System:** Gradle 9.x+ / AGP 9.x+.
* **Source Standard:** All agent logic must reside within `src/main/kotlin` with strict package separation per agent.
* **Obfuscation:** R8 (ProGuard replacement) enabled for release builds with full obfuscation and optimization.
* **Hybrid Dependency Whitelist (Google Play Compliance):** To ensure maximum privacy while supporting necessary moderation, ONLY the following third-party dependencies may be pulled during development. Adding any unlisted dependency requires explicit architectural review:
    1. **AndroidX / Jetpack Compose** (Apache-2.0) — UI and core framework.
    2. **Room** (Apache-2.0) — Database ORM.
    3. **SQLCipher for Android** (Zetetic Community Edition, BSD) — Encrypted SQLite.
    4. **Signal Protocol** (e.g., `signal-protocol-java` GPLv3 or a modern FOSS Kotlin port) — Messaging E2EE.
    5. **Tor-Android** (Guardian Project, Apache-2.0/BSD) — Onion routing.
    6. **BouncyCastle** (`bcprov-jdk18on`, MIT/Apache-2.0) — Supplemental cryptography only. Do **NOT** use Google Tink.
    7. **Firebase & Google Play Services** — Exclusively for FCM wake-ups and moderation.
    8. **Kotlinx Coroutines & Serialization** (Apache-2.0).
    9. **WebRTC Android SDK** (Apache-2.0) - For video calls.
    10. **Syncthing-Android (Core)** (MPLv2.0) - For file synchronization.
    
    *The following open-source projects are being logically incorporated or heavily referenced for Phase 3 utilities. All integrated code MUST strictly comply with their respective copyleft licenses:*
    10. **Aegis Authenticator** (GPLv3) - For 2FA/OTP token generation logic.
    11. **Fossify Suite (Gallery, Calendar, Files, Contacts, Notes)** (GPLv3) - For local-only utility modules.
    12. **Loop Habit Tracker** (GPLv3) - For the Habits tracking module.
    13. **LocalSend** (MIT / GPLv3) - For mDNS local file discovery logic.
    14. **NewPipe Extractor / ExoPlayer** (GPLv3 / Apache-2.0) - For the internal media player and video URL parsing.
    15. **AntennaPod / Feeder** (GPLv3) - For the Tor-based RSS/Podcast feed parser.
    16. **HeliBoard / FlorisBoard** (Apache-2.0) - For the custom, tracking-free in-app keypad.
    17. **Scrambled Exif / ImagePipe** (GPLv3) - For the image metadata stripping logic.
* **Permissions Required:**
    * `FOREGROUND_SERVICE` — Required to run the Discovery Agent as a foreground service when sharing is enabled.
    * `INTERNET` — Required for local P2P, Tor, Firebase Cloud Messaging, and moderation checks.
    * `NEARBY_WIFI_DEVICES` — Required for Wi-Fi Aware and Wi-Fi Direct peer discovery on Android 13+.
    * `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (with `android:usesPermissionFlags="neverForLocation"` — this flag is **mandatory** to avoid requiring `ACCESS_FINE_LOCATION`), `BLUETOOTH_ADVERTISE` — For BLE/Bluetooth P2P.
    * `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE` — For NSD fallback.
    * `CAMERA` — For QR code scanning during trust verification only.
    * `USE_BIOMETRIC` — For biometric authentication option.
    * **Manifest Note:** The app's `AndroidManifest.xml` must declare the `<service android:name=".discovery.DiscoveryForegroundService" android:foregroundServiceType="connectedDevice" />` and request the `FOREGROUND_SERVICE` permission so the service can continue when the app is backgrounded.
* **Permissions NOT Used:** `READ_CONTACTS`, `READ_PHONE_STATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `READ_EXTERNAL_STORAGE` (scoped storage only), `RECORD_AUDIO`, `READ_CALL_LOG`, or any permission that could leak personal data.

---

## Application Sections (Screen Architecture)

### Section 1: Browse Content (Public)
* Anonymous access (no profile required).
* View content indexes received from connected peers.
* Filter by category flags, emotion flags (AND/OR toggle).
* Filter by source: All / Acquaintances / Trusted Users.
* Sort by reputation / popularity / date / alphabetical.
* Like, dislike, and flag content anonymously.
* Save content to Private Library.

### Section 2: Social (Profile Required)
* View nearby fictional profiles.
* Add acquaintances.
* Initiate Trusted User verification (QR scan).
* View trusted user list.
* Private messaging with trusted users.

### Section 3: My Library (Private)
* View all saved content (private + trusted + public).
* Create new content (choose visibility: Private / Trusted / Public).
* Export content to device storage.
* Manage content visibility.

### Section 4: Settings
* Change password.
* Manage profile (create / edit / delete).
* Export / Import app data.
* View security log.
* Revoke trusted users.
* Adjust PoW difficulty.
* Clear cached peer data.
* View app disclaimer and privacy policy.

---

> **Disclaimer:** This application is provided "as is" without warranty of any kind. The developer is not responsible for any damage, data loss, privacy breach, legal consequence, emotional distress, or any other outcome resulting from the use of this application. By using Clear Space, you acknowledge that you are doing so **entirely at your own risk.** The app does not ask for, store, or transmit your personal data. Profiles are fictional and do not represent real identities. The developer has no ability to access, recover, or control any data within the application.


