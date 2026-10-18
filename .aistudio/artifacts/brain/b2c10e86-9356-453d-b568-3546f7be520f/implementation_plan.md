# Scanova — QR & Barcode Scanner

A modern, fast, offline-first Android application for scanning QR codes and 1D/2D barcodes in real time or from gallery images, complete with intelligent content classification, secure context-aware actions, persistent local history, batch scanning, CSV export, and custom QR code generation.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and UX decisions incorporate your confirmed selections from Phase 1 and establish the complete scope for implementation:

- **Multi-Code Selection Behavior (Confirmed)**: When multiple QR codes or barcodes are detected simultaneously in the live camera frame or a gallery image, Scanova highlights the primary code closest to the frame center and renders interactive bounding boxes around secondary codes so the user can tap any detected code to switch selection before opening its details.
- **Phase 4 Enhancements Included (Confirmed)**:
  - **Continuous Batch Scanning Mode**: A toggleable mode on the Scanner and Settings screens that records consecutive unique scans with a floating counter badge and quick review tray without interrupting the camera feed.
  - **CSV History Export**: Full history export to a valid `.csv` file using Android's zero-permission `ActivityResultContracts.CreateDocument("text/csv")` contract.
  - **Custom QR Code Generator**: Built-in creator sheet/screen to generate, preview, and share crisp QR codes for Plain Text, URLs, and Wi-Fi credentials on-device.
- **Product Barcode External Lookups (Confirmed)**: Scanned product barcodes (`EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`) offer one-tap external lookup actions for both **Google Search** and **Open Food Facts / UPC Lookup**, alongside instant code copying.
- **Strict Offline-First & Zero-Permission Privacy**: Uses the bundled Google ML Kit Barcode Scanning model (`com.google.mlkit:barcode-scanning`) for immediate zero-network operation after install, Android Photo Picker (`PickVisualMedia`) for gallery scanning without storage permissions, and local Room + DataStore persistence.

---

## 1. Overview & Core Concept

- **What It Does**: Scanova decodes all standard 2D (`QR Code`, `Data Matrix`, `PDF417`, `Aztec`) and 1D (`EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`, `Code 128`, `Code 39`, `Code 93`, `Codabar`, `ITF`) symbologies from the live camera or gallery images, classifies the payload into 11 structured categories, and presents safe, context-aware actions.
- **Target Audience / Persona**: Everyday Android users and professionals needing a reliable, privacy-respecting scanner that works instantaneously offline without ads, trackers, or forced cloud accounts, localized natively in **Bahasa Indonesia** (primary) and **English** (secondary).
- **Key Value**: Combines high-speed CameraX + ML Kit frame analysis with security-first content inspection (masked Wi-Fi passwords, inspectable URLs, no auto-execution of untrusted payloads) and power-user tools like batch scanning, CSV export, and custom QR generation.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Live Camera Scanning & Multi-Code Selection**:
   - Opens directly to the **Scanner** tab. If camera permission is not yet granted, displays a clear contextual rationale card with a button to request permission—while keeping **Scan from Gallery** immediately usable even without camera access.
   - Displays a full-bleed CameraX preview with a rounded targeting reticle, four Indigo corner brackets, subtle pulse indicator, instruction pill (*"Arahkan kamera ke QR Code atau barcode"*), top control bar (Flashlight toggle, Batch mode toggle, Create QR shortcut), and bottom control bar (Gallery picker, Zoom presets `1x` / `2x` + pinch-to-zoom slider).
   - When a single code is detected (in standard mode), triggers optional haptic/sound feedback, pauses analysis, and navigates smoothly to **Result Detail**. When multiple codes are detected, pauses or locks frame overlays showing the central primary code highlighted in Indigo (`#6366F1`) and secondary codes in Amber (`#F59E0B`), allowing the user to tap any bounding box or confirm the primary code.
2. **Batch Scanning Flow**:
   - When **Batch Mode** is active, scanning a code triggers haptic/audio feedback, saves the entry to Room (preventing consecutive frame duplicates), and shows a floating bottom card with the latest decoded value, total batch count badge, and a *"Lihat Hasil Batch"* button without leaving the camera preview.
3. **Gallery Image Scanning**:
   - Tapping the Gallery button launches the native Android Photo Picker (`PickVisualMedia(ImageOnly)`).
   - Decodes the bitmap with EXIF orientation awareness. If multiple barcodes exist in the image, displays an interactive image overlay / selector so the user can inspect or pick which code to open. If no barcode is found, shows a clear snackbar/dialog with retry options.
4. **Smart Result Detail & Security Inspection**:
   - Displays the category badge & icon, barcode format (`QR_CODE`, `EAN_13`, etc.), formatted timestamp, scan source (`Kamera` or `Galeri`), structured parsed fields, and collapsible/scrollable raw value card.
   - **Security Guardrails**: URLs are shown in full before opening; suspicious or non-HTTP(S) schemes are flagged and treated as plain text; Wi-Fi passwords are masked (`••••••••`) with an eye toggle to reveal and one-tap copy for SSID/password.
   - Context-aware actions adapt per category: Open Browser, Copy Credentials, Dial Number, Send SMS, Compose Email, Add to Contacts (`ContactsContract.Intents.Insert`), Add to Calendar (`CalendarContract.Events`), Open Maps (`geo:`), Google Search, and Open Food Facts lookup.
5. **History, Filtering & CSV Export**:
   - Chronological list grouped by date (*Hari Ini*, *Kemarin*, or formatted date), with a real-time search bar, filter chips (`Semua`, `Favorit`, `QR Code`, `Barcode 1D`, plus category chips), sort order toggle (Terbaru / Terlama), multi-select deletion mode, swipe/button delete, and a top-bar **Export CSV** action.
6. **Custom QR Code Generator**:
   - Accessible from the Scanner top bar or History screen, enabling users to generate high-contrast QR codes for URLs, Plain Text, or Wi-Fi networks, preview them full-screen for another device to scan, or share the generated PNG image via Android's native Share sheet.
7. **Settings & Runtime Localization**:
   - Structured sections for **Preferensi Pindai** (Sound, Vibration, Auto-save history, Continuous batch mode), **Tampilan** (Terang, Gelap, Ikuti Sistem, Warna Dinamis Material You), **Data & Privasi** (Export CSV, Clear History with confirmation, Offline Privacy guarantee dialog), and **Umum** (Bahasa Indonesia / English selector with instant UI update, App Version, About Scanova).

### Visual Identity & Theme
- **Aesthetic Direction**: Modern minimalist utility with precision camera HUD optics—clean rounded cards, crisp typographic hierarchy, and distraction-free scanning controls.
- **Color Palette & Mood**:
  - **Primary Accent**: Indigo `#6366F1` (Dark mode primary `#818CF8`, Light mode primary `#4F46E5` / `#6366F1`).
  - **Dark Theme Surfaces**: Deep obsidian background `#0B1020`, elevated slate surface `#151B2E`, subtle surface container `#1E2640`.
  - **Light Theme Surfaces**: Crisp alabaster background `#F8FAFC`, pure white surface `#FFFFFF`, soft slate container `#F1F5F9`.
  - **Semantic Status**: Emerald Success `#10B981`, Amber Warning `#F59E0B`, Rose Error `#EF4444`.
- **Typography & Hierarchy**:
  - Locally bundled Google Fonts via `font-util`: **Plus Jakarta Sans** for clean geometric headings, titles, and UI labels, paired with **JetBrains Mono** for raw barcode payloads, EAN/UPC numbers, and Wi-Fi credentials.
- **Interactive Feedback & Motion**:
  - Smooth corner-bracket expansion and color shift (`#6366F1` -> `#10B981`) upon barcode lock, subtle breathing animation on the targeting reticle (respecting system reduced-motion settings), tactile haptic click/success pulses (`HapticFeedbackConstants`), and edge-to-edge system bar insets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Bundled ML Kit Barcode Scanning (`com.google.mlkit:barcode-scanning`)**
  - *Chosen Approach*: Use the bundled ML Kit artifact rather than Play Services dynamic download.
  - *Why*: Guarantees 100% offline readiness immediately upon app installation without waiting for background model downloads over cellular/Wi-Fi.
  - *Alternatives Considered*: Unbundled Play Services scanner (smaller APK size, but fails on first offline launch if the model hasn't downloaded).
- **Decision 2: Pure Kotlin Multi-Format Content Parser & QR Matrix Encoder**
  - *Chosen Approach*: Isolate barcode payload classification (`URL`, `WIFI`, `VCARD`, `MECARD`, `GEO`, `VEVENT`, `MATMSG`, `MAILTO`, `SMSTO`, `TEL`, `PRODUCT`, `TEXT`) in a dedicated, stateless domain parser, and include ZXing Core (`com.google.zxing:core`) for offline QR bitmap generation.
  - *Why*: Makes all 11 content types fast, deterministic, and 100% unit-testable on the JVM while enabling offline custom QR code creation.
- **Decision 3: Bilingual In-App Language Switching (Bahasa Indonesia Default + English)**
  - *Chosen Approach*: Provide comprehensive `values/strings.xml` (Bahasa Indonesia default) and `values-en/strings.xml` (English) resources alongside a locale-aware wrapper in Compose coupled to DataStore preferences.
  - *Why*: Ensures default strings are natural, idiomatic Bahasa Indonesia while allowing instant runtime toggling between Indonesian and English from Settings across all Android API levels (API 26+).

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                         │
│                                                                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │  ScannerScreen   │  │   ResultScreen   │  │      HistoryScreen       │  │
│  │  - CameraX HUD   │  │  - Smart Actions │  │  - Search & Category     │  │
│  │  - Multi-Code    │  │  - Masked Wi-Fi  │  │    Filter Chips          │  │
│  │    Tap Selector  │  │  - URL Inspector │  │  - Multi-Select & Delete │  │
│  │  - Batch Counter │  │  - Product Web   │  │  - CSV Export Launcher   │  │
│  │  - Photo Picker  │  │    Lookups       │  │  - QR Generator Sheet    │  │
│  └────────┬─────────┘  └────────┬─────────┘  └────────────┬─────────────┘  │
│           │                     │                         │                │
│           ▼                     ▼                         ▼                │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │ ScannerViewModel │  │ ResultViewModel  │  │     HistoryViewModel     │  │
│  └────────┬─────────┘  └────────┬─────────┘  └────────────┬─────────────┘  │
└───────────┼─────────────────────┼─────────────────────────┼────────────────┘
            │                     │                         │
            ▼                     ▼                         ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     Domain & Core Scanner / Parser Layer                   │
│                                                                            │
│  ┌───────────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ MlKitBarcodeAnalyzer      │  │ SmartContentParser                    │  │
│  │ - CameraX ImageAnalysis   │  │ - Classifies URL, Wi-Fi, vCard/MeCard │  │
│  │ -STRATEGY_KEEP_ONLY_LATEST│  │   Geo, Calendar, SMS, Email, Phone,   │  │
│  │ - Gallery Bitmap Decoder  │  │   Product (EAN/UPC), Plain Text       │  │
│  │ - Multi-Box Normalizer    │  │ - URL Scheme & Security Sanitizer     │  │
│  └───────────────────────────┘  └───────────────────────────────────────┘  │
└─────────────────────────────────────────┬──────────────────────────────────┘
                                          │
                                          ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                        Data Layer (Offline-First)                          │
│                                                                            │
│  ┌───────────────────────────────────┐  ┌───────────────────────────────┐  │
│  │ ScanHistoryRepository             │  │ UserPreferencesRepository     │  │
│  │ - Room Database (ScanEntity, DAO) │  │ - Jetpack DataStore           │  │
│  │ - Reactive Flow<List<ScanRecord>> │  │ - Sound, Vibration, AutoSave, │  │
│  │ - Search, Filter, Sort, Favorites │  │   BatchMode, Theme, Dynamic   │  │
│  │ - Session Duplicate Guard         │  │   Color, Language (ID / EN)   │  │
│  │ - RFC-4180 CSV Serializer         │  │                               │  │
│  └───────────────────────────────────┘  └───────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State
- **`ScanEntity` (Room Table `scan_history`)**:
  - `id: Long` (Primary Key, auto-generated)
  - `rawValue: String` (Exact decoded payload)
  - `displayTitle: String` (Human-readable summary, e.g., SSID name, contact name, domain, or formatted number)
  - `contentType: String` (`URL`, `WIFI`, `EMAIL`, `PHONE`, `SMS`, `CONTACT`, `LOCATION`, `CALENDAR`, `PRODUCT`, `TEXT`, `UNKNOWN`)
  - `barcodeFormat: String` (`QR_CODE`, `DATA_MATRIX`, `PDF417`, `AZTEC`, `EAN_13`, `EAN_8`, `UPC-A`, `UPC-E`, `CODE_128`, `CODE_39`, `CODE_93`, `CODABAR`, `ITF`)
  - `is2D: Boolean` (True for QR/Aztec/DataMatrix/PDF417, False for 1D barcodes)
  - `timestamp: Long` (Epoch millis)
  - `scanSource: String` (`CAMERA` or `GALLERY`)
  - `isFavorite: Boolean`
- **`ParsedScanContent` (Sealed Hierarchy)**:
  - Strongly typed representations for `UrlContent(url, isHttps, host, isSuspiciousScheme)`, `WifiContent(ssid, password, encryptionType, isHidden)`, `ContactContent(name, organization, phones, emails, address, note)`, `EmailContent(address, subject, body)`, `SmsContent(phoneNumber, message)`, `PhoneContent(phoneNumber)`, `LocationContent(latitude, longitude, query)`, `CalendarContent(summary, description, location, startRaw, endRaw)`, `ProductContent(code, symbology, countryHint)`, and `PlainTextContent(text)`.

### Interactive Component & State Mapping
- **CameraX Lifecycle & Hardware Controls**:
  - `ScannerViewModel` tracks `torchEnabled`, `hasFlashUnit`, `zoomRatio`, `maxZoomRatio`, `batchMode`, `batchScannedItems`, and `detectedCodes`.
  - Pinch-to-zoom (`detectTransformGestures`) and preset zoom chips (`1x`, `2x`, `Max`) update `CameraControl.setZoomRatio()`. Tap-to-focus converts screen tap coordinates via `SurfaceOrientedMeteringPointFactory` into a `FocusMeteringAction` with a visual focus ring indicator.
- **Multi-Code Selection Overlay**:
  - When ML Kit returns >1 barcode in a frame or gallery photo, normalized bounding coordinates (`[0f..1f]`) are mapped to the Compose canvas. The code closest to `(0.5f, 0.5f)` is selected by default; tapping any secondary bounding box switches the active selection card at the bottom of the screen, where the user can tap **"Buka Detail"** to inspect it.
- **Safe External Intent Execution**:
  - All external actions (`ACTION_VIEW`, `ACTION_DIAL`, `ACTION_SENDTO`, `ContactsContract.Intents.Insert.ACTION`, `CalendarContract.Events`) are wrapped in a safe launcher helper with `try / catch (ActivityNotFoundException)` that surfaces a friendly localized snackbar if no compatible app is installed on the device.
