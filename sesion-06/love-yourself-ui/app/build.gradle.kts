plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// App solo de diseno: sin Compose, sin permisos, sin servicios. Usa vistas
// comunes de Android, igual que los overlays de la app real, para que lo que
// se ve aca sea lo que se ve alla.
android {
    namespace = "com.love.yourself"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.love.yourself.ui"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
