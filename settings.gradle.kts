pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EzanSaati"
include(":app")

// Proje OneDrive içinde; OneDrive derleme çıktılarını senkronize ederken kilitleyip derlemeyi bozuyor.
// Bu yüzden build klasörleri OneDrive dışında tutulur.
val externalBuildRoot = File(
    System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir"),
    "EzanSaatiBuild",
)
gradle.beforeProject {
    layout.buildDirectory.set(File(externalBuildRoot, if (this == rootProject) "root" else name))
}
