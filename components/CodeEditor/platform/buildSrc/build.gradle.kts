plugins {
    `kotlin-dsl`
}

rootProject.apply {
    from(rootProject.file("../gradle/projectProperties.gradle.kts"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
}
