import org.jetbrains.kotlin.konan.properties.loadProperties
import world.anhgelus.parismobility.plugin.DownloadLinesSvgTask

plugins {
    id("world.anhgelus.parismobility")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

val keys = loadProperties("keys.properties")
val keystore = loadProperties(rootProject.file("keystore.properties").path)

fun get(k: String): String {
    return keys[k] as String? ?: System.getenv(k)
}

android {
    namespace = "world.anhgelus.parismobility"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "world.anhgelus.parismobility"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SERVER_HOSTNAME", "\"${get("SERVER_HOSTNAME")}\"")
        buildConfigField("int", "SERVER_PORT", get("SERVER_PORT"))
    }

    signingConfigs {
        create("release") {
            keyAlias = keystore["keyAlias"] as String
            keyPassword = keystore["keyPassword"] as String
            storeFile = file(keystore["storeFile"] as String)
            storePassword = keystore["storePassword"] as String
        }
    }

    buildTypes {
        release {
            optimization { enable = true }
            signingConfig = signingConfigs["release"]
        }
        debug {
            signingConfig = signingConfigs["debug"]
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.bundles.serialization)

    // navbar
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    // datastores
    implementation(libs.androidx.datastore.preferences)

    // glance
    implementation(libs.bundles.glance)
}

tasks.named<DownloadLinesSvgTask>("downloadData").configure {
    modes = listOf("metro", "rer", "tram", "train")
    token = get("PRIM_TOKEN")
}