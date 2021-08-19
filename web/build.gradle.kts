val COMPOSE_CORE_VERSION: String by project
val COMPOSE_WEB_VERSION: String by project
val COMPOSE_REPO_USERNAME: String? by project
val COMPOSE_REPO_KEY: String? by project
val COMPOSE_WEB_BUILD_WITH_SAMPLES = project.property("compose.web.buildSamples")!!.toString().toBoolean()

apply<jetbrains.compose.web.gradle.SeleniumDriverPlugin>()

fun Project.isSampleProject() = projectDir.parentFile.name == "examples"

tasks.register("printBundleSize") {
    dependsOn(
       subprojects.filter { it.isSampleProject() }.map { ":examples:${it.name}:printBundleSize" } 
    )
}

subprojects { 
    apply(plugin = "maven-publish")

    group = "org.jetbrains.compose.web"
    version = COMPOSE_WEB_VERSION

    pluginManager.withPlugin("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "internal"
                    url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
                    credentials {
                        username = COMPOSE_REPO_USERNAME ?: ""
                        password = COMPOSE_REPO_KEY ?: ""
                    }
                }
            }
        }
    }

    val printTestBundleSize by tasks.registering {
        doLast {
            val bundlePath = buildDir.resolve(
                "compileSync/test/testDevelopmentExecutable/kotlin/${rootProject.name}-${project.name}-test.js"
            )
            if (bundlePath.exists()) {
                val size = bundlePath.length()
                println("##teamcity[buildStatisticValue key='testBundleSize::${project.name}' value='$size']")
            }
        }
    }

    afterEvaluate {
        tasks.named("jsTest") { finalizedBy(printTestBundleSize) }
    }

    if (isSampleProject()) {
        val printBundleSize by tasks.registering {
            dependsOn(tasks.named("jsBrowserDistribution"))
            doLast {
                val jsFile = buildDir.resolve("distributions/${project.name}.js")
                val size = jsFile.length()
                println("##teamcity[buildStatisticValue key='bundleSize::${project.name}' value='$size']")
            }
        }

        afterEvaluate {
            tasks.named("build") { finalizedBy(printBundleSize) }
        }
    }

    if (COMPOSE_WEB_BUILD_WITH_SAMPLES) {
        println("substituting published artifacts with projects ones in project $name")
        configurations.all {
            resolutionStrategy.dependencySubstitution {
                substitute(module("org.jetbrains.compose.web:web-widgets")).apply {
                     with(project(":web-widgets"))
                }
                substitute(module("org.jetbrains.compose.web:web-core")).apply {
                     with(project(":web-core"))
                }
            }
        }
    }

    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
        }
        google()
    }
}

