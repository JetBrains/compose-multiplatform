val COMPOSE_WEB_VERSION: String by project
val COMPOSE_REPO_USERNAME: String? by project
val COMPOSE_REPO_KEY: String? by project
val COMPOSE_WEB_BUILD_WITH_EXAMPLES = project.property("COMPOSE_WEB_BUILD_WITH_EXAMPLES")!!.toString()?.toBoolean()

buildscript {
    dependencies {
//        classpath deps.kotlin.gradlePlugin
//        classpath "app.cash.exhaustive:exhaustive-gradle:${versions.exhaustive}"
//        classpath 'com.vanniktech:gradle-maven-publish-plugin:0.14.2'
//        classpath 'org.jetbrains.dokka:dokka-gradle-plugin:1.4.30'
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
        classpath("com.github.jengelman.gradle.plugins:shadow:6.1.0")
        classpath("com.android.tools.build:gradle:4.2.0")
//        classpath 'com.diffplug.spotless:spotless-plugin-gradle:5.8.2'
    }
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        jcenter()
    }
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

    if (COMPOSE_WEB_BUILD_WITH_EXAMPLES) {
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
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven { 
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") 
        }
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
        }
    }
}

