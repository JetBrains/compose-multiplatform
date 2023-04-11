import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {}
    sourceSets {
        val jvmMain by getting  {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))
            }
        }
    }
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