# راهنمای ساخت و انتشار اندروید

## پیش‌نیازها

| ابزار | نسخه پیشنهادی |
|---|---|
| JDK | 21 (با jlink) |
| Android SDK Platform | 35 |
| Android Build Tools | 35.0.0 |
| Gradle | 8.9 (wrapper موجود است) |
| Kotlin | 2.0.21 |
| AGP | 8.7.3 |

`local.properties` باید مسیر SDK را داشته باشد:

```properties
sdk.dir=/path/to/android-sdk
```

## دستورهای روزمره

```bash
# تست دامنه و دیتابیس
./gradlew :domain:test :data:testDebugUnitTest

# کامپایل و ساخت دیباگ
./gradlew :app:assembleDebug

# ساخت نسخه انتشار (APK + AAB)
./gradlew :app:assembleRelease :app:bundleRelease

# پاک‌سازی
./gradlew clean
```

خروجی‌ها:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

## نصب و تست روی دستگاه

```bash
adb devices
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat | grep -i hesabyar
```

اگر نسخه دیباگ قبلی نصب است، چون applicationId دیباگ `ir.hesabyar.app.debug` است، می‌توانید هر دو را هم‌زمان نصب کنید.

## امضا

فایل‌های `release.keystore` و `keystore.properties` در ریشه پروژه قرار دارند. alias پیش‌فرض `hesabyar` است.

برای انتشار عمومی:

1. یک keystore جدید بسازید:
   ```bash
   keytool -genkeypair -v -keystore hesabyar-release.jks \
     -alias hesabyar -keyalg RSA -keysize 4096 -validity 10000
   ```
2. مقادیر را در `keystore.properties` جایگزین کنید.
3. کلید و رمزها را در جای امن (نه در Git عمومی) نگه دارید.
4. دوباره `assembleRelease` و `bundleRelease` اجرا کنید.

> کلید موجود فقط برای تحویل نمونه است. تغییر کلید بعد از انتشار در کافه‌بازار مجاز نیست.

## انتشار در کافه‌بازار

1. فایل `app-release.aab` را آپلود کنید (یا APK را برای تست).
2. امضای AAB باید با کلید ثابت شما باشد؛ کلید را گم نکنید.
3. آیکون، اسکرین‌شات، توضیحات فارسی و رده‌بندی سنی را تکمیل کنید.
4. سیاست حریم خصوصی: برنامه آفلاین است و داده‌ای ارسال نمی‌کند؛ همین را شفاف بنویسید.
5. قبل از انتشار، نسخه را در `app/build.gradle.kts` افزایش دهید (`versionCode` و `versionName`).

## عیب‌یابی

| مشکل | راه‌حل |
|---|---|
| `SDK location not found` | `local.properties` یا متغیر `ANDROID_HOME` را تنظیم کنید. |
| خطای jlink / transform | JDK کامل ۲۱ نصب باشد، نه فقط JRE. |
| خطای KSP/Hilt | `./gradlew --stop` و سپس build مجدد؛ کش‌ها را پاک کنید. |
| APK نصب نمی‌شود | نسخه قبلی با همان applicationId و امضای متفاوت را حذف کنید. |
| AAB آپلود نمی‌شود | versionCode تکراری است یا امضا با نسخه قبلی فرق دارد. |

## CI پیشنهادی

```yaml
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: '21' }
- run: ./gradlew :domain:test :data:testDebugUnitTest :app:assembleRelease :app:bundleRelease
```

کلید امضا را از Secretهای CI تزریق کنید و هرگز در مخزن عمومی قرار ندهید.
