plugins {
    `kotlin-dsl`
}

initDeps(project)

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven ("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}


dependencies {
    implementation(Deps.JetBrains.Compose.gradlePlugin)
    implementation(Deps.JetBrains.Kotlin.gradlePlugin)
    implementation(Deps.Android.Tools.Build.gradlePlugin)
}

kotlin {
    // Add Deps to compilation, so it will become available in main project
    sourceSets.getByName("main").kotlin.srcDir("buildSrc/src/main/kotlin")
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}