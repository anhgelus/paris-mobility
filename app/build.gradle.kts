import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

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

tasks.register("updateLines") {
    description = "Update JSON storing lines"

    val token = System.getenv("PRISM_TOKEN")

    val client = HttpClientBuilder.create().build()
    val groups = mapOf(
        "metro" to Pair("metro", null),
        "tram" to Pair("tram", null),
        "rer" to Pair("rail", "local"),
        "transilien" to Pair("rail", "suburbanRailway"),
        "bus" to Pair("bus", "regionalBus"),
    )
    val base =
        "https://prim.iledefrance-mobilites.fr/marketplace/ilico/getData?method=getlc&format=json"
    groups.forEach { (file, data) ->
        val req = HttpGet(
            "$base&TransportMode=${data.first}" +
                    if (data.second != null) "&TransportSubmode=${data.second}"
                    else ""
        )
        req.addHeader("Accept", "application/json")
        req.addHeader("apiKey", token)
        val resp = client.execute(req)
        val content = resp.entity.content.bufferedReader().use { it.readText() }
        if (resp.statusLine.statusCode != 200) {
            throw Exception(
                "invalid status code ${resp.statusLine.statusCode}, content $content"
            )
        }
        val f = file("src/main/res/raw/$file.json")
        f.writeText(content)
        resp.close()
    }
    client.close()
}