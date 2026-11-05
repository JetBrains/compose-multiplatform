plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral {
        url = uri("https://cache-redirector.jetbrains.com/maven-central")
    }
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-client-core:1.5.1")
    implementation("io.ktor:ktor-client-cio:1.5.1")
}
