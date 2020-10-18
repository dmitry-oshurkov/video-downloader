import com.inet.gradle.setup.abstracts.*
import com.inet.gradle.setup.abstracts.DesktopStarter.Location.*
import org.apache.tools.ant.taskdefs.condition.Os.*
import org.gradle.crypto.checksum.*
import org.gradle.crypto.checksum.Checksum.Algorithm.*
import java.time.*

plugins {
    kotlin("jvm") version "1.4.10"
    id("org.openjfx.javafxplugin") version "0.0.9"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.beryx.runtime") version "1.11.4"
    id("de.inetsoftware.setupbuilder") version "4.8.7"
    id("org.gradle.crypto.checksum") version "1.2.0"
}

group = "website.video.downloader"
version = "20.2"
description = "Видеозагрузка"

val tornadofxVersion: String by rootProject

repositories {
    mavenCentral()
}

application {
    mainClassName = "website.video.downloader.MainKt"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("no.tornado:tornadofx:$tornadofxVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    implementation("org.zeroturnaround:zt-exec:1.12")
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("io.github.config4k:config4k:0.4.2")
    implementation("org.slf4j:slf4j-jdk14:1.7.30")

    testImplementation(kotlin("test-junit"))
}

javafx {
    version = "14"
    modules = listOf("javafx.controls", "javafx.swing")
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("java.desktop", "java.sql", "jdk.unsupported", "jdk.httpserver", "jdk.crypto.ec"))
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = compileKotlin.get().kotlinOptions.jvmTarget }
    wrapper { gradleVersion = "6.6.1" }

    val copyDependencies by registering(Copy::class) {
        dependsOn(runtime)
        if (isFamily(FAMILY_WINDOWS))
            from("setup/youtube-dl.exe", "setup/msvcr100.dll", "setup/video-downloader.vbs")
        else
            from("setup/youtube-dl")
        into("${runtime.get().imageDir}/bin")
    }

    val release by registering {
        group = "distribution"
        dependsOn(runtime, copyDependencies)

        doFirst {
            with(file("${runtime.get().imageDir}/release")) {
                appendText("APP_VERSION=\"$version\"\n")
                appendText("APP_BUILT=\"${LocalDateTime.now()}\"\n")
            }

            val extForRemove = if (!isFamily(FAMILY_WINDOWS)) ".bat" else ""
            file("${runtime.get().imageDir}/bin/${project.name}${extForRemove}").delete()
        }
    }

    setupBuilder {
        vendor = "Dmitry Oshurkov"
        application = if (isFamily(FAMILY_WINDOWS)) "Video Downloader" else project.name
        description = "Download online video to local storage for further playing"
        appIdentifier = project.name
        version = project.version.toString()
        icons = "setup/app.icns"

        from(runtime.get().imageDir)
        mainClass = project.application.mainClassName
        mainJar = "lib/${project.name}-${project.version}-all.jar"

        desktopStarter(closureOf<DesktopStarter> {
            displayName = "Видеозагрузка"
            description = "Загрузка видео из Youtube"
            location = StartMenu
            categories = "AudioVideo;AudioVideoEditing;"
            executable = if (isFamily(FAMILY_WINDOWS)) "${project.name}.vbs" else "bin/${project.name}"
            workDir = if (isFamily(FAMILY_WINDOWS)) "bin" else null
        })
    }

    val chmodX = "chmod +x \$INSTALLATION_ROOT/bin"

    deb {
        dependsOn(release)
        homepage = "https://video-downloader.oshurkov.name"
        maintainerEmail = "video-downloader@oshurkov.name"
        depends = "python3"
        postinst += listOf("$chmodX/${project.name}", "$chmodX/java", "$chmodX/keytool", "$chmodX/youtube-dl")
    }

    msi { dependsOn(release) }

    val createChecksums by registering(Checksum::class) {
        files = fileTree(setupBuilder.destinationDir)
        outputDir = setupBuilder.destinationDir
        algorithm = SHA256
    }

    @Suppress("UNUSED_VARIABLE")
    val distribution by registering {
        group = "distribution"
        dependsOn(if (isFamily(FAMILY_WINDOWS)) msi else deb)
        finalizedBy(createChecksums)
    }
}
