# یادآور من (Yadavar Man)

اپ اندروید یادآور فارسی با دستیار هوش مصنوعی، ساخته‌شده با Capacitor.

## امکانات
- یادآورها با تقویم شمسی و آلارم تمام‌صفحه
- دکمه‌های اعلان (انجام شد / تعویق)
- دستیار هوش مصنوعی (کلید API را خود کاربر در تنظیمات وارد می‌کند)
- ویجت صفحه اصلی با پنجره شناور چت و ورودی صوتی (fa-IR)
- میانبرهای آیکون برنامه

## ساختار
- `www/index.html` — کل رابط کاربری و منطق برنامه
- `android/` — پروژه بومی اندروید (AlarmPlugin، AlarmService، QuickActivity، QuickWidget)

## ساخت
```bash
npm install
npx cap copy android
cd android
./gradlew assembleDebug
```
پیش‌نیاز: JDK 17+ و Android SDK 35.

## English
Persian reminder app for Android with an AI assistant, full-screen alarms, and a home-screen quick-chat widget with voice input. Built with Capacitor.
