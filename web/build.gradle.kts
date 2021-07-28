val COMPOSE_CORE_VERSION: String by project
val COMPOSE_WEB_VERSION: String by project
val COMPOSE_REPO_USERNAME: String? by project
val COMPOSE_REPO_KEY: String? by project
val COMPOSE_WEB_BUILD_WITH_SAMPLES = project.property("compose.web.buildSamples")!!.toString().toBoolean()

apply<jetbrains.compose.web.gradle.SeleniumDriverPlugin>()

fun Project.isSampleProject() = projectDir.parentFile.name == "samples"

subprojects {
    apply(plugin = "maven-publish")

    group = "org.jetbrains.compose.web"
    version = COMPOSE_WEB_VERSION

    if (isSampleProject()) {

        val buildGradleSpec = copySpec {
            from("build.gradle.kts") {
                filter { 
                    it.replace(
                        "id(\"org.jetbrains.compose\")", 
                        "id(\"org.jetbrains.compose\") version \"$COMPOSE_CORE_VERSION\"", 
                    )
                }
            }
        }

        tasks.register<Sync>("sync") {
            println("project ===> ${project.projectDir.parentFile.name}")
            val targetDir = rootProject.projectDir.resolve("../examples/${project.projectDir.name}")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            from(project.projectDir)
            into(targetDir)
            exclude("build")

            with(buildGradleSpec)
            doLast {
                println("from ${project.projectDir} => $targetDir")
            }
        }
    }

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
    }
}

