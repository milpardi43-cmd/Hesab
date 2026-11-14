# Hesabyar — project state

## Environment
- Android SDK: `/home/ubuntu/android-sdk` (platform 35, build-tools 35.0.0)
- Gradle 8.9 wrapper, AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt 2.52
- Build: `cd /home/ubuntu/hesabyar-android && ANDROID_HOME=/home/ubuntu/android-sdk ./gradlew assembleRelease bundleRelease`
- JDK 21 with jlink installed (openjdk-21-jdk-headless)

## Modules
- `:domain` — pure Kotlin (models, AccountingEngine, JalaliDate, PersianText, use cases, repo interfaces). 29 unit tests pass.
- `:core` — Android library: Room entities/DAOs/DB, DataStore settings, notifications, XLSX writer, design system (theme + components), backup export DAO.
- `:data` — repository implementations, PostingExecutor (atomic Room transaction), backup JSON/CSV/XLSX, Hilt bindings.
- `:app` — Compose UI, navigation, workers, onboarding.

## Implemented
- Domain engine covering sale (cash/credit/mixed), receipt, supplier payment, expense (paid/unpaid), purchase with freight allocation, sale return, transfer, cash/stock adjustment, generic reversal.
- Room schema v1 (19 tables, FKs + indices), converters, DAOs, atomic posting.
- Settings DataStore, notification helper.
- Theme (deep green, Vazirmatn), components (money, date picker, charts, fields, transaction rows).
- Full Compose UI: splash/onboarding, setup wizard, dashboard, sale, sale return, purchase, expense, transactions list/detail, products/form/detail/stock adjust, contacts/form/detail/payment/opening balance, accounts/detail/transfer/adjust, reports, reminders, settings/data management/subscription/about.
- ReminderWorker and AutoBackupWorker; Hilt-injected application.
- Backup JSON, CSV and XLSX export; import/recovery flows.
- Unit and Room integration tests: 32 passing (21 accounting + 8 Jalali + 3 repository).
- Signed release APK and AAB built; debug APK built.
- iOS SwiftUI starter with XcodeGen spec, sample data and main screens.
- Web demo (separate Webdev project) with Persian RTL UI and localStorage sample data.

## Artifacts
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Deliverables copies: `/home/ubuntu/deliverables/`
- Docs: `README.md`, `docs/ARCHITECTURE.md`, `docs/BUILD.md`, `docs/IOS_GUIDE.md`, `ios/README.md`

## Known limitations
- Single-user, offline only; no cloud sync yet.
- Subscription/payment is a UI placeholder, not connected to a real gateway.
- iOS starter is not full parity and was not compiled here (Linux, no Xcode).
- Sample signing key must be replaced before public distribution.
