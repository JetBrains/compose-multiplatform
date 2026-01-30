import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import de.undercouch.gradle.tasks.download.Download
import kotlin.text.capitalize

plugins {
    kotlin("jvm") version "1.5.21"
    id("org.jetbrains.compose") version "1.0.0-alpha1"
    id("de.undercouch.download") version "4.1.1"
    application
}

val libraryPath = "third_party/java-cef"
val hostOs = System.getProperty("os.name")
val target = when {
    hostOs == "Mac OS X" -> "macos"
    hostOs == "Linux" -> "linux"
    hostOs.startsWith("Win") -> "windows"
    else -> throw Error("Unknown os $hostOs")
}

val cefDownloadZip = run {
    val zipName = "jcef-runtime-$target.zip"
    val zipFile = File("third_party/$zipName")

    tasks.register("downloadCef", Download::class) {
        onlyIf { !zipFile.exists() }
        src("https://bintray.com/jetbrains/skija/download_file?file_path=$zipName")
        dest(zipFile)
        onlyIfModified(true)
    }.map { zipFile }
}

val cefUnZip = run {
    val targetDir = File("third_party/java-cef").apply { mkdirs() }
    tasks.register("unzipCef", Copy::class) {
        from(cefDownloadZip.map { zipTree(it) })
        into(targetDir)
    }.map { targetDir }
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    // temp
    maven("https://packages.jetbrains.team/maven/p/ui/dev")
}

dependencies {
    implementation("org.jetbrains.jcef:jcef-skiko:0.1")
    implementation(compose.desktop.currentOs)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    dependsOn(cefUnZip)
}

application {
    applicationDefaultJvmArgs = listOf("-Djava.library.path=$libraryPath")
    mainClassName = "org.jetbrains.compose.desktop.AppKt"
}
