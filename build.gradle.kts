// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Plugin de Google Services: lee app/google-services.json para conectar la app con Firebase.
    // "apply false" = aquí solo se declara la versión; se activa en app/build.gradle.kts.
    alias(libs.plugins.google.services) apply false
}