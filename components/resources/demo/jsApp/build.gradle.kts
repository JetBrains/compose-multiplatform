plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val copyResources = tasks.create("copyJsResourcesWorkaround", Copy::class.java) {
    from(project(":resources:demo:shared").file("src/commonMain/resources"))
    to("build/processedResources/js/main")
}

kotlin {
    js(IR) {
        browser {
            this.commonWebpackConfig {
                println("this.outputPath: ${this.outputPath}")
                this.devServer?.contentBase
            }
            this.webpackTask {
                this.dependsOn(copyResources)
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting  {
            dependencies {
                implementation(project(":resources:demo:shared"))
            }
            resources.srcDirs += project(":resources:demo:shared").file("src/commonMain/resources")
        }
    }
}

compose.experimental {
    web.application {}
}
