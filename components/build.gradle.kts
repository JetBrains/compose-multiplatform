buildscript {
    // __LATEST_COMPOSE_RELEASE_VERSION__
    val composeVersion = System.getenv("COMPOSE_RELEASE_VERSION") ?: "0.4.0-build182"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    dependencies {
        classpath("org.jetbrains.compose:compose-gradle-plugin:$composeVersion")
        // __KOTLIN_COMPOSE_VERSION__
        classpath(kotlin("gradle-plugin", version = "1.4.32"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
