import org.jetbrains.kotlin.konan.properties.loadProperties
import world.anhgelus.parismobility.plugin.DownloadLinesSvgTask
import world.anhgelus.parismobility.plugin.DownloadLinesTask
import world.anhgelus.parismobility.plugin.DownloadPlansTask
import world.anhgelus.parismobility.plugin.DownloadStopsTask

plugins {
    id("world.anhgelus.parismobility")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

val keys = loadProperties("keys.properties")

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

    buildTypes {
        release {
            optimization {
                enable = false
            }
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
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
}

tasks.named<DownloadPlansTask>("downloadPlans").configure {
    target = file("src/main/res/drawable/plan_metro.png").absolutePath
}

tasks.named<DownloadLinesSvgTask>("downloadLinesSvg").configure {
    target = file("src/main/res/drawable").absolutePath
    modes = listOf("metro", "rer", "tram", "train")
    token = get("PRIM_TOKEN")
}

tasks.named<DownloadLinesTask>("downloadLines").configure {
    target = file("src/main/res/raw/lines.json").absolutePath
}

tasks.named<DownloadStopsTask>("downloadStops").configure {
    target = file("src/main/res/raw/stops.json").absolutePath
}