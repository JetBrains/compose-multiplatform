plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val copyResources = tasks.create("copyJsResourcesWorkaround", Copy::class.java) {
    from(project(":resources:demo:shared").file("src/commonMain/resources"))
    into("build/processedResources/js/main")
}

afterEvaluate {
    project.tasks.getByName("jsProcessResources").finalizedBy(copyResources)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting  {
            dependencies {
                implementation(project(":resources:demo:shared"))
            }
        }
    }
}

compose.experimental {
    web.application {}
}

project.configurations.forEach { conf ->
    conf.resolutionStrategy.eachDependency {
        if (requested.module.name.contains("kotlin-stdlib")) {
            useVersion("1.8.20-Beta")
        }
    }
}