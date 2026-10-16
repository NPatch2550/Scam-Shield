# 📱 Scam Shield – Android Emulator User & Setup Guide

This guide explains how to run, use, and test **Scam Shield** in **any Android emulator** running on your laptop.

---

## ⚡ 1-Minute Quick Start: How to Open in Your Emulator

The Scam Shield server is **already running** on `0.0.0.0:3000` on your laptop.

### 0. ☁️ Public Cloudflare DNS URL (Accessible by Anyone Worldwide)
You can share this public HTTPS URL with anyone on any phone or laptop:
* **Public Mobile App:** [https://royalty-deutsch-fantasy-consult.trycloudflare.com/app](https://royalty-deutsch-fantasy-consult.trycloudflare.com/app)
* **Public Phone Simulator:** [https://royalty-deutsch-fantasy-consult.trycloudflare.com/admin/phone-simulator.html](https://royalty-deutsch-fantasy-consult.trycloudflare.com/admin/phone-simulator.html)
* **Public Admin Portal:** [https://royalty-deutsch-fantasy-consult.trycloudflare.com/admin](https://royalty-deutsch-fantasy-consult.trycloudflare.com/admin)
*(To restart the tunnel at any time, double-click `start-tunnel.bat` in the project folder).*

---

### 1. In Official Android Studio AVD (Android Virtual Device)
1. Launch your Android Virtual Device emulator in Android Studio.
2. Open the **Google Chrome** app inside the emulator.
3. In the address bar, type:
   ```
   http://10.0.2.2:3000/app
   ```
   *(Or open the public HTTPS link above).*
4. Tap the three dots menu (**⋮**) in Chrome $\rightarrow$ tap **"Install App"** (or **"Add to Home screen"**).
5. The **Scam Shield 🛡️** app icon is now installed directly onto your emulator's home screen and app drawer!

---

### 2. In Third-Party Android Emulators (BlueStacks, LDPlayer, Nox, MuMu)
1. Open the browser inside BlueStacks, LDPlayer, or Nox.
2. Type your laptop's LAN IP or public DNS:
   ```
   https://royalty-deutsch-fantasy-consult.trycloudflare.com/app
   ```
3. Tap **"Install App"** / **"Add to Home screen"** to install the standalone application.

---

### 3. On Host PC (Zero-Install Testing)
If you don't have an emulator open right now, you can open the **Android Device Simulator** directly in your PC browser:
* **Interactive Android Simulator:** [http://localhost:3000/admin/phone-simulator.html](http://localhost:3000/admin/phone-simulator.html)
* **Mobile App View:** [http://localhost:3000/app](http://localhost:3000/app)
* **Admin Portal:** [http://localhost:3000/admin](http://localhost:3000/admin)

---

## 🎮 How to Test All Features in the Emulator

### 1. Floating Assistive Bubble (`SYSTEM_ALERT_WINDOW`)
* The green shield bubble 🟢 floats over the screen.
* Drag it anywhere with touch/mouse.
* Tap the bubble to expand the **Company Trust Card & Warning Overlay** showing the 3-pillar breakdown (Regulatory 40pts, SEO 30pts, Behavioral 30pts).
* As threats are detected, the bubble shifts color in real time:
  * 🟢 **SAFE (0%)**
  * 🟡 **CAUTION (15–39%)**
  * 🟠 **SUSPICIOUS (40–69%)**
  * 🔴 **CRITICAL (70–100%)**

### 2. "Before You Invest" Direct Search Portal
* Tap **Verify** (or 🔍 in the bottom navigation).
* Enter any entity or tap quick chips:
  * `AlphaGold Securities` $\rightarrow$ Flagged as **SUSPICIOUS / BLACKLISTED** (unlicensed broker cold-calling first jobbers).
  * `cryptovaultpro.io` $\rightarrow$ Flagged as **CRITICAL** (unlicensed crypto exchange with withdrawal unlock fees).
  * `1234567890` $\rightarrow$ Flagged as **CRITICAL 100% (Mule Bank Account / บัญชีม้า)**.
  * `KasikornBank` $\rightarrow$ Verified **WHITELISTED REGULATORY ENTITY** (BOT licensed).

### 3. Call Defense Mode & 3-Way Cloud PBX Merging
* Tap **Call** in the bottom navigation.
* Tap **"Simulate Call"**:
  1. An incoming call from `089-123-4567` triggers the **CallScreeningService** alert.
  2. Tap **"Start 3-Way Audio Stream"**: Bridges User $\leftrightarrow$ Scammer $\leftrightarrow$ AI Listener Bot.
  3. Real-time dual-channel audio streams into the NLP Threat Engine.
  4. Red flags (guaranteed return, zero-risk, APK download link) update the floating bubble to **RED 100% Critical** in real time!
  5. Audio is held only in volatile RAM and permanently wiped on disconnect.

### 4. Chat Defense Mode (LINE / Telegram Scanner)
* Tap **Chat** in the bottom navigation.
* Tap **"Run On-Device Screen Scan"**:
  * Scans simulated chat hierarchy (`AccessibilityService`).
  * Detects pig-butchering scripts, unverified APK links, and mule accounts.
  * Bubble pops up with warning overlay.
* **PII Guard Test**:
  * Type a 13-digit Thai National ID (e.g. `1234567890123`) or a 6-digit OTP $\rightarrow$ Immediate warning chime & vibration trigger!

### 5. Mobile Banking Transfer Brake
* Tap **Transfer Brake** in the bottom navigation.
* Tap **"Simulate 'Confirm Transfer' Tap"**:
  * When transferring funds to `1234567890`, the high-priority red **Transfer Brake** modal immediately blocks the screen.
  * Advises user: *“Legitimate brokers never ask for personal bank accounts. Cancel transfer to save funds.”*

---

## 🛠️ Running in Android Studio

If you want to build and run the native Kotlin code in Android Studio:
1. Open **Android Studio**.
2. Select **File $\rightarrow$ Open** and choose the directory:
   ```
   C:\Users\pc\.gemini\antigravity\scratch\scam-shield\android
   ```
3. Android Studio will automatically sync the project using Gradle 8.7 and JDK 17 (pre-configured in `gradle.properties`).
4. Select your target emulator (e.g., **Pixel 8** or **Medium Phone API 37**).
5. Click **Run ▶ (Shift + F10)**.
6. The app will build and launch natively on your emulator!

---

## ⚡ Direct APK Installation into Emulator (No Build Wait)

The debug APK has already been compiled:
- **Local APK Path:** `C:\Users\pc\.gemini\antigravity\scratch\scam-shield\android\app\build\outputs\apk\debug\app-debug.apk`
- **Public Download URL:** `http://10.0.2.2:3000/app/scam-shield.apk`

### Option A: Drag & Drop
Simply drag `app-debug.apk` from Windows File Explorer and drop it directly onto your running Android Studio emulator window. Android will automatically install it.

### Option B: ADB Install (Command Line)
With your emulator running, execute:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\pc\.gemini\antigravity\scratch\scam-shield\android\app\build\outputs\apk\debug\app-debug.apk"
```
To launch the app immediately via ADB:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.scamshield.app/.ui.MainActivity
```

