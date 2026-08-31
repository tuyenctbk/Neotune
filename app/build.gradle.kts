import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.gms.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    lint {
        disable += setOf("MissingTranslation", "StringFormatInvalid")
    }

    namespace = "com.easeaudio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neotune.radio"
        minSdk = 24
        targetSdk = 37
        versionCode = 45
        versionName = "4.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val storeFilePath = localProperties.getProperty("RELEASE_STORE_FILE") ?: "common_release_key.jks"
            storeFile = rootProject.file(storeFilePath)
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: "dpadhero123"
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: "dpad_hero_alias"
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: "dpadhero123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.window.size)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Media3 ExoPlayer — all stream-format extensions must be declared explicitly;
    // DefaultMediaSourceFactory resolves them via reflection at runtime and will throw
    // ClassNotFoundException if the module is absent from the APK.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)   // HLS (.m3u8) streams
    implementation(libs.androidx.media3.exoplayer.dash)  // DASH (.mpd) streams
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    // Coil Image Loading
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Firebase Remote Config, Analytics, Crashlytics & Perf
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation(libs.firebase.crashlytics)

    debugImplementation(libs.androidx.ui.tooling)
}
