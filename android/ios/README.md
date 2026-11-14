# Hesabyar iOS Starter

اسکلت SwiftUI حسابیار برای ادامه مسیر iOS.

## اجرا

```bash
brew install xcodegen   # یک‌بار
cd ios
xcodegen generate
open Hesabyar.xcodeproj
```

سپس در Xcode تیم توسعه را انتخاب و روی شبیه‌ساز اجرا کنید. حداقل نسخه iOS: 16.0.

## چیست و چه نیست

- **هست**: رابط فارسی RTL، تقویم جلالی، تومان، داشبورد، فروش، هزینه، تراکنش‌ها، کالاها، اشخاص و گزارش با Swift Charts.
- **نیست**: هنوز دیتابیس پایدار، همگام‌سازی، پشتیبان‌گیری و هم‌ارزی کامل با اندروید ندارد. داده‌ها در حافظه و نمونه‌اند.

راهنمای کامل در `../docs/IOS_GUIDE.md` است.

## ساختار

- `Hesabyar/Core`: مدل‌ها، فرمت‌ها، طراحی و داده نمونه.
- `Hesabyar/Features`: صفحه‌های اصلی.
- `Hesabyar/Resources/Fonts`: وزیرمتن.
- `project.yml`: تنظیمات XcodeGen.
