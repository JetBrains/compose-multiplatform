import org.gradle.kotlin.dsl.gradleKotlinDsl

plugins {
    `java`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.24"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    val jacksonVersion = "2.12.5"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("io.ktor:ktor-client-okhttp:1.6.4")
    implementation("org.apache.tika:tika-parsers:1.24.1")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.jetbrains:space-sdk-jvm:83821-beta")
    implementation("de.undercouch:gradle-download-task:4.1.2")
}
