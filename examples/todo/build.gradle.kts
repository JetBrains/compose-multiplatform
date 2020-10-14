plugins {
    `kotlin-dsl`
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://dl.bintray.com/arkivanov/maven")
        maven("https://dl.bintray.com/badoo/maven")
    }
}
