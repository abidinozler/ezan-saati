# Ezan Saati

Kişisel kullanım için Android ezan vakti ve kıble pusulası uygulaması. Tamamen offline çalışır.

## Özellikler
- Diyanet yöntemiyle (imsak 18°, yatsı 17°, temkin süreleri) offline vakit hesabı
- Sıradaki vakte geri sayım
- Kıble pusulası (manyetik sapma düzeltmeli, hizalanınca titreşim)
- Vakit bazlı ezan bildirimi (Doze modunda da zamanında; yeniden başlatmada kendini kurar)
- GPS veya 81 il listesinden şehir seçimi
- Diyanet takvimine göre dakika bazlı ince ayar

## Kurulum
1. [Android Studio](https://developer.android.com/studio) indirip kurun (Android SDK'yı kendisi indirir).
2. Android Studio → **Open** → bu klasörü (`EZAN_SAATI`) seçin. İlk açılışta Gradle senkronizasyonu birkaç dakika sürer.
3. Telefonda **Geliştirici seçenekleri** → **USB hata ayıklama**'yı açın
   (Ayarlar → Telefon hakkında → Yapı numarasına 7 kez dokunun).
4. Telefonu USB ile bağlayın, Android Studio'da üstte cihazı seçip **▶ Run**'a basın.

Testler: `app/src/test/.../CalculatorTest.kt` dosyasında sağ tık → **Run**.

## Proje yapısı
```
app/src/main/java/com/ezansaati/app/
├── prayer/        Vakit hesabı (PrayerCalculator), Prayer enum
├── qibla/         Kıble açısı ve uzaklık
├── data/          Ayarlar (SharedPreferences), 81 il listesi
├── location/      GPS konumu (FusedLocationProvider)
├── notification/  Alarm zinciri, bildirimler, sistem olay alıcıları
├── sensor/        Pusula (rotation vector sensörü)
├── ui/            Compose ekranları: Vakitler, Kıble, Ayarlar
├── MainActivity.kt
└── MainViewModel.kt
```
