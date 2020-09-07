buildscript {
    repositories {
        google()
        jcenter()
        maven("https://packages.jetbrains.team/maven/p/ui/dev")
    }

    dependencies {
        classpath("org.jetbrains.compose:compose-gradle-plugin:0.1.0-demo3")
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath(kotlin("gradle-plugin", version = "1.4.0"))
    }
}

include(":common", ":android", ":desktop")