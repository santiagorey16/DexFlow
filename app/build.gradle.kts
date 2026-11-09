plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Firebase / Google Services
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.santiagorey.devflow"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.santiagorey.devflow"

        minSdk = 28
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // =====================================================
    // JETPACK COMPOSE
    // =====================================================

    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )


    // =====================================================
    // ANDROID CORE
    // =====================================================

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )


    // =====================================================
    // NAVIGATION
    // =====================================================

    implementation(
        libs.androidx.navigation.compose
    )


    // =====================================================
    // VIEWMODEL
    // =====================================================

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )


    // =====================================================
    // FIREBASE
    // =====================================================

    implementation(
        platform(libs.firebase.bom)
    )

    implementation(
        libs.firebase.auth
    )


    // =====================================================
    // GOOGLE SIGN-IN
    // CREDENTIAL MANAGER
    // =====================================================

    implementation(
        libs.androidx.credentials
    )

    implementation(
        libs.androidx.credentials.play.services.auth
    )

    implementation(
        libs.googleid
    )


    // =====================================================
    // COIL
    // PARA IMÁGENES DE PERFIL / URLs
    // =====================================================

    implementation(
        libs.coil.compose
    )


    // =====================================================
    // TESTING
    // =====================================================

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}