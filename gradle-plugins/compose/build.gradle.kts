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
    implementationClass = "org.jetbrains.compose.ComposePlugin"
}

mavenPublicationConfig {
    displayName = "JetBrains Compose Gradle Plugin"
    description = "JetBrains Compose Gradle plugin for easy configuration"
    artifactId = "compose-gradle-plugin"
}

buildConfig {
    packageName = "org.jetbrains.compose"
    clsName = "ComposeBuildConfig"
    buildConfigField("String", "composeVersion", BuildProperties.composeVersion(project))
    buildConfigField("Boolean", "isComposeWithWeb", BuildProperties.isComposeWithWeb(project).toString())
}

val embedded by configurations.creating

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))
    implementation(project(":preview-rpc"))

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
testGradleVersion("6.4")
testGradleVersion("6.8.3")
testGradleVersion("7.0-milestone-3")

val javaHomeForTests: String? = when {
    // __COMPOSE_NATIVE_DISTRIBUTIONS_MIN_JAVA_VERSION__
    JavaVersion.current() >= JavaVersion.VERSION_15 -> System.getProperty("java.home")
    else -> System.getenv("JDK_15")
         ?: System.getenv("JDK_FOR_GRADLE_TESTS")
}
val isWindows = getCurrentOperatingSystem().isWindows

val gradleTestsPattern = "org.jetbrains.compose.gradle.*"
tasks.test {
    filter {
        excludeTestsMatching(gradleTestsPattern)
    }
}
fun testGradleVersion(gradleVersion: String) {
    val taskProvider = tasks.register("testGradle-$gradleVersion", Test::class) {
        tasks.test.get().let { defaultTest ->
            classpath = defaultTest.classpath
        }
        systemProperty("gradle.version.for.tests", gradleVersion)
        filter {
            includeTestsMatching(gradleTestsPattern)
        }
        dependsOn("validateTaskProperties")
    }
    tasks.named("check") {
        dependsOn(taskProvider)
    }
}

configureJUnit()

tasks.withType<Test>().configureEach {
    configureJavaForComposeTest()

    dependsOn("publishToMavenLocal")
    systemProperty("compose.plugin.version", BuildProperties.deployVersion(project))
}

task("printAllAndroidxReplacements") {
    doLast { printAllAndroidxReplacements() }
}