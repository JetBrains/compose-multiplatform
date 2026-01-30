import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
}

// Write kotlin.version into a file, so it can be read by karma-test-runner patch (see test-utils/conf)
File(projectDir.resolve("build"), "kotlin.version").apply {
    this.parentFile.mkdirs()
    val kotlinVersion = extra["kotlin.version"].toString()
    println("Writing kotlin.version=$kotlinVersion into $absolutePath")
    createNewFile()
    writeText(kotlinVersion)
}
