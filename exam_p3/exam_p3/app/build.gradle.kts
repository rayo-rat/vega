plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.exam_p3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.exam_p3"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {

    // --- Dependencias base que ya tenías ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ======================================================
    //          DEPENDENCIAS NECESARIAS PARA TU APP
    // ======================================================

    // --- Google Maps ---
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // --- Ubicación GPS ---
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- Anotaciones (FIX del error @NonNull) ---
    implementation("androidx.annotation:annotation:1.7.1")

    // --- Conector MySQL JDBC ---
    implementation("mysql:mysql-connector-java:5.1.49")

    // --- KTX (útil para notificaciones y utilidades AndroidX) ---
    implementation("androidx.core:core-ktx:1.12.0")
}
