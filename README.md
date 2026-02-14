# YahtzeeGoDice

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Build Flavors](https://img.shields.io/badge/Flavors-real%20%7C%20mock-blue)
![Silesian University of Technology](https://img.shields.io/badge/University-Silesian%20University%20of%20Technology-0057B8)
![2026](https://img.shields.io/badge/Year-2026-orange)

**YahtzeeGoDice** is an Android mobile app that brings classic **Yahtzee** to physical **GoDice** smart dice.
The app uses Bluetooth Low Energy (BLE) for real dice, and also supports a **mock** mode for development without hardware.

---

## 🎯 Features

- Connect and manage multiple GoDice devices
- Real-time dice events: roll, stable face, color, charging state, battery level
- Full Yahtzee gameplay flow:
  - Exactly 5 dice required per turn
  - Up to 2 rerolls per turn (hold/lock selected dice)
  - 13 standard Yahtzee scoring categories
  - 1-4 players
  - Final scoreboard with winner highlight
- Optional `mock` flavor for hardware-free testing
- UI localization support: `en-US` and `pl`

---

## 🧩 Technologies

- **Kotlin / Android**
- **Bluetooth Low Energy** communication
- Local GoDice SDK module (`godicesdklib`) with JNI bridge to native C code
- Manager + Listener/Observer-style event distribution

---

## 📦 Project Structure

- `goDiceTest/app` - main Android application module
- `goDiceTest/godicesdklib` - Android library module exposing `GoDiceSDK` (JNI + native API)
- `common` - native C API files (`godiceapi.c/.h`)
- `common/test` - native C++ test target (CMake)

---

## ✅ Requirements

- Android Studio
- JDK 11
- Android SDK for app module:
  - `minSdk = 33`
  - `targetSdk = 36`
  - `compileSdk = 36`
- For `real` flavor testing:
  - physical Android device with BLE
  - Bluetooth enabled
  - Location services enabled (GPS)

> BLE behavior is device-dependent; emulator support is typically insufficient for real GoDice testing.

---

## 🚀 Build & Run

From project root:

```bash
cd goDiceTest
```

Build real flavor:

```bash
./gradlew assembleRealDebug
```

Install real flavor on connected device:

```bash
./gradlew installRealDebug
```

Build/install mock flavor:

```bash
./gradlew assembleMockDebug
./gradlew installMockDebug
```

---

## 🧪 Mock Dice Mode

Use the `mock` build flavor when you do not have physical GoDice available.

- App uses `MockDiceManager` instead of BLE manager
- Mock dice are created after tapping **Scan Dice**
- After connecting mock dice, rolls are simulated by shaking the phone (accelerometer)
- Game logic and scoring flow remain the same as in `real`

---

## 🎮 App Flow

1. `MainActivity` - scan and connect dice
2. Select exactly 5 dice for gameplay
3. `PlayerSetupActivity` - set player count and names (1-4)
4. `GameActivity` - Yahtzee turns and scoring
5. `ResultsActivity` - final scores and winner(s)

---

## 🔗 GoDice SDK Integration

- `godicesdklib` loads native `godicesdklib` via JNI
- Native build compiles:
  - `goDiceTest/godicesdklib/src/main/c/jni/jni_def.c`
  - `common/godiceapi.c`
- `DiceManager` sets `GoDiceSDK.listener` and maps SDK callbacks into app-level state updates

---

## 🔐 Permissions

Manifest declarations include:

- `BLUETOOTH`, `BLUETOOTH_ADMIN`
- `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`

Runtime flow requests the required permissions/enablers before scanning.

---

## 🧱 Tests

Android tests (template-level):

```bash
cd goDiceTest
./gradlew test
./gradlew connectedAndroidTest
```

Native C++ test target exists in `common/test` (separate CMake flow).

---

## License

License: **All Rights Reserved (view-only)**

Note: GoDice SDK/API components may be subject to separate licensing terms.

---

## Authors

- **Piotr Marcol** - [p-marcol](https://github.com/p-marcol)
- **Jakub Barylak** - [Jakub-Barylak](https://github.com/Jakub-Barylak)

---

_Project developed as part of the course **Mobile Application Design**, Silesian University of Technology, 2026._

---

That's all folks!
Place your bets, shuffle the code, and may RNGesus smile upon you.
