buildscript {
    // __LATEST_COMPOSE_RELEASE_VERSION__
    val composeVersion = "0.0.0-use_new_compose_version-dev459"//System.getenv("COMPOSE_TEMPLATE_COMPOSE_VERSION") ?: "1.0.0-beta5"

    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    dependencies {
        classpath("org.jetbrains.compose:compose-gradle-plugin:$composeVersion")
        classpath("com.android.tools.build:gradle:7.0.0")
        // __KOTLIN_COMPOSE_VERSION__
        classpath(kotlin("gradle-plugin", version = "1.6.0-RC2"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
