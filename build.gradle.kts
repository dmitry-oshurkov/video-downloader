import org.apache.tools.ant.taskdefs.condition.Os.*
import java.time.*

plugins {
    kotlin("jvm") version "1.4.10"
    id("org.openjfx.javafxplugin") version "0.0.9"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.beryx.runtime") version "1.11.4"
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

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("java.naming", "java.desktop", "jdk.unsupported", "jdk.httpserver", "jdk.crypto.ec"))
    imageDir.set(file("$buildDir/release"))
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = compileKotlin.get().kotlinOptions.jvmTarget }
    wrapper { gradleVersion = "6.6.1" }

    @Suppress("UNUSED_VARIABLE") val release by registering {
        group = "distribution"
        dependsOn(runtime)

        doFirst {
            with(file("${runtime.get().imageDir}/release")) {
                appendText("APP_VERSION=\"$version\"\n")
                appendText("APP_BUILT=\"${LocalDateTime.now()}\"\n")
            }

            val extForRemove = if (!isFamily(FAMILY_WINDOWS)) ".bat" else ""

            file("${runtime.get().imageDir}/bin/${project.name}${extForRemove}").delete()
        }
    }
}
