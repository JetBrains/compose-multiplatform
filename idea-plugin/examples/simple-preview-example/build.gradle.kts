buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
    dependencies {
        classpath("org.jetbrains.compose:compose-gradle-plugin:0.0.9-preview-images")
        classpath(kotlin("gradle-plugin", version = "1.5.31"))
    }
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        google()
    }
}
