plugins {
    `kotlin-dsl`
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // todo: remove after https://github.com/badoo/Reaktive/issues/580 is resolved
        jcenter()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
