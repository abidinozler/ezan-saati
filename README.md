# Ezan Saati

Offline prayer times, qibla compass and home-screen widget for Android.
Android için internetsiz ezan vakitleri, kıble pusulası ve widget.

## Features / Özellikler
- Fully offline prayer time calculation — Diyanet, Muslim World League, ISNA, Egypt, Karachi, Umm al-Qura; Shafi'i / Hanafi asr
- Countdown to the next prayer, on the app, the widget and the lock screen
- Qibla compass with magnetic declination correction
- Per-prayer adhan notifications (on time even in Doze; re-armed after reboot)
- Moon phase and Hijri date
- Languages: Türkçe, English, العربية, Deutsch

## Install / Kurulum
Download the latest `EzanSaati-*.apk` from [Releases](https://github.com/abidinozler/ezan-saati/releases/latest).

For automatic updates, install [Obtainium](https://github.com/ImranR98/Obtainium) and add
`https://github.com/abidinozler/ezan-saati` as an app.

Güncellemeleri otomatik almak için [Obtainium](https://github.com/ImranR98/Obtainium)'u kurup
`https://github.com/abidinozler/ezan-saati` adresini uygulama olarak ekleyin.

## Build
Requires Android Studio (its bundled JBR, Java 25) — Gradle 9.8.2, AGP 9.5.

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug testDebugUnitTest
```

Signing uses `keystore/ezan.keystore`, which is not in the repository.

## License
GPL-3.0 — see [LICENSE](LICENSE).
