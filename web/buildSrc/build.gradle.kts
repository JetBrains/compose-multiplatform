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
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
}
