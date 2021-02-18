plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-client-core:1.5.1")
    implementation("io.ktor:ktor-client-cio:1.5.1")
}
