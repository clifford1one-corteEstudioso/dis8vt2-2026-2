plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Sin Compose: la app entera se hace con vistas comunes de Android, igual que
// las capas que se dibujan encima de las otras apps.

android {
    namespace = "com.love.yourself"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.love.yourself.fase06"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Solo para la notificacion del modo investigacion (CapturaContinuaService).
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}