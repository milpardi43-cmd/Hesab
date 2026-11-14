# حسابیار | Hesabyar

اپلیکیشن فارسی مدیریت مالی و حسابداری فروشگاهی برای کسب‌وکارهای کوچک ایرانی؛ با Kotlin، Jetpack Compose، Room و معماری Offline-First. رابط راست‌به‌چپ، تقویم هجری شمسی و مبالغ به تومان.

## نسخه نمایشی آنلاین

[مشاهده حسابیار در مرورگر](https://milpardi43-cmd.github.io/Hesab/) · این نسخه برای نمایش تعاملی است و داده‌های نمونه را فقط در مرورگر ذخیره می‌کند.

## دانلود اندروید ۱٫۰٫۰

فایل‌های APK و AAB در [صفحه Releases](https://github.com/milpardi43-cmd/Hesab/releases) قرار می‌گیرند. APK برای نصب مستقیم است؛ AAB برای ارسال به فروشگاه‌های اندرویدی.

## بخش‌های مخزن

- [`android/`](android/) — سورس اندروید Kotlin/Compose شامل ماژول‌های app, domain, data و core؛ به‌همراه استارتر SwiftUI در `android/ios/`.
- [`web/`](web/) — نسخه نمایشی استاتیک HTML/CSS/JS که با GitHub Pages منتشر می‌شود.
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — معماری نرم‌افزار و جریان تراکنش‌ها.
- [`BUILD-ANDROID.md`](BUILD-ANDROID.md) — ساخت، امضا و انتشار Android.
- [`IOS-GUIDE.md`](IOS-GUIDE.md) — راهنمای ساخت استارتر iOS در macOS/Xcode.

## ویژگی‌های اصلی Android

فروش نقدی/نسیه/ترکیبی و برگشت از فروش؛ خرید با کرایه و میانگین بهای تمام‌شده؛ هزینه‌ها؛ کالا و انبار؛ مشتری و تأمین‌کننده؛ حساب‌ها و انتقال وجه؛ گزارش‌های مالی؛ یادآوری‌ها؛ پشتیبان JSON و خروجی CSV/Excel. عملیات مالی در Room و در تراکنش اتمیک ذخیره می‌شوند.

## ساخت Android

پیش‌نیاز: JDK 21 و Android SDK 35.

```bash
cd android
# local.properties با sdk.dir را ایجاد کنید
./gradlew :domain:test :data:testDebugUnitTest :app:assembleDebug
```

برای ساخت Release امضاشده، فایل `android/keystore.properties` و keystore اختصاصی بسازید؛ الگو و نکته‌های امنیتی در [BUILD-ANDROID.md](BUILD-ANDROID.md) است. هیچ کلید امضایی یا رمز محرمانه‌ای در این مخزن نگهداری نمی‌شود.

## ساختار معماری

`domain` منطق حسابداری خالص Kotlin را در بر دارد؛ `core` شامل Room، DataStore و Design System است؛ `data` مخزن‌ها و ثبت اتمیک اسناد را پیاده‌سازی می‌کند؛ و `app` UI با Compose، ViewModel، ناوبری و WorkManager را می‌سازد. جزییات در [ARCHITECTURE.md](ARCHITECTURE.md) آمده است.

## وضعیت و محدودیت‌ها

نسخه وب، دمو با داده محلی است. Android نسخه آفلاین و تک‌کاربره است؛ همگام‌سازی ابری، چندکاربره و درگاه پرداخت واقعی هنوز ارائه نشده‌اند. استارتر iOS با SwiftUI آماده توسعه است، اما هنوز ذخیره‌سازی پایدار و هم‌ارزی کامل با Android ندارد. برای iOS به macOS و Xcode نیاز است.

## تست‌ها

۳۲ تست دامنه و یکپارچگی در پروژه اجرا شده‌اند: ۲۱ تست موتور حسابداری، ۸ تست تقویم جلالی و ۳ تست Repository/Room. CI نیز تست‌ها و ساخت Debug را در Pull Request و push اجرا می‌کند.

---

حسابیار — حساب‌وکتاب کسب‌وکار شما، ساده و دقیق.
