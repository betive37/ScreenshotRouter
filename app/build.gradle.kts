import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun detectCompileSdk(): Int {
    val configured = providers.gradleProperty("android.compileSdk").orNull?.toIntOrNull()
    if (configured != null) return configured

    val sdkRoot = providers.environmentVariable("ANDROID_HOME").orNull
        ?: providers.environmentVariable("ANDROID_SDK_ROOT").orNull
    val detected = sdkRoot
        ?.let { File(it, "platforms") }
        ?.listFiles()
        ?.mapNotNull { it.name.removePrefix("android-").toIntOrNull() }
        ?.maxOrNull()

    return detected ?: 35
}

val detectedCompileSdk = detectCompileSdk()
val configuredTargetSdk = providers.gradleProperty("android.targetSdk").orNull?.toIntOrNull()
val resolvedTargetSdk = configuredTargetSdk ?: minOf(detectedCompileSdk, 35)

android {
    namespace = "com.example.screenshotrouter"
    compileSdk = detectedCompileSdk

    defaultConfig {
        applicationId = "com.example.screenshotrouter"
        minSdk = 26
        targetSdk = resolvedTargetSdk
        versionCode = 1
        versionName = "0.0.1"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
