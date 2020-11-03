buildscript {
    repositories {
        // TODO: remove after new build is published
        mavenLocal()
        google()
        jcenter()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    dependencies {
        // TODO/migrateToMaster 0.1.0-dev104 is built from "unmerged" branch,
        //  replace it by version from androidx-master-dev when scrollbars will be merged
        classpath("org.jetbrains.compose:compose-gradle-plugin:0.1.0-m1-build57")
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath(kotlin("gradle-plugin", version = "1.4.0"))
    }
}

allprojects {
    repositories {
        // TODO: remove after new build is published
        mavenLocal()
        google()
        jcenter()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}