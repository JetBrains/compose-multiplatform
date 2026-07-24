plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":sharedUI"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
