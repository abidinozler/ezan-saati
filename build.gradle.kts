plugins {
    id("com.android.application") version "9.5.0" apply false
    // AGP 9 Kotlin desteğini kendisi içerir; bu satır yalnızca Kotlin sürümünü sabitler.
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
