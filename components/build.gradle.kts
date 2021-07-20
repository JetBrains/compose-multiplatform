buildscript {
    val composeVersion = property("compose.version")

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    dependencies {
        classpath("org.jetbrains.compose:compose-gradle-plugin:$composeVersion")
        // __KOTLIN_COMPOSE_VERSION__
        classpath(kotlin("gradle-plugin", version = "1.5.21"))
    }
}

subprojects {
    version = findProperty("deploy.version") ?: property("compose.version")!!

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins.withId("java") {
        configureIfExists<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11

            withJavadocJar()
            withSourcesJar()
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