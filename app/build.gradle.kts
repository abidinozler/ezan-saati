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
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("ezan")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("ezan")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
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
