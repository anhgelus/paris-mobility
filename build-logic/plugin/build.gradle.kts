plugins {
    `kotlin-dsl`
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
    gradlePluginPortal()
}

val ktorVersion = "3.5.2"

dependencies {
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-cio:${ktorVersion}")
    implementation("com.android.tools:sdk-common:32.3.2")
}