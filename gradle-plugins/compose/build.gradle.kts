import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    kotlin("jvm")
    id("de.fuerstenau.buildconfig")
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

gradlePluginConfig {
    pluginId = "org.jetbrains.compose"
    artifactId = "compose-gradle-plugin"
    displayName = "JetBrains Compose Plugin"
    description = "JetBrains Compose Gradle plugin for easy configuration"
    implementationClass = "org.jetbrains.compose.ComposePlugin"
}

buildConfig {
    packageName = "org.jetbrains.compose"
    clsName = "ComposeBuildConfig"
    buildConfigField("String", "composeVersion", BuildProperties.composeVersion(project))
}

val embedded by configurations.creating

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))
    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    fun embeddedCompileOnly(dep: String) {
        compileOnly(dep)
        embedded(dep)
    }
    // include relocated download task to avoid potential runtime conflicts
    embeddedCompileOnly("de.undercouch:gradle-download-task:4.1.1")
}

val shadow = tasks.named<ShadowJar>("shadowJar") {
    val fromPackage = "de.undercouch"
    val toPackage = "org.jetbrains.compose.$fromPackage"
    relocate(fromPackage, toPackage)
    archiveClassifier.set("shadow")
    configurations = listOf(embedded)
    exclude("META-INF/gradle-plugins/de.undercouch.download.properties")
}

val jar = tasks.named<Jar>("jar") {
    dependsOn(shadow)
    from(zipTree(shadow.get().archiveFile))
    this.duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// __SUPPORTED_GRADLE_VERSIONS__
val minGradleVersionForTests = "6.4"
val maxGradleVersionForTests = "6.7"
val javaHomeForTests: String? = when {
    JavaVersion.current() >= JavaVersion.VERSION_14 -> System.getProperty("java.home")
    else -> System.getenv("JDK_14")
         ?: System.getenv("JDK_15")
         ?: System.getenv("JDK_FOR_GRADLE_TESTS")
}
val isWindows = getCurrentOperatingSystem().isWindows

tasks.test {
    configureTest(maxGradleVersionForTests)
}

val testMinGradleVersion by tasks.registering(Test::class) {
    tasks.test.get().let { defaultTest ->
        classpath = defaultTest.classpath
    }
    configureTest(minGradleVersionForTests)
}

fun Test.configureTest(gradleVersion: String) {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    dependsOn("publishToMavenLocal")
    systemProperty("compose.plugin.version", BuildProperties.deployVersion(project))
    systemProperty("gradle.version.for.tests", gradleVersion)

    if (javaHomeForTests != null) {
        val executableFileName = if (isWindows) "java.exe" else "java"
        executable = File(javaHomeForTests).resolve("bin/$executableFileName").absolutePath
    } else {
        doFirst { error("Use JDK 14+ to run tests or set up JDK_14/JDK_15/JDK_FOR_GRADLE_TESTS env. var") }
    }
}

tasks.named("check") {
    dependsOn(testMinGradleVersion)
}

task("printAllAndroidxReplacements") {
    doLast { printAllAndroidxReplacements() }
}