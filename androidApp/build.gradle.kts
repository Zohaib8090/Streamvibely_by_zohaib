plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "app.streamvibely"
    compileSdk {
            version = release(37) { minorApiLevel = 2 }
        }

    defaultConfig {
        applicationId = "com.zohaibbaig.streamvibely"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { buildConfig = true }
    // Free Jamendo client ID: put jamendo.clientId=... in local.properties (or set JAMENDO_CLIENT_ID).
    val jamendoId = rootProject.file("local.properties").takeIf { it.exists() }
        ?.readLines()?.firstOrNull { it.startsWith("jamendo.clientId=") }?.substringAfter("=")?.trim()
        ?: System.getenv("JAMENDO_CLIENT_ID").orEmpty()
    defaultConfig { buildConfigField("String", "JAMENDO_CLIENT_ID", "\"$jamendoId\"") }

    // Release signing comes from env vars set by the GitHub release workflow. Without them (local builds)
    // the release APK is signed with the debug key so it still installs.
    val keystorePath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.datastore.preferences)
    implementation(libs.play.services.auth)
    implementation(libs.documentfile)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.okhttp)
    implementation(libs.newpipe.extractor)
    runtimeOnly(libs.innertubex)
    runtimeOnly(libs.ktor.client.okhttp)
    runtimeOnly(libs.ktor.client.cio)
    testImplementation("junit:junit:4.13.2")
}
