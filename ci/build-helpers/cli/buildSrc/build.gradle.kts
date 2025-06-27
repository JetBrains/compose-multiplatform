plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
}

dependencies {
    implementation("org.jetbrains.compose.internal.build-helpers:publishing")
}
