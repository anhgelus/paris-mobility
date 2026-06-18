import com.android.ide.common.vectordrawable.Svg2Vector

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
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
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // navbar
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
}

fun getPrismToken(): String {
    return System.getenv("PRISM_TOKEN")!!
}

tasks.register("updateLines") {
    description = "Update lines data"
    dependsOn(":updateLinesJson")
    dependsOn(":updateLinesSvg")
}

tasks.register("updateLinesJson") {
    description = "Update JSON storing lines"

    val groups = mapOf(
        "metro" to Pair("metro", null),
        "tram" to Pair("tram", null),
        "rer" to Pair("rail", "local"),
        "transilien" to Pair("rail", "suburbanRailway"),
        "bus" to Pair("bus", "regionalBus"),
    )
    val base =
        "https://prim.iledefrance-mobilites.fr/marketplace/ilico/getData?method=getlc&format=json"
    groups.forEach { (fileName, data) ->
        providers.exec {
            commandLine(
                "curl", "-H", "Accept: application/zip",
                "-H", "apiKey: ${getPrismToken()}",
                "-X", "GET",
                "-o", file("src/main/res/raw/$fileName.json").absolutePath,
                "$base&TransportMode=${data.first}" +
                        if (data.second != null) "&TransportSubmode=${data.second}"
                        else ""
            )
        }
        logger.info("Downloaded {}'s JSON data", fileName)
    }
}

tasks.register("updateLinesSvg") {
    description = "Update JSON storing lines"

    mkdir("build/intermediates/res/lines/")

    val base =
        "https://prim.iledefrance-mobilites.fr/marketplace/ilico/getIcon/sprite?usage=signage_spaces&format=zip_svg&style=colored&getAll=true"
    listOf("metro", "rer", "tram", "train").forEach { mode ->
        val zip = file("build/intermediates/res/lines/$mode.zip")
        providers.exec {
            commandLine(
                "curl",
                "-H", "Accept: application/zip",
                "-H", "apiKey: ${getPrismToken()}",
                "-X", "GET",
                "-o", zip.absolutePath,
                "$base&transportMode=$mode"
            )
        }
        logger.info("Downloaded {}'s SVG", mode)
        zipTree(zip).forEach {
            val name = it.name.split(":")[2].split(".")[0].lowercase()
            val dest = file("src/main/res/drawable/${mode}_$name.xml")
            val err = Svg2Vector.parseSvgToXml(it.toPath(), dest.outputStream())
            if (!err.isEmpty()) throw Exception(err)
        }
    }
}