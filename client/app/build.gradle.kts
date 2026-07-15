import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}


val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    val stream = localPropertiesFile.inputStream()
    properties.load(stream)
    stream.close()
}

// Also try loading from .env if GOOGLE_CLIENT_ID is not in local.properties
if (properties.getProperty("GOOGLE_CLIENT_ID") == null) {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        val stream = envFile.inputStream()
        val envProperties = Properties()
        envProperties.load(stream)
        stream.close()
        val envGoogleClientId = envProperties.getProperty("GOOGLE_CLIENT_ID")
        if (envGoogleClientId != null) {
            properties.setProperty("GOOGLE_CLIENT_ID", envGoogleClientId)
        }
    }
}

val googleClientId = properties.getProperty("GOOGLE_CLIENT_ID") ?: ""

android {
    namespace = "com.example.movie_booking_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.movie_booking_app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.services.auth)

    // UI XML
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.shimmer)
    implementation(libs.glide)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    ksp(libs.glide.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    // Enables .await() on Firebase / Google Play Services Task<T>
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Image Loading
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")

    // Unit Test
    testImplementation(libs.junit)

    // YouTube Player SDK
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")

    // Android Test
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
