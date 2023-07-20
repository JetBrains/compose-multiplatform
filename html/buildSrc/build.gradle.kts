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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
