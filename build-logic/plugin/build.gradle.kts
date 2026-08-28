plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.4.10"
}

gradlePlugin {
    plugins {
        create("dataGeneratorPlugin") {
            id = "world.anhgelus.parismobility"
            implementationClass = "world.anhgelus.parismobility.plugin.DataGeneratorPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

val kotlinxSerialization = "1.11.0"

dependencies {
    implementation("com.android.tools:sdk-common:32.3.2")
    implementation("com.android.tools.build:gradle-api:9.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerialization")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerialization")
}