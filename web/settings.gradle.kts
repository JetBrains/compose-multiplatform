pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
        }
        google()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                println("[build] compose core version: ${extra["COMPOSE_CORE_VERSION"]}")
                useModule("org.jetbrains.compose:org.jetbrains.compose.gradle.plugin:${extra["COMPOSE_CORE_VERSION"]}")
            } else if (requested.id.id == "org.jetbrains.kotlin.multiplatform") {
                useModule("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.5.21")
            }
        }
    }
}

fun module(name: String, path: String) {
    include(name)
    project(name).projectDir = file(path)
}


module(":web-core", "$rootDir/core")
module(":web-widgets", "$rootDir/widgets")
module(":web-integration-core", "$rootDir/integration-core")
module(":web-integration-widgets", "$rootDir/integration-widgets")
module(":web-benchmark-core", "$rootDir/benchmark-core")
module(":compose-compiler-integration", "$rootDir/compose-compiler-integration")

module(":samples:falling_balls_with_web", "samples/falling_balls_with_web")
module(":samples:compose-web-lp", "samples/web_landing")
module(":samples:web-compose-bird", "samples/web-compose-bird")
module(":samples:web-with-react", "samples/web-with-react")
module(":samples:web-getting-started", "samples/web-getting-started")



if (extra["compose.web.buildSamples"]!!.toString().toBoolean() == true) {
    println("building with examples")
    module(":examples:falling_balls_with_web", "../examples/falling_balls_with_web")
    module(":examples:compose-web-lp", "../examples/web_landing")
    module(":examples:web-compose-bird", "../examples/web-compose-bird")
    module(":examples:web-with-react", "../examples/web-with-react")
    module(":examples:web-getting-started", "../examples/web-getting-started")
}
