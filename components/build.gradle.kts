import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") apply false
}

subprojects {
    version = findProperty("deploy.version")!!

    plugins.withId("java") {
        configureIfExists<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11

            withJavadocJar()
            withSourcesJar()
        }
    }

    tasks.withType<KotlinCompile>() {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    plugins.withId("maven-publish") {
        configureIfExists<PublishingExtension> {
            repositories {
                maven {
                    name = "ComposeRepo"
                    setUrl(System.getenv("COMPOSE_REPO_URL"))
                    credentials {
                        username = System.getenv("COMPOSE_REPO_USERNAME")
                        password = System.getenv("COMPOSE_REPO_KEY")
                    }
                }
            }
        }
    }
}