plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}

dependencies {
    implementation("org.jetbrains.compose.internal.build-helpers:publishing")
}
