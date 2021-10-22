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
                useModule("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.5.31")
            }
        }
    }
}

fun module(name: String, path: String) {
    include(name)
    val projectDir = rootDir.resolve(path).normalize().absoluteFile
    if (!projectDir.exists()) {
        throw AssertionError("file $projectDir does not exist")
    }
    project(name).projectDir = projectDir
}


module(":web-core", "core")
module(":web-svg", "svg")
module(":web-widgets", "widgets")
module(":web-integration-core", "integration-core")
module(":web-integration-widgets", "integration-widgets")
module(":compose-compiler-integration", "compose-compiler-integration")
module(":internal-web-core-runtime", "internal-web-core-runtime")
module(":test-utils", "test-utils")

if (extra["compose.web.tests.skip.benchmarks"]!!.toString().toBoolean() != true) {
    module(":web-benchmark-core", "benchmark-core")
} else {
    println("skipping benchmarks")
}

if (extra["compose.web.buildSamples"]!!.toString().toBoolean() == true) {
    println("building with examples")
    module(":examples:falling-balls-web", "../examples/falling-balls-web")
    module(":examples:compose-web-lp", "../examples/web-landing")
    module(":examples:web-compose-bird", "../examples/web-compose-bird")
    module(":examples:web-with-react", "../examples/web-with-react")
}
