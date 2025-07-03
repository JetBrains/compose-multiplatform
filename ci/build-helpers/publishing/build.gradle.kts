import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.gradleKotlinDsl

plugins {
    `java`
    `maven-publish`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
    id("com.github.johnrengelman.shadow") apply false
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
}

val embeddedDependencies by configurations.creating { isTransitive = false }
dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    fun embedded(dep: String) {
        compileOnly(dep)
        embeddedDependencies(dep)
    }

    val jacksonVersion = "2.12.5"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-okhttp:3.1.3")
    implementation("org.apache.tika:tika-parsers:1.24.1")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.jetbrains:space-sdk-jvm:2024.3-185883")
    embedded("de.undercouch:gradle-download-task:4.1.2")
}

val shadowJar by tasks.registering(ShadowJar::class) {
    val fromPackage = "de.undercouch"
    val toPackage = "org.jetbrains.compose.internal.publishing.$fromPackage"
    relocate(fromPackage, toPackage)
    archiveClassifier.set("shadow")
    configurations = listOf(embeddedDependencies)
    from(sourceSets["main"]!!.output)
    exclude("META-INF/gradle-plugins/de.undercouch.download.properties")
}

val jar = tasks.named<Jar>("jar") {
    dependsOn(shadowJar)
    from(zipTree(shadowJar.get().archiveFile))
    this.duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
