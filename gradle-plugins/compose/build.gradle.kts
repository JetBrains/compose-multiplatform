import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish.plugin)
    id("java-gradle-plugin")
    id("maven-publish")
    alias(libs.plugins.shadow.jar)
    alias(libs.plugins.download)
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
    fieldsToGenerate.put("composeMaterial3Version", BuildProperties.composeMaterial3Version(project))
    fieldsToGenerate.put("composeGradlePluginVersion", BuildProperties.deployVersion(project))
    fieldsToGenerate.put("composeHotReloadVersion", libs.plugin.hot.reload.get().version)
}
tasks.named("compileKotlin", KotlinCompilationTask::class) {
    dependsOn(buildConfig)
    compilerOptions.freeCompilerArgs.add("-opt-in=org.jetbrains.compose.ExperimentalComposeLibrary")
}
sourceSets.main.configure {
    java.srcDir(buildConfig.flatMap { it.generatedOutputDir })
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

    fun hotReloadDep(dep: Any) {
        // We need to explicitly depend on the Hot Reload Gradle plugin to be able to apply it by id.
        // Thus, we need to publish the dependency in the resulting .pom/.module files.
        // Other dependencies are embedded, so we use `shadow` for the publication.
        shadow(dep)

        // we still need `runtimeOnly` because `gradle-plugin` is included in the `components` project
        runtimeOnly(dep)
    }

    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(kotlin("native-utils"))
    compileOnly(libs.plugin.android)
    compileOnly(libs.plugin.android.api)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
    testImplementation(kotlin("gradle-plugin-api"))

    embedded(libs.download.task)
    embedded(libs.kotlin.poet)
    embedded(project(":preview-rpc"))
    embedded(project(":jdk-version-probe"))

    hotReloadDep(libs.plugin.hot.reload)
}

val packagesToRelocate = listOf("de.undercouch", "com.squareup.kotlinpoet")

val shadow = tasks.named<ShadowJar>("shadowJar") {
    for (packageToRelocate in packagesToRelocate) {
        relocate(packageToRelocate, "org.jetbrains.compose.internal.$packageToRelocate")
    }
    archiveBaseName.set("shadow")
    archiveClassifier.set("")
    archiveVersion.set("")
    configurations = listOf(embeddedDependencies)
    exclude("META-INF/gradle-plugins/de.undercouch.download.properties")
    exclude("META-INF/versions/**")
}

val jar = tasks.named<Jar>("jar") {
    dependsOn(shadow)
    from(zipTree(shadow.get().archiveFile))
    this.duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val supportedGradleVersions = project.propertyList("compose.tests.gradle.versions")
val supportedAgpVersions = project.propertyList("compose.tests.agp.versions")
val excludedGradleAgpVersions = project.propertyList("compose.tests.gradle-agp.exclude")

fun Project.propertyList(name: String) =
    project.property(name).toString()
        .split(",")
        .map { it.trim() }

val gradleTestsPattern = "org.jetbrains.compose.test.tests.integration.*"

// check we don't accidentally including unexpected classes (e.g. from embedded dependencies)
tasks.registerVerificationTask<CheckJarPackagesTask>("checkJar") {
    dependsOn(jar)
    jarFile.set(jar.archiveFile)
    allowedPackagePrefixes.addAll("org.jetbrains.compose", "kotlinx.serialization", "com.squareup.kotlinpoet")
}

tasks.test {
    dependsOn(jar)
    classpath = project.files(jar.map { it.archiveFile }) + classpath
    filter {
        excludeTestsMatching(gradleTestsPattern)
    }
}

if (properties.getOrDefault("dev.junit.parallel", "false") == "true") {
    logger.lifecycle("Test task will run in parallel")
    tasks.withType(Test::class.java) {
        //https://junit.org/junit5/docs/current/user-guide/#writing-tests-parallel-execution-config-properties
        systemProperties["junit.jupiter.execution.parallel.enabled"] = true
        systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    }
}

/**
 * Gradle 8.0 removed auto downloading of requested toolchains unless a toolchain repository is configured.
 * For now, the only option to enable auto downloading out-of-the-box is to use Foojay Disco resolver,
 * which uses api.foojay.io service.
 * It is not desirable to depend on little known service for provisioning JDK distributions, even for tests.
 * Thus, the only option is to download the necessary JDK distributions ourselves.
 */
val jdkVersionsForTests = listOf(11, 19)
val jdkForTestsRoot = project.gradle.gradleUserHomeDir.resolve("compose-jb-jdks")
val downloadJdksForTests = tasks.register("downloadJdksForTests") {}

for (jdkVersion in jdkVersionsForTests) {
    val ext = if (hostOS == OS.Windows) ".zip" else ".tar.gz"
    val archive = jdkForTestsRoot.resolve("$jdkVersion$ext")
    val unpackDir = jdkForTestsRoot.resolve("$jdkVersion").apply { mkdirs() }
    val downloadJdkTask = tasks.register("downloadJdk$jdkVersion", Download::class) {
        src("https://corretto.aws/downloads/latest/amazon-corretto-$jdkVersion-x64-${hostOS.id}-jdk$ext")
        dest(archive)
        onlyIf { !dest.exists() }
    }
    val unpackJdkTask = tasks.register("unpackJdk$jdkVersion", Copy::class) {
        dependsOn(downloadJdkTask)
        val archive = archive
        val archiveTree = when {
            archive.name.endsWith(".tar.gz") -> tarTree(archive)
            archive.name.endsWith(".zip") -> zipTree(archive)
            else -> error("Unsupported archive format: ${archive.name}")
        }
        from(archiveTree)
        into(unpackDir)
        onlyIf { (unpackDir.listFiles()?.size ?: 0) == 0 }
    }
    downloadJdksForTests.dependsOn(unpackJdkTask)
}

for (gradleVersion in supportedGradleVersions) {
    for (agpVersion in supportedAgpVersions) {
        tasks.registerVerificationTask<Test>("test-Gradle(${gradleVersion})-Agp($agpVersion)") {
            classpath = tasks.test.get().classpath
            filter { includeTestsMatching(gradleTestsPattern) }
            dependsOn(downloadJdksForTests)

            /*
             * Fixes this kind of error:
             * What went wrong:
             * An exception occurred applying plugin request [id: 'com.android.application', version: '8.2.2']
             * > Failed to apply plugin 'com.android.internal.version-check'.
             * > Minimum supported Gradle version is 8.2. Current version is 7.4.
             */
            val isExcluded = excludedGradleAgpVersions.contains("$gradleVersion/$agpVersion")
            onlyIf { !isExcluded }

            systemProperty("compose.tests.gradle.test.jdks.root", jdkForTestsRoot.absolutePath)
            systemProperty("compose.tests.gradle.version", gradleVersion)
            systemProperty("compose.tests.agp.version", agpVersion)
            systemProperty(
                "compose.tests.gradle.configuration.cache",
                GradleVersion.version(gradleVersion) >= GradleVersion.version("8.0")
            )
        }
    }
}

configureAllTests {
    dependsOn(":publishToMavenLocal")
    systemProperty("compose.tests.compose.gradle.plugin.version", BuildProperties.deployVersion(project))
    val summaryDir = project.layout.buildDirectory.get().asFile.resolve("test-summary")
    systemProperty("compose.tests.summary.file", summaryDir.resolve("$name.md").absolutePath)
    systemProperties(project.properties.filter { it.key.startsWith("compose.") })
}
