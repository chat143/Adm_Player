import java.io.File

plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

// ---- সব ফাইল রুটে রাখার জন্য: সোর্স ও আইকন বিল্ডের সময় অটো তৈরি হবে ----
val genKotlin = File(projectDir, "build/gen/kotlin").apply { mkdirs() }
File(projectDir, "MainActivity.kt").copyTo(File(genKotlin, "MainActivity.kt"), overwrite = true)

val genRes = File(projectDir, "build/gen/res")
File(genRes, "mipmap-anydpi-v26").mkdirs()
File(genRes, "drawable").mkdirs()

File(genRes, "mipmap-anydpi-v26/ic_launcher.xml").writeText(
    """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_bg" />
    <foreground android:drawable="@drawable/ic_fg" />
</adaptive-icon>
"""
)

File(genRes, "drawable/ic_bg.xml").writeText(
    """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0" android:startY="0"
                android:endX="108" android:endY="108"
                android:startColor="#FFFFFFFF"
                android:endColor="#FFC6CBD3" />
        </aapt:attr>
    </path>
</vector>
"""
)

File(genRes, "drawable/ic_fg.xml").writeText(
    """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path
        android:pathData="M44,31 H64 A14,14 0 0 1 78,45 V65 A14,14 0 0 1 64,79 H44 A14,14 0 0 1 30,65 V45 A14,14 0 0 1 44,31 Z"
        android:strokeColor="#FFFFFFFF" android:strokeWidth="1.5">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="54" android:startY="31"
                android:endX="54" android:endY="79"
                android:startColor="#FFFFFFFF"
                android:endColor="#FFE6E9EE" />
        </aapt:attr>
    </path>
    <path
        android:fillColor="#FF111111"
        android:pathData="M48,43 L48,67 L68,55 Z"
        android:strokeColor="#FF111111" android:strokeWidth="3" android:strokeLineJoin="round" />
</vector>
"""
)

android {
    namespace = "com.adm.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.adm.player"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    sourceSets.getByName("main") {
        manifest.srcFile("AndroidManifest.xml")
        java.setSrcDirs(listOf("build/gen/kotlin"))
        res.setSrcDirs(listOf("build/gen/res"))
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
}
