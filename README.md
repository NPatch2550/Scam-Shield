# 🛡️ Scam Shield (Android Edition)

> **Real-Time Active Defense & Anti-Investment Scam Verification System for First Jobbers**  
> *Developed for Hackathon Submission*

---

## 📌 Overview

**Scam Shield** is an active defense Android application backed by a Node.js/Express and MongoDB backend. Tailored specifically for First Jobbers and Young Professionals, it provides real-time protection against sophisticated financial fraud:
- **Fake Stock & Crypto Brokers** & Pig-Butchering Schemes (*หลอกให้รักแล้วชวนลงทุน*)
- **High-Yield Savings & Fake IPOs**
- **Manipulated SEO/AEO & Poisoned Search Rankings**
- **Mule Bank Accounts (*บัญชีม้า*) & Unlicensed Fund Solicitations**

---

## ⚡ Quick Evaluation Guide for Judges

You can evaluate Scam Shield in any of the following 3 ways:

### 1. Direct APK Install (Instant Test)
A pre-compiled, **16 KB page-size aligned** debug APK is included at the root of this submission package:
```bash
adb install -r ScamShield-v1.0-Debug.apk
```
Or simply drag and drop `ScamShield-v1.0-Debug.apk` into your running Android Emulator.

### 2. Android Studio (Full Native Kotlin Source)
1. Open Android Studio -> **File > Open** -> select the `android/` directory.
2. Ensure JDK 17+ is selected.
3. Build & Run on any Android 8.0+ emulator or physical device.

### 3. Backend & Interactive Web Simulator
1. Ensure Node.js (v18+) is installed.
2. Double-click `start-server.bat` (or run `npm install && npm start`).
3. (Optional) Run `start-tunnel.bat` for public internet access via Cloudflare tunnel.
4. Visit the web simulators in your browser:
   - **Phone Simulator:** `http://localhost:3000/admin/phone-simulator.html`
   - **Interactive Visualizer:** `http://localhost:3000/visualizer.html`
   - **Admin Fraud Center:** `http://localhost:3000/admin/`

For detailed evaluation steps, test scenarios, and architecture breakdowns, see **[HACKATHON_SUBMISSION.md](HACKATHON_SUBMISSION.md)**.

---

## 📱 Key Features

| Feature | Android Technology | Description |
|---|---|---|
| **Floating Assistive Bubble** | `SYSTEM_ALERT_WINDOW` | Draggable UI overlay showing real-time risk color (Green -> Yellow -> Red) on top of LINE, Telegram, browsers. |
| **Chat Defense** | `AccessibilityService` | Scans text nodes in real time with near-zero battery drain; detects scam scripts & PII leaks. |
| **Screen OCR Fallback** | `MediaProjection API` + ML Kit | Optical character recognition on screenshots and fake portfolio dashboards. |
| **Call Defense** | `CallScreeningService` + 3-Way PBX | Intercepts scam numbers before ringing and supports 3-way call conference with AI bot. |
| **Transfer Brake** | `AccessibilityService` Overlay | High-priority overlay intercepting users before confirming transfers to mule accounts. |
| **Anti-SEO/AEO Engine** | Backend Microservice | Cross-checks SEC/DBD registries, detects domain typosquatting, and flags astroturfed fake PR. |

---

## 📂 Project Directory Structure

```text
├── ScamShield-v1.0-Debug.apk    # Pre-compiled, 16KB-aligned APK ready to install
├── README.md                     # This file
├── HACKATHON_SUBMISSION.md       # Comprehensive evaluation and architecture guide
├── EMULATOR_SETUP_GUIDE.md       # Step-by-step setup guide for Android Studio / AVD
├── start-server.bat              # 1-Click launcher for backend server
├── start-tunnel.bat              # 1-Click launcher for public Cloudflare tunnel
├── package.json                  # Backend dependencies and scripts
│
├── android/                      # Complete Native Android Studio Kotlin Project
│   ├── app/
│   │   ├── src/main/java/com/scamshield/app/
│   │   │   ├── ui/               # MainActivity, ChatDefenseActivity, DirectSearchActivity, etc.
│   │   │   ├── services/         # FloatingBubbleService, ChatDefenseAccessibilityService, etc.
│   │   │   ├── api/              # Retrofit API clients & models
│   │   │   └── utils/            # ScamDetector, Preferences
│   │   ├── src/main/res/         # Layouts, drawables, strings, accessibility configs
│   │   └── build.gradle.kts      # Android build config with 16KB page alignment support
│   ├── gradle/wrapper/           # Gradle Wrapper 8.7
│   └── build.gradle.kts          # Root build config (AGP 8.5.2)
│
├── src/                          # Node.js / Express Backend Engine
│   ├── models/                   # Mongoose Schemas (ScamEntity, LegitCompany, Report, CallSession)
│   ├── routes/                   # Verify, Call, Report, Admin, Dashboard endpoints
│   ├── services/                 # nlpEngine, entityExtractor, riskScorer, domainAnalyzer
│   ├── socket/                   # Socket.io real-time event streaming
│   └── server.js                 # HTTP + WebSocket entry point
│
├── public/                       # Web portal, Admin dashboard & interactive simulators
│   ├── admin/                    # Admin portal & Phone Simulator
│   ├── app/                      # Hosted APK download page
│   └── visualizer.html           # System architecture visualizer
│
└── bin/                          # Cloudflared binary for public tunneling
```

---

## 🏆 Hackathon Highlights
- **100% Privacy-First:** On-device accessibility node parsing; raw audio/screen never permanently stored.
- **Works Across Any App:** True native floating bubble protects users in LINE OpenChat, Telegram, Facebook, and mobile banking.
- **Tested & Validated:** Includes built-in simulated scam chats, mule account verification, and interactive phone testbed.
