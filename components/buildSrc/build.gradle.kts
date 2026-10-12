import java.util.Properties

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
}

val componentProperties = Properties().apply {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
}
val kotlinVersion = providers.gradleProperty("kotlin.version")
    .getOrElse(componentProperties.getProperty("kotlin.version"))
val agpVersion = providers.gradleProperty("agp.version")
    .getOrElse(componentProperties.getProperty("agp.version"))

dependencies {
    compileOnly(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    // Keep AGP visible to KGP's Android integration in the shared buildSrc classloader.
    implementation("com.android.tools.build:gradle:$agpVersion")
}
