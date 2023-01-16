import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

gradlePluginConfig {
    pluginId = "org.jetbrains.compose"
    implementationClass = "org.jetbrains.compose.ComposePlugin"
    pluginPortalTags = listOf("ui-framework")
}

mavenPublicationConfig {
    displayName = "JetBrains Compose Gradle Plugin"
    description = "JetBrains Compose Gradle plugin for easy configuration"
    artifactId = "compose-gradle-plugin"
}

val buildConfigDir
    get() = project.layout.buildDirectory.dir("generated/buildconfig")
val buildConfig = tasks.register("buildConfig", GenerateBuildConfig::class.java) {
    classFqName.set("org.jetbrains.compose.ComposeBuildConfig")
    generatedOutputDir.set(buildConfigDir)
    fieldsToGenerate.put("composeVersion", BuildProperties.composeVersion(project))
    fieldsToGenerate.put("composeGradlePluginVersion", BuildProperties.deployVersion(project))
}
tasks.named("compileKotlin") {
    dependsOn(buildConfig)
}
sourceSets.main.configure {
    java.srcDir(buildConfigDir)
}

val embeddedDependencies by configurations.creating {
    isTransitive = false
}

dependencies {
    // By default, Gradle resolves plugins only via Gradle Plugin Portal.
    // To avoid declaring an additional repo, all dependencies must:
    // 1. Either be provided by Gradle at runtime (e.g. gradleApi());
    // 2. Or be included and optionally relocated.
    // Use `embedded` helper to include a dependency.
    fun embedded(dep: Any) {
        compileOnly(dep)
        testCompileOnly(dep)
        embeddedDependencies(dep)
    }

    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(kotlin("native-utils"))

    testImplementation(gradleTestKit())
    testImplementation(kotlin("gradle-plugin-api"))

    // include relocated download task to avoid potential runtime conflicts
    embedded("de.undercouch:gradle-download-task:5.3.0")

    embedded("org.jetbrains.kotlinx:kotlinx-serialization-json:${BuildProperties.serializationVersion}")
    embedded("org.jetbrains.kotlinx:kotlinx-serialization-core:${BuildProperties.serializationVersion}")
    embedded("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${BuildProperties.serializationVersion}")
    embedded(project(":preview-rpc"))
}

val shadow = tasks.named<ShadowJar>("shadowJar") {
    val fromPackage = "de.undercouch"
    val toPackage = "org.jetbrains.compose.$fromPackage"
    relocate(fromPackage, toPackage)
    archiveClassifier.set("shadow")
    configurations = listOf(embeddedDependencies)
    exclude("META-INF/gradle-plugins/de.undercouch.download.properties")
    exclude("META-INF/versions/**")
}

val jar = tasks.named<Jar>("jar") {
    dependsOn(shadow)
    from(zipTree(shadow.get().archiveFile))
    this.duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val supportedGradleVersions = project.property("compose.tests.gradle.versions")
    .toString().split(",")
    .map { it.trim() }
    .map { GradleVersion.version(it) }

val gradleTestsPattern = "org.jetbrains.compose.test.tests.integration.*"

// check we don't accidentally including unexpected classes (e.g. from embedded dependencies)
tasks.registerVerificationTask<CheckJarPackagesTask>("checkJar") {
    dependsOn(jar)
    jarFile.set(jar.archiveFile)
    allowedPackagePrefixes.addAll("org.jetbrains.compose", "kotlinx.serialization")
}

tasks.test {
    dependsOn(jar)
    classpath = project.files(jar.map { it.archiveFile }) + classpath
    filter {
        excludeTestsMatching(gradleTestsPattern)
    }
}

for (gradleVersion in supportedGradleVersions) {
    tasks.registerVerificationTask<Test>("testGradle-${gradleVersion.version}") {
        classpath = tasks.test.get().classpath

        if (gradleVersion >= GradleVersion.version("7.6")) {
            systemProperty("compose.tests.gradle.configuration.cache", "true")
        }
        systemProperty("compose.tests.gradle.version", gradleVersion.version)
        filter {
            includeTestsMatching(gradleTestsPattern)
        }
    }
}

configureAllTests {
    configureJavaForComposeTest()
    dependsOn(":publishToMavenLocal")
    systemProperty("compose.tests.compose.gradle.plugin.version", BuildProperties.deployVersion(project))
    val summaryDir = project.buildDir.resolve("test-summary")
    systemProperty("compose.tests.summary.file", summaryDir.resolve("$name.md").absolutePath)
    systemProperties(project.properties.filter { it.key.startsWith("compose.") })
}

task("printAllAndroidxReplacements") {
    doLast { printAllAndroidxReplacements() }
}
