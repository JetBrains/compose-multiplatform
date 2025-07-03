import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.10" apply false
    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
}

subprojects {
    group = "org.jetbrains.compose.internal.build-helpers"
    version = project.property("deploy.version") as String

    repositories {
        mavenCentral()
    }

    plugins.withType(JavaBasePlugin::class.java) {
        afterEvaluate {
            configureIfExists<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
                if (sourceSets.names.contains(SourceSet.MAIN_SOURCE_SET_NAME)) {
                    withJavadocJar()
                    withSourcesJar()
                }
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        afterEvaluate {
            tasks.withType<KotlinCompile> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    plugins.withId("maven-publish") {
        configureIfExists<PublishingExtension> {
            configurePublishing(project)
        }
    }
}

fun PublishingExtension.configurePublishing(project: Project) {
    repositories {
        configureEach {
            val repoName = name
            project.tasks.register("publishTo${repoName}") {
                group = "publishing"
                dependsOn(project.tasks.named("publishAllPublicationsTo${repoName}Repository"))
            }
        }
        maven {
            name = "BuildRepo"
            url = uri("${rootProject.buildDir}/repo")
        }
        maven {
            name = "ComposeInternalRepo"
            url = uri(
                System.getenv("COMPOSE_INTERNAL_REPO_URL")
                    ?: "https://maven.pkg.jetbrains.space/public/p/compose/internal"
            )
            credentials {
                username =
                    System.getenv("COMPOSE_INTERNAL_REPO_USERNAME")
                        ?: System.getenv("COMPOSE_REPO_USERNAME")
                                ?: ""
                password =
                    System.getenv("COMPOSE_INTERNAL_REPO_KEY")
                        ?: System.getenv("COMPOSE_REPO_KEY")
                                ?: ""
            }
        }
    }
    publications {
        create<MavenPublication>("main") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(project.components["java"])
        }
    }
}

inline fun <reified T> Project.configureIfExists(fn: T.() -> Unit) {
    extensions.findByType(T::class.java)?.fn()
}
