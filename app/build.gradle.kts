import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Sürüm kodu: 2026-01-01'den bu yana geçen dakika. Her derlemede artar, böylece telefon yeni sürümü tanır.
val buildVersionCode: Int = ((System.currentTimeMillis() / 1000 - 1_767_225_600L) / 60).toInt()

android {
    namespace = "com.ezansaati.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ezansaati.app"
        minSdk = 26
        targetSdk = 36
        versionCode = buildVersionCode
        versionName = "1.1.$buildVersionCode"
    }

    // Telefondaki sürümün üzerine güncelleme kurulabilmesi için her derleme aynı anahtarla imzalanır.
    // keystore/ezan.keystore ilk kurulan APK'yı imzalayan debug anahtarının kopyasıdır; kaybedilmemeli
    // (OneDrive'da yedekli, git'e girmez).
    signingConfigs {
        create("ezan") {
            storeFile = rootProject.file("keystore/ezan.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Play Console'a yüklenen paketin anahtarı (Play, uygulamayı kendi anahtarıyla yeniden imzalar).
        // Bilgiler keystore/upload.properties'te; dosya yoksa play sürümü de ezan anahtarıyla imzalanır.
        val uploadProps = rootProject.file("keystore/upload.properties")
        if (uploadProps.exists()) {
            val props = Properties().apply { uploadProps.inputStream().use(::load) }
            create("upload") {
                storeFile = rootProject.file("keystore/" + props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    // Varsayılan anahtar; play sürümü kendi flavor'ında upload anahtarını seçer.
    // (Build type'ta signingConfig verilirse flavor'dakini ezdiği için orada verilmez.)
    defaultConfig.signingConfig = signingConfigs.getByName("ezan")

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("ezan")
        }
        release {
            isMinifyEnabled = false
        }
    }

    // github: APK olarak GitHub'da yayınlanan, telefona elle/Obtainium ile kurulan sürüm.
    // play: Play Store kurallarına uygun sürüm (izin farkları src/play/AndroidManifest.xml'de).
    flavorDimensions += "dagitim"
    productFlavors {
        create("github") {
            dimension = "dagitim"
            buildConfigField("boolean", "PLAY_STORE", "false")
        }
        create("play") {
            dimension = "dagitim"
            // com.ezansaati.app Play'de başka bir geliştiricinin uygulamasına ait.
            applicationId = "com.abidinozler.ezansaati"
            buildConfigField("boolean", "PLAY_STORE", "true")
            signingConfigs.findByName("upload")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")

    testImplementation("junit:junit:4.13.2")
}
