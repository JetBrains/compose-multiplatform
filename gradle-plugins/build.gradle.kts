import com.gradle.publish.PluginBundleExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.publish.plugin) apply false
    alias(libs.plugins.shadow.jar) apply false
    alias(libs.plugins.download) apply false
}

subprojects {
    group = BuildProperties.group
    version = BuildProperties.deployVersion(project)

    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }

    plugins.withId("java") {
        configureIfExists<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8

            withJavadocJar()
            withSourcesJar()
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType(KotlinJvmCompile::class).configureEach {
            // must be set to a language version of the kotlin compiler & runtime,
            // which is bundled to the oldest supported Gradle
            kotlinOptions.languageVersion = "1.5"
            kotlinOptions.apiVersion = "1.5"
            kotlinOptions.jvmTarget = "1.8"
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

                maven {
                    name = "LocalDir"
                    url = rootProject.buildDir.resolve("repo").toURI()
                }
            }
        }
    }

    afterEvaluate {
        val publicationConfig = mavenPublicationConfig
        val gradlePluginConfig = gradlePluginConfig

        if (publicationConfig != null) {
            if (gradlePluginConfig != null) {
                // pluginMaven is a default publication created by java-gradle-plugin
                // https://github.com/gradle/gradle/issues/10384
                configureMavenPublication("pluginMaven", publicationConfig)
                configureGradlePlugin(publicationConfig, gradlePluginConfig)
            } else {
                configureMavenPublication("maven", publicationConfig) {
                    from(components["java"])
                }
            }
        }
    }
}

fun Project.configureMavenPublication(
    publicationName: String,
    config: MavenPublicationConfigExtension,
    customize: MavenPublication.() -> Unit = {}
) {
    // maven publication for plugin
    configureIfExists<PublishingExtension> {
        publications.create<MavenPublication>(publicationName) {
            artifactId = config.artifactId
            pom {
                name.set(config.displayName)
                description.set(config.description)
                url.set(BuildProperties.website)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }

            customize()
        }
    }
}

@Suppress("UnstableApiUsage")
fun Project.configureGradlePlugin(
    publicationConfig: MavenPublicationConfigExtension,
    gradlePluginConfig: GradlePluginConfigExtension
) {
    // gradle plugin definition (relates to gradlePlugin extension block from java-gradle-plugin)
    // and metadata for gradle plugin portal (relates to pluginBundle extension block from com.gradle.plugin-publish)
    configureIfExists<GradlePluginDevelopmentExtension> {
        vcsUrl.set(BuildProperties.vcs)
        website.set(BuildProperties.website)
        description = publicationConfig.description

        plugins {
            create("gradlePlugin") {
                id = gradlePluginConfig.pluginId
                displayName = publicationConfig.displayName
                description = publicationConfig.description
                implementationClass = gradlePluginConfig.implementationClass
                version = project.version
                tags.set(gradlePluginConfig.pluginPortalTags)
            }
        }
    }
}

tasks.register("publishToMavenLocal") {
    val publishToMavenLocal = this
    for (subproject in subprojects) {
        subproject.plugins.withId("maven-publish") {
            publishToMavenLocal.dependsOn("${subproject.path}:publishToMavenLocal")
        }
    }
}
