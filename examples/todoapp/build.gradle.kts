plugins {
    `kotlin-dsl`
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
