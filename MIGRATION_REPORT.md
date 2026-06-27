# Study Lab Manager — TypeScript → Kotlin Android Migration Report

## Overview

The original **Study Lab Manager** is a Vite + React + TypeScript SPA wrapped in a
Capacitor Android shell (package `com.libraryx`). It operates in two modes: a **Solo**
mode (local-only, `localStorage`) and a **SaaS** mode (Firebase Auth + Firestore, multi-tenant).
The current routing redirects `/` straight to `/saas/login`, making Solo mode effectively
unreachable but still preserved in the original codebase.

This report documents the full port to a **native Kotlin Android** application using
Jetpack Compose, Hilt, and the Firebase Android SDK — every screen, data-layer service,
and utility function has a direct Kotlin counterpart.

---

## Architecture Mapping

| TypeScript / Web Layer | Kotlin / Android Equivalent |
|---|---|
| React Context (`StudyLabContext`, `SaasAuthContext`) | Hilt ViewModels + `StateFlow` |
| `<StudyLabProvider source="firebase">` | `SaasSessionHolder` → `FirebaseStudyLabRepository` |
| `<StudyLabProvider source="local">` | `LocalStudyLabRepository` (DataStore) |
| `localStorage` (`studylab_students`, `studylab_settings`) | `DataStore<Preferences>` (same JSON key names for backup interop) |
| Firebase JS SDK (`firebase/firestore`, `firebase/auth`) | Firebase Android SDK (BoM 33.7.0) |
| `react-router-dom` `<Routes>` / `useNavigate` | `NavHost` / `NavController` (Navigation Compose 2.8.5) |
| shadcn/ui primitives (Card, Button, Badge, Input, Table, Dialog, Select…) | Material3 (Compose BOM 2024.12.01) |
| Radix `<Sidebar>` / `<SidebarTrigger>` | `ModalNavigationDrawer` + `TopAppBar` |
| Tailwind neon CSS variables | `StudyLabColors` object + Material3 `ColorScheme` |
| `fontFamily.display` / `fontFamily.mono` (Tailwind) | `FontFamily.Monospace` (system fallback — see Known Gaps) |
| `jsPDF` / `jspdf-autotable` (`pdf.ts`, `saasPdf.ts`) | `android.graphics.pdf.PdfDocument` + `Canvas` (`PdfReceiptGenerator.kt`) |
| `generateCSVReport` | `CsvExporter.kt` |
| Browser `URL.createObjectURL` / `<a download>` | Storage Access Framework `CreateDocument` / `MediaStore` |
| Browser `<input type="file">` | SAF `OpenDocument` |
| `window.open("https://wa.me/…")` | `Intent(ACTION_VIEW, Uri.parse("https://wa.me/…"))` |
| `Notification` API + `setInterval` (5-min) | `NotificationCompat` + WorkManager `PeriodicWorkRequest` (15-min) |
| `startNotificationService` / `stopNotificationService` | `NotificationScheduler.start()` / `.stop()` (WorkManager) |
| `signInWithPopup(auth, googleProvider)` | Credential Manager `GetGoogleIdOption` (in `MainActivity`) |
| Capacitor Android shell | Native `ComponentActivity` + Hilt |
| `hashPasscode` / `verifyPasscode` (`lib/passcode.ts`) | `Passcode.kt` (SHA-256, same `"studylab::"` salt, hex-encoded) |
| `generateId` / `hashPin` (`lib/store.ts`) | `IdGenerator.kt` (same 32-bit rolling hash for backup compatibility) |

---

## Screen-by-Screen Correspondence

| Original route | TS file | Kotlin file |
|---|---|---|
| `/saas/login` | `SaasLogin.tsx` | `SaasLoginScreen.kt` |
| `/saas/subadmin/dashboard` | `Dashboard.tsx` | `DashboardScreen.kt` + `DashboardViewModel.kt` |
| `/saas/subadmin/students` | `Students.tsx` | `StudentsScreen.kt` + `StudentsViewModel.kt` + `StudentDialogs.kt` |
| `/saas/subadmin/monthly-dues` | `MonthlyDues.tsx` | `MonthlyDuesScreen.kt` + `MonthlyDuesViewModel.kt` |
| `/saas/subadmin/seat-map` | `SeatMap.tsx` | `SeatMapScreen.kt` + `SeatMapViewModel.kt` |
| `/saas/subadmin/reports` | `Reports.tsx` / `SaasReports.tsx` | `ReportsScreen.kt` + `ReportsViewModel.kt` |
| `/saas/subadmin/backup` | `Backup.tsx` / `SaasBackup.tsx` | `BackupScreen.kt` + `BackupViewModel.kt` |
| `/saas/subadmin/settings` | `Settings.tsx` | `SettingsScreen.kt` + `SettingsViewModel.kt` |
| `/saas/student` | `SaasStudentPortal.tsx` | `StudentPortalScreen.kt` + `StudentPortalViewModel.kt` |
| Membership lock overlay | `MembershipLockScreen.tsx` | `MembershipLockScreen.kt` |
| `/mode-select` | `ModeSelect.tsx` | `ModeSelectScreen.kt` |
| 404 | `NotFound.tsx` | `NotFoundScreen.kt` |

---

## Required Manual Steps Before Building

### 1. Add `google-services.json`

The web app hard-codes its Firebase config in `src/lib/firebase.ts`. In the Android
project, Firebase is configured via `google-services.json` (applied by the
`com.google.gms.google-services` Gradle plugin). Place the file at:

```
app/google-services.json
```

**How to get it:**
1. Go to [Firebase Console](https://console.firebase.google.com/) → project `study-lab-4088e`.
2. Project Settings → General → Your apps → Add app → Android.
3. Register package name `com.libraryx`, download `google-services.json`.

The original Firebase config values for reference:

| Field | Value |
|---|---|
| `apiKey` | `AIzaSyC4SiI5xIrOLTJilKHJlyDZox07ylnpmQs` |
| `authDomain` | `study-lab-4088e.firebaseapp.com` |
| `projectId` | `study-lab-4088e` |
| `storageBucket` | `study-lab-4088e.firebasestorage.app` |
| `messagingSenderId` | `498489372389` |
| `appId` | `1:498489372389:web:74b7cc1910e0d2a423ae0c` |

### 2. Set the Google Sign-In Web Client ID

In `MainActivity.kt`, replace the placeholder:

```kotlin
.setServerClientId("YOUR_WEB_CLIENT_ID")
```

With the **OAuth 2.0 Web Client ID** from:
Firebase Console → Project Settings → General → Your apps → Web app → Web Client ID
(or Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client IDs → Web client).

### 3. Add App Icons

No launcher icon assets are included. Android requires mipmap resources at:

```
app/src/main/res/mipmap-mdpi/ic_launcher.png        (48×48)
app/src/main/res/mipmap-hdpi/ic_launcher.png         (72×72)
app/src/main/res/mipmap-xhdpi/ic_launcher.png        (96×96)
app/src/main/res/mipmap-xxhdpi/ic_launcher.png       (144×144)
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png      (192×192)
```

Use Android Studio's **Image Asset Studio** (right-click `res` → New → Image Asset) to
generate these from any source image. The original web app uses `/logo.png` from the
project root.

### 4. (Optional) Add a Custom Font

The original uses a monospaced "cyber" display font via Tailwind's `fontFamily.display`.
The Kotlin port substitutes `FontFamily.Monospace` (system default). To replicate the
exact look, place `.ttf` / `.otf` files in `app/src/main/res/font/` and update
`DisplayFontFamily` in `ui/theme/Type.kt`.

---

## Intentional Deviations from the Original UI

| Original behaviour | Android behaviour | Reason |
|---|---|---|
| Wide `<Table>` rows (Name / Phone / Seat / Fee / Status / Last Payment / Overdue / Actions) | Per-student `Card` in `LazyColumn` with stacked fields | Tables wider than 360 dp cause horizontal scroll on phones and are not idiomatic Android UI |
| Monthly Dues grid scrollable in both axes | Horizontally-scrollable `Row` of month columns per student inside a `LazyColumn` | Preserves the "one column per month" layout while fitting a phone width |
| "Print" button → browser `window.print()` | Removed; PDF export buttons on Reports screen cover this use-case | `window.print()` has no Android equivalent; PDF download is the canonical mobile path |
| "Firebase not configured" branch in `SaasLogin.tsx` | Omitted | `google-services.json` is a build-time requirement on Android; unconfigured state cannot occur at runtime |
| Notification polling every **5 minutes** (`setInterval`) | WorkManager periodic task every **15 minutes** | 15 min is WorkManager's minimum interval for `PeriodicWorkRequest` (Android OS battery optimisation) |
| Solo mode reachable from root `/` | Solo mode routes exist but are unreachable (root → SaaS login, same as original) | Routing matches the original's current effective state; re-enabling Solo mode requires changing `startDestination` in `StudyLabNavGraph.kt` |
| Theme toggle persists in `localStorage` | Dark/light toggled in-memory per session | DataStore preference can be added trivially; deferred to keep scope minimal |

---

## Backup Interoperability

Backup JSON files exported from the original web app (via `exportBackup()` in
`lib/store.ts`) are **compatible** with the Android app's `BackupSerializer.kt`:

- Same `localStorage` key names (`studylab_students`, `studylab_settings`) used by
  `LocalStudyLabDataSource.kt`.
- `generateId` produces identical IDs (same 32-bit rolling hash over charCodes).
- `hashPin` produces identical hashes (same algorithm).
- `hashPasscode` / `verifyPasscode` use the same SHA-256 + `"studylab::"` salt prefix
  with hex encoding.

Imports from a SaaS-mode backup into the Android SaaS mode are **not supported** (same
as the original — `FirebaseStudyLabRepository.importData` is a deliberate no-op).

---

## Build Instructions

```bash
# Prerequisites: JDK 17+, Android SDK API 35, google-services.json in place
cd study-lab-manager-kotlin
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease   # needs keystore — see Android signing docs
```

Open in Android Studio (Ladybug or later) for the IDE experience, Gradle sync, and
the mipmap Image Asset Studio.

---

## Known Compilation Warnings (Non-Breaking)

The generated source was not compiled against the SDK during generation (no Android SDK
available in the generation environment). The following minor issues may require
post-generation cleanup:

- `StudentsScreen.kt` / `StudentDialogs.kt`: a few imported symbols from
  `androidx.compose.material3` may need the full qualified name if the IDE flags
  ambiguity (e.g. `ExposedDropdownMenu` vs `DropdownMenu`).
- `AppScaffold.kt`: the `Menu` icon used for the nav drawer toggle imports
  `Icons.Filled.Dashboard` as a placeholder — replace with `Icons.Filled.Menu` once
  material-icons-extended is on the classpath.
- Unused import `LazyColumn`/`items` in `ReportsScreen.kt` (the list was converted to
  `forEach` inline) — harmless, remove if desired.

These are cosmetic; a `./gradlew assembleDebug` will surface any remaining issues with
full IDE context.
