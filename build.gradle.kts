import com.inet.gradle.setup.abstracts.*
import com.inet.gradle.setup.abstracts.DesktopStarter.Location.*
import org.apache.tools.ant.taskdefs.condition.Os.*
import org.gradle.api.JavaVersion.*
import org.gradle.crypto.checksum.*
import org.gradle.crypto.checksum.Checksum.Algorithm.*

plugins {
    kotlin("jvm") version "1.5.30"
    id("org.openjfx.javafxplugin") version "0.0.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.beryx.runtime") version "1.12.5"
    id("de.inetsoftware.setupbuilder") version "4.8.7"
    id("org.gradle.crypto.checksum") version "1.2.0"
    id("io.pixeloutlaw.gradle.buildconfigkt") version "2.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

group = "website.video.downloader"
version = "21.4"
description = "Видеозагрузка"

val kotlinxCoroutinesVersion: String by rootProject
val tornadofxVersion: String by rootProject
val kotestVersion: String by rootProject

repositories {
    mavenCentral()
}

application {
    mainClass.set("website.video.downloader.MainKt")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$kotlinxCoroutinesVersion")
    implementation("no.tornado:tornadofx:$tornadofxVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.4")
    implementation("org.zeroturnaround:zt-exec:1.12")
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("io.github.config4k:config4k:0.4.2")
    implementation("org.slf4j:slf4j-jdk14:1.7.32")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.mockk:mockk:1.12.0")
}

javafx {
    version = VERSION_16.toString()
    modules = listOf("javafx.controls", "javafx.swing")
}

buildConfigKt {
    packageName = "website.video.downloader"
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("java.desktop", "java.sql", "java.net.http", "jdk.unsupported", "jdk.httpserver", "jdk.crypto.ec"))

    jpackage {
        skipInstaller = true
        resourceDir = file("setup")
    }
}

tasks {
    compileKotlin {
        dependsOn(ktlintFormat)
        kotlinOptions.jvmTarget = VERSION_16.toString()
    }
    compileTestKotlin { kotlinOptions.jvmTarget = compileKotlin.get().kotlinOptions.jvmTarget }
    wrapper { gradleVersion = "7.2" }
    test { useJUnitPlatform() }

    val imageDir = "${jpackageImage.get().jpackageData.imageOutputDir}/${jpackageImage.get().jpackageData.imageName}"
    val jarName = shadowJar.get().archiveFileName.get()

    setupBuilder {
        vendor = "Dmitry Oshurkov"
        application = if (isFamily(FAMILY_WINDOWS)) "Video Downloader" else project.name
        description = "Download online video to local storage for further playing"
        appIdentifier = project.name
        version = project.version.toString()
        icons = "setup/video-downloader.icns"

        from(imageDir)
        mainClass = project.application.mainClass.get()
        mainJar = if (isFamily(FAMILY_WINDOWS)) "app/$jarName" else "lib/app/$jarName"

        desktopStarter(
            closureOf<DesktopStarter> {
                displayName = "Video Downloader"
                description = "Download online video to local storage for further playing"
                location = StartMenu
                categories = "AudioVideo;AudioVideoEditing;"
                executable = if (isFamily(FAMILY_WINDOWS)) "${project.name}.exe" else "bin/${project.name}"
            }
        )
    }

    deb {
        val chmodX = "chmod +x \$INSTALLATION_ROOT"

        dependsOn(jpackage)
        homepage = "https://video-downloader.website/"
        maintainerEmail = "video-downloader@oshurkov.name"
        depends = "ffmpeg"
        postinst += listOf(
            "$chmodX/bin/${project.name}",
            "$chmodX/lib/runtime/bin/java",
            "$chmodX/lib/runtime/bin/keytool"
        )
    }

    msi {
        dependsOn(jpackage)
        from("setup") {
            include("youtube-dl.exe", "ffmpeg.exe", "msvcr100.dll")
            into("runtime/bin")
        }
    }

    val preparePkg by registering(Copy::class) {
        group = "distribution"
        from("$projectDir/setup/PKGBUILD")
        into("$buildDir/tmp")
    }

    val copyPkg by registering(Copy::class) {
        group = "distribution"
        from("$buildDir/tmp/${project.name}-${project.version}-1-any.pkg.tar.zst")
        into("$buildDir/distributions")
    }

    val pkg by registering(Exec::class) {
        group = "distribution"
        dependsOn(jpackage, preparePkg)
        finalizedBy(copyPkg)
        workingDir = file("$buildDir/tmp")
        commandLine = listOf("makepkg", "-cf")
    }

    val createChecksums by registering(Checksum::class) {
        files = fileTree(setupBuilder.destinationDir)
        outputDir = setupBuilder.destinationDir
        algorithm = SHA256
    }

    @Suppress("UNUSED_VARIABLE")
    val distribution by registering {
        group = "distribution"
        dependsOn(if (isFamily(FAMILY_WINDOWS)) msi else pkg)
        finalizedBy(createChecksums)
    }

    runKtlintCheckOverMainSourceSet { dependsOn(runKtlintFormatOverKotlinScripts, runKtlintFormatOverMainSourceSet) }
    runKtlintCheckOverTestSourceSet { dependsOn(runKtlintFormatOverKotlinScripts, runKtlintFormatOverTestSourceSet) }
    runKtlintCheckOverKotlinScripts { dependsOn(runKtlintFormatOverKotlinScripts) }
    processResources { dependsOn(runKtlintFormatOverKotlinScripts) }
}
