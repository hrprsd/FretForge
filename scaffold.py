import os

base_dir = r"C:\Users\Haraprasad\.gemini\antigravity\Projects\FretForge"
dirs = [
    "app/src/main/java/com/fretforge/data",
    "app/src/main/java/com/fretforge/repository",
    "app/src/main/java/com/fretforge/ui/home",
    "app/src/main/java/com/fretforge/ui/group",
    "app/src/main/java/com/fretforge/ui/practice",
    "app/src/main/java/com/fretforge/ui/summary",
    "app/src/main/java/com/fretforge/ui/history",
    "app/src/main/java/com/fretforge/ui/theme",
    "app/src/main/java/com/fretforge/navigation",
    "app/src/main/res/values",
    "app/src/main/res/raw",
    "app/src/main/res/drawable",
    "app/src/main/assets/images",
    "app/src/main/res/mipmap-hdpi",
    "app/src/main/res/mipmap-mdpi",
    "app/src/main/res/mipmap-xhdpi",
    "app/src/main/res/mipmap-xxhdpi",
    "app/src/main/res/mipmap-xxxhdpi"
]

for d in dirs:
    os.makedirs(os.path.join(base_dir, d), exist_ok=True)

# settings.gradle.kts
open(os.path.join(base_dir, "settings.gradle.kts"), "w").write('''pluginManagement {
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
rootProject.name = "FretForge"
include(":app")
''')

# build.gradle.kts
open(os.path.join(base_dir, "build.gradle.kts"), "w").write('''buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
    }
}
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
''')

# gradle.properties
open(os.path.join(base_dir, "gradle.properties"), "w").write('''org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
''')

# app/build.gradle.kts
open(os.path.join(base_dir, "app/build.gradle.kts"), "w").write('''plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.fretforge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fretforge"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // GSON
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
''')

# AndroidManifest.xml
open(os.path.join(base_dir, "app/src/main/AndroidManifest.xml"), "w").write('''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.fretforge">

    <application
        android:name=".FretForgeApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FretForge">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.FretForge">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
''')

# strings.xml
open(os.path.join(base_dir, "app/src/main/res/values/strings.xml"), "w").write('''<resources>
    <string name="app_name">Fret Forge</string>
</resources>
''')

# themes.xml
open(os.path.join(base_dir, "app/src/main/res/values/themes.xml"), "w").write('''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FretForge" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
''')

# Application Class
open(os.path.join(base_dir, "app/src/main/java/com/fretforge/FretForgeApp.kt"), "w").write('''package com.fretforge

import android.app.Application

class FretForgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
''')
print("Scaffolding complete.")
