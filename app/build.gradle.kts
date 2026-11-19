import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val keystoreProps = Properties().apply {
    // keystore/keystore.properties (git-ignored), or keystore.properties in the project root.
    val f = listOf("keystore/keystore.properties", "keystore.properties").map { rootProject.file(it) }.firstOrNull { it.exists() }
    f?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.htr.collagemaker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.htr.collagemaker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
        // Supabase, read-only (design/backend-handoff.md). Only the publishable key may ship in the app —
        // never the secret (service-role) key. Override in local.properties with supabase.url / supabase.anonKey;
        // set both to blank there to use the built-in catalogue only.
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("supabase.url", "https://eflwdrrptirhknkusxdm.supabase.co")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("supabase.anonKey", "sb_publishable_LKN8xjH4CNssWRCChpusKQ_7pzOt7zs")}\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // R8 makes Compose much faster (and the APK smaller); profileinstaller below ships the
            // Compose baseline profiles so the first launches are smooth even on sideloaded installs.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    // Firebase (app/google-services.json), versions from the BoM: Analytics, Crashlytics,
    // Firestore (issue reports, device tokens), Cloud Messaging (campaigns) and Remote Config (force update).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.review.ktx)          // In-app reviews
    implementation(libs.androidx.work.runtime.ktx) // Scheduled local reminders
    debugImplementation(libs.androidx.ui.tooling)
}
