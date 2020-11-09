buildscript {
    repositories {
        // TODO: remove after new build is published
        mavenLocal().mavenContent {
            includeModule("org.jetbrains.compose", "compose-gradle-plugin")
        }
        google()
        jcenter()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    dependencies {
        // __UPDATE_COMPOSE_VERSION_MARKER__
        classpath("org.jetbrains.compose:compose-gradle-plugin:0.1.0-build113")
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath(kotlin("gradle-plugin", version = "1.4.0"))
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
