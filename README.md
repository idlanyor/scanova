# Scanova — QR & Barcode Scanner

**Scan Smarter, Instantly.**

Scanova is a fast, modern, privacy-respecting, and 100% offline-first Android application for scanning 2D QR codes and 1D barcodes in real time or from gallery images.

---

## Key Features

1. **Real-Time CameraX + Bundled ML Kit Scanner**
   - Supports all 13 ML Kit 2D (`QR Code`, `Data Matrix`, `PDF417`, `Aztec`) and 1D (`EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`, `Code 128`, `Code 39`, `Code 93`, `Codabar`, `ITF`) barcode symbologies.
   - Uses the bundled Google ML Kit Barcode Scanning model (`com.google.mlkit:barcode-scanning`) so scanning works immediately offline upon installation.
   - Hardware controls: Flashlight (torch) toggle, Pinch-to-Zoom + `1x`/`2x`/`4x` zoom buttons, and Tap-to-Focus with visual focus ring.
2. **Multi-Code Interactive Bounding Box Selection**
   - When multiple codes appear simultaneously in the camera viewfinder or a gallery image, Scanova highlights the primary code closest to the frame center in Indigo (`#6366F1`) and secondary codes in Amber (`#F59E0B`), allowing users to tap any bounding box or chip to inspect and open its details.
3. **Continuous Batch Scanning Mode**
   - Toggleable batch mode records consecutive unique scans with haptic/sound feedback and a floating session counter card without leaving the camera preview.
4. **Zero-Permission Gallery Image Scanning**
   - Uses the native Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) with EXIF orientation correction and efficient bitmap downsampling—no broad storage permissions required.
5. **Smart Content Recognition & Security Guardrails**
   - Automatically classifies payloads into **URL**, **Wi-Fi**, **Contact (vCard/MeCard)**, **Calendar Event (iCal)**, **Geographic Location (`geo:`)**, **Email (`mailto:`/`MATMSG:`)**, **SMS (`smsto:`)**, **Phone (`tel:`)**, **Product Barcode (`EAN`/`UPC` with GS1 country origin hint)**, **Plain Text**, and **Unknown**.
   - **Security by Design**: Never auto-opens URLs, dials numbers, or connects to Wi-Fi without explicit user confirmation. Suspicious URI schemes (`javascript:`, `data:`, `file:`, `intent:`) are flagged and neutralized as plain text. Wi-Fi passwords are masked by default with a reveal toggle.
6. **Persistent Offline History & RFC-4180 CSV Export**
   - Backed by Room (`ScanovaDatabase`) with real-time search, date grouping (*Hari Ini*, *Kemarin*, formatted date), category/2D/1D/Favorites filter chips, sort order toggle, multi-select batch deletion, and one-tap CSV export via Android's document creation contract.
7. **Built-In Custom QR Code Generator**
   - Generate, preview, save, and share high-resolution QR codes for Plain Text, URLs, and Wi-Fi credentials offline using ZXing Core.
8. **Bilingual Localization & Material Design 3 Theme**
   - Native **Bahasa Indonesia** (default) and **English** support with instant runtime switching in Settings, alongside Light, Dark (`#0B1020` / `#151B2E`), System Default, and Material You Dynamic Color modes.

---

## Architecture

```text
app/src/main/java/com/example/
├── core/
│   ├── design/        # Category icons, accent colors, and localized label mappings
│   ├── model/         # Domain models (ScanRecord, ParsedScanContent, BarcodeFormatType)
│   ├── navigation/    # Adaptive NavigationBar / NavigationRail + NavHost routes
│   ├── scanner/       # SmartContentParser, MlKitBarcodeAnalyzer, QrCodeGenerator
│   └── util/          # ExternalIntentHelper, ScanFeedbackHelper, LocalizedAppWrapper
├── data/
│   ├── local/         # Room ScanEntity, ScanDao, ScanovaDatabase
│   ├── preferences/   # Jetpack DataStore UserPreferencesRepository
│   └── repository/    # ScanHistoryRepository with session duplicate guard & CSV builder
├── feature/
│   ├── scanner/       # ScannerScreen, ScannerViewModel, QrGeneratorSheet
│   ├── result/        # ResultScreen, ResultViewModel
│   ├── history/       # HistoryScreen, HistoryViewModel
│   └── settings/      # SettingsScreen, SettingsViewModel
├── ui/theme/          # Color.kt, Type.kt (Plus Jakarta Sans & JetBrains Mono), Theme.kt
└── MainActivity.kt    # Edge-to-edge activity & ScanovaApp root composable
```

---

## Build, Test & Release Instructions

### 1. Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### 2. Build Debug APK
```bash
gradle :app:assembleDebug
```
The generated debug APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Automated CI/CD & GitHub Releases
Proyek ini dilengkapi dengan GitHub Actions workflow di `.github/workflows/release.yml`.
- **Triggers**:
  - **Otomatis setiap ada commit / push** ke branch `main` atau `master` (menghasilkan rilis dengan penomoran versi tanggal & commit hash, misal `v2026.10.18-a1b2c3d`).
  - **Otomatis saat push Git tag versi** (misal: `git tag v1.0.0 && git push origin v1.0.0`).
  - **Manual** melalui tab **Actions > Build & Release Scanova > Run workflow** di GitHub.
- **Output di GitHub Releases**:
  - `scanova-<version>.apk` (file APK siap pasang di perangkat fisik).
  - `scanova-<version>.aab` (file Android App Bundle untuk Google Play Store).
  - Release dipublikasikan langsung ke [https://github.com/idlanyor/scanova/releases](https://github.com/idlanyor/scanova/releases).

## Manual Physical Device Validation Checklist

Because real-time optical camera performance depends on physical camera sensors and lighting conditions, perform the following checks on a physical Android device before production release:

- [ ] **QR Code on Paper**: Scan a printed QR code under normal indoor lighting and verify instant lock and navigation to Result Detail.
- [ ] **QR Code on Another Screen**: Scan a QR code displayed on a laptop or second smartphone screen at varying brightness levels.
- [ ] **EAN-13 & UPC-A Retail Product Barcodes**: Scan a physical retail package barcode and test the **Google Search** and **Open Food Facts** lookup buttons.
- [ ] **Code 128 Barcode**: Scan a logistics/shipping label barcode.
- [ ] **Multi-Code Selection**: Hold two QR/barcodes in frame simultaneously, verify the central primary code is highlighted in Indigo and secondary codes in Amber, and tap a secondary box to switch selection.
- [ ] **Batch Scanning Mode**: Enable **Batch** on the Scanner top bar, scan 3 different codes consecutively, and verify the floating counter increments without leaving the camera view.
- [ ] **Flashlight (Torch) & Zoom**: Toggle the flashlight in a dim environment and test pinch-to-zoom plus `1x` / `2x` preset buttons.
- [ ] **Gallery Import**: Select a screenshot containing a QR code via the Gallery button and verify decoding works even if Camera permission is denied.
- [ ] **Background / Foreground Transitions**: Put the app in the background while on the Scanner screen, return to foreground, and confirm the camera preview resumes cleanly with the torch turned off.
