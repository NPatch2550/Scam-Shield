# 🛡️ Scam Shield (Android Edition)
### Anti-Investment Scam & Corporate Authenticity Verification System for First Jobbers

> **Hackathon Submission Package & Evaluation Guide**

---

## 🎯 Executive Summary & Problem Statement

First Jobbers and Young Professionals in Southeast Asia and globally are facing an unprecedented epidemic of **financial investment fraud**:
1. **Manipulated Search & AI Rankings (SEO & AEO Poisoning):** Scammers pay for sponsored Google ads, flood press release wires with fake PR articles, and manipulate AI answer engines. When young investors search *"Is [Scam Broker] legit?"*, the top results falsely claim they are verified.
2. **"Pig-Butchering" & Fake VIP Stock Groups:** Scammers lure victims from dating apps and social media into private LINE OpenChat and Telegram groups promising 40%+ monthly returns with "zero risk", before extorting "unlock fees" or disappearing.
3. **Mule Account Exploitation (*บัญชีม้า*):** Victims are instructed to transfer their hard-earned salaries into personal bank accounts rather than licensed corporate custodian accounts.
4. **Aggressive Cold Calls:** Fake financial advisors and securities brokers cold-call young professionals with high-pressure insider stock tips.

**Scam Shield** is a real-time, active defense Android application backed by a Node.js/Express backend that provides continuous protection across **Phone Calls**, **Messaging Apps (LINE/Telegram)**, and **Mobile Banking**.

---

## 🏗️ System Architecture & Defense Engines

```
                           ┌────────────────────────────────────────────────────────┐
                           │          SCAM SHIELD ANDROID ACTIVE CLIENT             │
                           └────────────────────────────────────────────────────────┘
                                     │                        │                  │
                1. CALL DEFENSE      │   2. CHAT & SCREEN     │  3. BANKING      │  4. DIRECT
                   CallScreening     │      DEFENSE           │     BRAKE        │     SEARCH
                   3-Way Merging     │   SYSTEM_ALERT_WINDOW  │  Transfer Brake  │  Trust Scorecard
                   SIP Cloud PBX     │   AccessibilityService │  Overlay Modal   │  SEC / DBD Check
                                     │                        │                  │
                                     ▼                        ▼                  ▼
                           ┌────────────────────────────────────────────────────────┐
                           │               CLOUD / LOCALHOST GATEWAY                │
                           │  Local: 10.0.2.2:3000  |  Public: trycloudflare.com   │
                           └────────────────────────────────────────────────────────┘
                                                         │
                                                         ▼
                           ┌────────────────────────────────────────────────────────┐
                           │        NODE.JS / EXPRESS.JS BACKEND ENGINE             │
                           │   • Rule-Based Streaming NLP Threat Analyzer           │
                           │   • 3-Pillar Dynamic Risk Scorer (0–100%)              │
                           │   • Socket.io Bi-Directional Event Streamer            │
                           │   • Anti-SEO/AEO Corporate Authenticity Engine         │
                           │   • MongoDB Collections (Blacklist, Whitelist, Reports)│
                           └────────────────────────────────────────────────────────┘
```

### The 4 Real-Time Defense Modes:

1. **Call Defense Mode (Cloud PBX 3-Way Bridge & Diarization):**
   * Pre-call interception using Android's native `CallScreeningService`.
   * Live 3-way conference call bridging the User $\leftrightarrow$ Caller $\leftrightarrow$ AI Listener Bot.
   * Dual-channel streaming STT analyzing red flags every 3 seconds in volatile RAM (guaranteed returns, zero risk, APK downloads, withdrawal taxes).
   * 100% ephemeral processing: Audio RAM is wiped immediately upon call termination.

2. **Chat Defense Mode (Assistive Floating Bubble & Node Reading):**
   * Draggable assistive touch bubble (`SYSTEM_ALERT_WINDOW`) rendered continuously on top of any app (LINE, Telegram, Instagram, Facebook).
   * Fast Node Reading (`AccessibilityService`) captures text nodes from view hierarchies with near-zero battery drain.
   * Visual OCR fallback (`MediaProjection API` + Google ML Kit Text Recognition) for screenshot-based returns or QR codes.
   * Dynamic color shift (🟢 Green $\rightarrow$ 🟡 Yellow $\rightarrow$ 🟠 Orange $\rightarrow$ 🔴 Red 100% Critical) and expandable Company Trust Card overlay.

3. **Anti-SEO / Anti-AEO Corporate Authenticity Engine:**
   * **Regulatory Registry Cross-Check:** Matches against SEC Thailand, Bank of Thailand (BOT), and DBD Department of Business Development.
   * **Domain Footprint & Typosquat Analysis:** Flags domains younger than 6 months and detects lookalike clone sites via Levenshtein distance.
   * **SEO Astroturfing Detector:** Flags companies with sudden fake PR saturation but zero legitimate corporate registration history.
   * **Mule Account Detector:** Flags personal bank accounts or PromptPay numbers when solicited under a corporate brokerage name.

4. **Mobile Banking Transfer Brake & PII Guard:**
   * If a user switches from a flagged investment chat directly into a mobile banking app (K PLUS, SCB EASY, Krungthai NEXT), the high-priority red Transfer Brake modal covers the screen before "Confirm Transfer" can be tapped.
   * Instant audio chime and haptic vibration trigger if the user attempts to send sensitive credentials (13-digit National ID, OTP, seed phrases).

---

## 📊 Dynamic 3-Pillar Risk Score Breakdown

$$\text{Risk Score (0–100\%)} = \underbrace{\text{Regulatory Match (40\%)}}_{\text{SEC / BOT / Mule DB}} + \underbrace{\text{SEO / AEO Authenticity (30\%)}}_{\text{Domain age, Typosquats, PR}} + \underbrace{\text{Behavioral Intent (30\%)}}_{\text{Guaranteed ROI, FOMO, APK}}$$

---

## 🚀 How to Run & Evaluate the Submission

### Option 1: Native Android Studio Project
1. Open Android Studio $\rightarrow$ **File $\rightarrow$ Open** $\rightarrow$ select the `android/` directory in this package.
2. Select an emulator (e.g., Pixel 8 or any device running Android 8.0 to Android 15/16).
3. Click **Run ▶ (Shift + F10)**.
4. *(Note: Built with AGP 8.5.2, Gradle 8.7, JDK 17, and verified 16 KB page-size alignment compliant for Android 15+)*.

### Option 2: Pre-Compiled APK (Direct Install)
* Install [`app-debug.apk`](app-debug.apk) directly via ADB:
  ```powershell
  adb install -r app-debug.apk
  ```
* Or drag & drop `app-debug.apk` onto any running Android emulator.

### Option 3: Backend Server & Web Simulator (Zero-Install)
1. Start the backend: double-click `start-server.bat` (or run `node src/server.js`).
2. Start the public internet tunnel: double-click `start-tunnel.bat`.
3. Open in any browser:
   * **Interactive Phone Simulator:** `http://localhost:3000/admin/phone-simulator.html`
   * **Mobile Web App:** `http://localhost:3000/app`
   * **Admin Verification Portal:** `http://localhost:3000/admin` *(Admin Key: `scamshield-admin-2024`)*

---

## 🧪 Quick Test Scenarios for Evaluators

| Test Feature | How to Test in App | Expected Result |
|---|---|---|
| **Before You Invest** | In app, tap **Before You Invest** $\rightarrow$ tap chip `AlphaGold` | 🚨 Blacklisted fake broker alert with 3-pillar breakdown |
| **Mule Account Check** | In app, tap **Before You Invest** $\rightarrow$ tap chip `1234567890` | 🚨 Instant 100% Critical Mule Account (*บัญชีม้า*) alert |
| **Legit Verification** | In app, tap **Before You Invest** $\rightarrow$ tap chip `KasikornBank` | ✅ Verified whitelisted bank / broker (BOT registered) |
| **Chat Defense Scan** | In app, tap **Chat Defense** $\rightarrow$ tap **⚡ Scan Chat** | 🚨 Flags pig-butchering scripts + turns Floating Bubble RED |
| **Transfer Brake** | In app, tap **Chat Defense** $\rightarrow$ tap **🛑 Simulate Banking Switch** | 🛑 Red full-screen Transfer Brake modal covers screen |
| **PII Credential Guard** | In app, tap **Chat Defense** $\rightarrow$ type `1234567890123` | 🚨 Immediate audio warning chime and heavy vibration |
| **3-Way Call Defense** | In app, tap **Call Defense** $\rightarrow$ tap **🔀 Merge Calls** | 📞 3-Way audio stream flags high-pressure tactics in real-time |

---

## 📦 Package Contents
* `/android`: Complete Android native Kotlin project (Jetpack, Coroutines, Retrofit, ML Kit, Material 3).
* `/src`: Node.js, Express.js backend with rule-based NLP engine, REST APIs, and Socket.io.
* `/public`: Standalone mobile PWA, live phone simulator, and admin portal.
* `app-debug.apk`: Pre-compiled, signed debug APK (28.4 MB, 16 KB page-aligned).
* `start-server.bat`: 1-click backend launcher.
* `start-tunnel.bat`: 1-click Cloudflare public DNS tunnel launcher.
* `SCAM_SHIELD_ARCHITECTURE.md`: Complete in-depth technical specification.
