plugins {
    kotlin("jvm") version "1.4.0"
    id("com.gradle.plugin-publish") version "0.10.1"
    id("de.fuerstenau.buildconfig") version "1.1.8"
    id("java-gradle-plugin")
    id("maven-publish")
}

private object Info {
    const val name = "Jetpack Compose Plugin"
    const val website = "https://jetbrains.org/compose"
    const val description = "Jetpack Compose gradle plugin for easy configuration"
    const val artifactId = "compose-gradle-plugin"
    val composeVersion = System.getenv("COMPOSE_GRADLE_PLUGIN_COMPOSE_VERSION") ?: "0.1.0-SNAPSHOT"
    val version = System.getenv("COMPOSE_GRADLE_PLUGIN_VERSION") ?: composeVersion
}

group = "org.jetbrains.compose"
version = Info.version

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    jcenter()
    mavenLocal()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation(gradleTestKit())
}

buildConfig {
    packageName = "org.jetbrains.compose"
    clsName = "ComposeBuildConfig"
    buildConfigField("String", "composeVersion", Info.composeVersion)
}

gradlePlugin {
    plugins {
        create("compose") {
            id = "org.jetbrains.compose"
            displayName = Info.name
            description = Info.description
            implementationClass = "org.jetbrains.compose.ComposePlugin"
            version = project.version
        }
    }
}

pluginBundle {
    website = Info.website
    description = Info.description
}

publishing {
    repositories {
        maven {
            setUrl(System.getenv("COMPOSE_REPO_URL"))
            credentials {
                username = System.getenv("COMPOSE_REPO_USERNAME")
                password = System.getenv("COMPOSE_REPO_KEY")
            }
        }
    }
    publications {
        create<MavenPublication>("pluginMaven") {
            artifactId = Info.artifactId
            pom {
                name.set(Info.name)
                description.set(Info.description)
                url.set(Info.website)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}