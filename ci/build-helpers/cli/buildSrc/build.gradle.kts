plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral {
        url = uri("https://cache-redirector.jetbrains.com/maven-central")
    }
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}

dependencies {
    implementation("org.jetbrains.compose.internal.build-helpers:publishing")
}
