# راهنمای نسخه iOS

پوشه `ios/` یک استارتر SwiftUI برای حسابیار است تا مسیر ساخت نسخه iOS از همین ابتدا باز باشد. این بخش در محیط فعلی (Linux) کامپایل نشده است؛ برای اجرا به macOS و Xcode نیاز دارد.

## پیش‌نیازها

- macOS 14 یا جدیدتر
- Xcode 15 یا جدیدتر
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`)
- حساب Apple Developer برای اجرا روی دستگاه واقعی و TestFlight

## ساخت پروژه

```bash
cd ios
xcodegen generate
open Hesabyar.xcodeproj
```

در Xcode:

1. Target `Hesabyar` را انتخاب کنید.
2. در Signing & Capabilities تیم توسعه خود را انتخاب کنید.
3. شبیه‌ساز iPhone را اجرا کنید.

نسخه deployment target برابر iOS 16.0 است. فونت‌های وزیرمتن در `Hesabyar/Resources/Fonts` و در `project.yml` به `UIAppFonts` اضافه شده‌اند.

## ساختار

```
ios/
├── project.yml                 مشخصات پروژه XcodeGen
├── Hesabyar/
│   ├── HesabyarApp.swift       نقطه ورود و TabView
│   ├── Core/
│   │   ├── Models.swift        مدل‌های دامنه و AppStore
│   │   ├── Formatters.swift    فرمت فارسی/جلالی/تومان
│   │   ├── DesignSystem.swift  رنگ، فونت، کامپوننت‌ها
│   │   └── SampleData.swift    داده نمونه و ذخیره‌سازی در حافظه
│   ├── Features/
│   │   ├── DashboardView.swift
│   │   ├── SaleSheet.swift     فرم فروش و هزینه
│   │   ├── TransactionsView.swift
│   │   ├── ProductsView.swift
│   │   ├── ContactsView.swift
│   │   └── ReportsView.swift   نمودار با Swift Charts
│   ├── Assets.xcassets/        آیکون ۱۰۲۴
│   └── Resources/Fonts/
```

## وضعیت فعلی

- رابط فارسی RTL، تقویم جلالی و تومان پیاده‌سازی شده است.
- صفحه‌های داشبورد، فروش، هزینه، تراکنش‌ها، کالاها، اشخاص و گزارش وجود دارند.
- داده نمونه در حافظه نگهداری می‌شود؛ فروش و هزینه از طریق `AppStore` روی آرایه‌ها اثر می‌گذارند.
- ذخیره‌سازی پایدار، پشتیبان‌گیری، نوتیفیکیشن و هم‌ارزی کامل با اندروید هنوز پیاده‌سازی نشده است.

## گام‌های بعدی برای هم‌ارزی

1. **مدل داده پایدار**: SwiftData با موجودیت‌های Business, Contact, Product, Account, Document, DocumentLine, Transaction, Reminder.
2. **موتور حسابداری مشترک**: گزینه بهتر، استخراج `:domain` به Kotlin Multiplatform و فراخوانی از Swift؛ گزینه سریع‌تر، پورت قواعد در Swift و تست دوطرفه.
3. **پشتیبان و خروجی**: فرمت JSON مشترک با اندروید تا انتقال داده بین دو پلتفرم ممکن شود.
4. **اعلان‌ها**: UserNotifications + BackgroundTasks برای یادآوری سررسید.
5. **آیکون و اسکرین‌شات**: آیکون ۱۰۲۴ موجود است؛ برای App Store اسکرین‌شات‌های ۶٫۷ و ۶٫۵ اینچ اضافه شود.
6. **TestFlight**: تنظیم bundle id، امضای خودکار، آپلود با Xcode Organizer یا `xcodebuild -exportArchive`.

## نکته حقوقی

نام و برند «حسابیار» را قبل از انتشار در App Store از نظر تشابه نام و علامت تجاری بررسی کنید. سیاست حریم خصوصی iOS باید عدم ارسال داده و ذخیره محلی را شفاف توضیح دهد.
