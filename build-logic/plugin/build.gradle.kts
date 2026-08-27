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
}

dependencies {
    implementation("com.android.tools:sdk-common:32.3.2")
}