import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {

    val knTargets = listOf(
        iosX64()
    )

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val darwinMain = sourceSets.getOrCreate("darwinMain")
        darwinMain.dependsOn(commonMain)
        val darwinTest = sourceSets.getOrCreate("darwinTest")
        darwinMain.dependsOn(commonTest)

//        val appleMain = sourceSets.maybeCreate("appleMain").apply {
//            dependsOn(nativeCommonMain)
//        }
        knTargets.forEach { target ->
                target.compilations.getByName("main").source(darwinMain)
                target.compilations.getByName("test").source(darwinTest)
        }
/*
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(npm("css-typed-om", "0.4.0"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }*/
    }
}

typealias SourceSets = NamedDomainObjectContainer<KotlinSourceSet>
fun SourceSets.getOrCreate(name: String): KotlinSourceSet = findByName(name) ?: create(name)
