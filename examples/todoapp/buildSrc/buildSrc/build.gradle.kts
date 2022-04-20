plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    //todo workaround to build iOS Arm64 simulator:
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
}
