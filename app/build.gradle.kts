import com.android.ide.common.vectordrawable.Svg2Vector
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
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

        buildConfigField("String", "PRIM_TOKEN", "\"${get("PRIM_TOKEN")}\"")
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

    // http requests
    implementation(libs.bundles.ktor)
}

fun request(
    url: String,
    outputPath: String,
    accept: String = "application/json",
    vararg headers: String
) {
    try {
        providers.exec {
            val args = mutableListOf(
                "curl", "-H", "Accept: $accept",
                "--fail-with-body",
                "-v",
                "-o", outputPath,
            )
            args.addAll(headers.flatMap { listOf("-H", it) })
            args.add(url)
            commandLine(args)
            args.forEach { logger.debug(it) }
        }.standardError.asText.get().let { logger.info(it) }
    } catch (e: ProcessExecutionException) {
        logger.error(e.message)
        logger.info("url: $url")
        throw e.cause ?: e
    }
}

tasks.register("updateLines") {
    description = "Update lines data"
    dependsOn(downloadLines, updateLinesSvg)
}

val downloadLines = tasks.register("downloadLinesJson") {
    description = "Download JSON describing lines"

    request(
        url = "https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/referentiel-des-lignes/exports/json",
        outputPath = file("src/main/res/raw/lines.json").absolutePath
    )
    logger.info("Lines data downloaded")
}

val downloadStops = tasks.register("downloadStopsJson") {
    description = "Download JSON describing stops"

    request(
        url = "https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/emplacement-des-gares-idf/exports/json",
        outputPath = file("src/main/res/raw/stops.json").absolutePath
    )
    logger.info("Stops data downloaded")
}

val updateLinesSvg = tasks.register("updateLinesSvg") {
    description = "Update lines' SVG"

    mkdir("build/intermediates/res/lines/")

    val base =
        "https://prim.iledefrance-mobilites.fr/marketplace/ilico/getIcon/sprite?usage=signage_spaces&format=zip_svg&style=colored&getAll=true"
    listOf("metro", "rer", "tram", "train").forEach { mode ->
        val zip = file("build/intermediates/res/lines/$mode.zip")
        request(
            "$base&transportMode=$mode",
            zip.absolutePath,
            "application/zip",
            "apiKey: ${get("PRIM_TOKEN")}"
        )
        logger.info("{}'s SVGs downloaded", mode)
        zipTree(zip).forEach {
            val name = it.name.split(":")[2].split(".")[0].lowercase()
            val dest = file("src/main/res/drawable/line_$name.xml")
            val err = Svg2Vector.parseSvgToXml(it.toPath(), dest.outputStream())
            if (!err.isEmpty()) throw Exception(err)
        }
    }
}

val downloadPlans = tasks.register("downloadPlans") {
    description = "Download used plans"

    request(
        url = "https://www.ratp.fr/sites/default/files/plans-lignes/Plans-essentiels/Plan-Metro.1772790495.png",
        outputPath = file("src/main/res/drawable/plan_metro.png").absolutePath
    )
    logger.info("Plans downloaded")
}