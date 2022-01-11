import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "me.user"
version = "1.0"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    implementation(compose.desktop.currentOs)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.demo.visuals.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "compose-demo"
            packageVersion = "1.0.0"
        }
    }
}

afterEvaluate {
    val additionalArguments = mutableListOf<String>()

    val runTask = tasks.named<JavaExec>("run") {
        this.args = additionalArguments
    }

    tasks.register("runWords") {
        additionalArguments.add("words")
        group = "compose desktop"
        dependsOn(runTask)
    }

    tasks.register("runWave") {
        additionalArguments.add("wave")
        group = "compose desktop"
        dependsOn(runTask)
    }

    tasks.register("runNewYear") {
        additionalArguments.add("NY")
        group = "compose desktop"
        dependsOn(runTask)
    }
}