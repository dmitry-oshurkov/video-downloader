plugins {
    kotlin("jvm") version "1.4.10"
    id("org.openjfx.javafxplugin") version "0.0.9"
    application
}

group = "com.downloader"
version = "20.1"

val tornadofxVersion: String by rootProject

repositories {
    mavenCentral()
}

application {
    mainClassName = "com.downloader.MainKt"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("no.tornado:tornadofx:$tornadofxVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    implementation("org.zeroturnaround:zt-exec:1.12")
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("org.slf4j:slf4j-jdk14:1.7.30")

    testImplementation(kotlin("test-junit"))
}

javafx {
    version = "14"
    modules = listOf("javafx.controls", "javafx.swing")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    wrapper {
        gradleVersion = "6.6.1"
    }
}
