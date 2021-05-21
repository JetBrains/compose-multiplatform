
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

val ktorVersion = "1.5.4"

plugins {
    kotlin("multiplatform") version "1.4.32"
    application
    war
    id("org.jetbrains.compose") version "0.0.0-web-dev-12"
    id("org.gretty") version "3.0.4"
}

repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven(url = "https://plugins.gradle.org/m2/")
}

kotlin {
    jvm()
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.web.web)
                implementation(compose.runtime)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.runtime) // Can this be decoupled?

                implementation("io.ktor:ktor-client-jetty:$ktorVersion")
                implementation("io.ktor:ktor-server-servlet:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-html-builder:$ktorVersion")
            }
        }
    }
}

gretty {
    contextPath = "/"
}

war {
    webAppDirName = "webapp"
}

val jvmJar: Jar by tasks
val jsBrowserProductionWebpack: KotlinWebpack by tasks
val war: War by tasks

var jsFileDir: File = jsBrowserProductionWebpack.destinationDirectory!!
var jsFileName: String = jsBrowserProductionWebpack.entry!!.name
var jsFileMapName: String = "$jsFileName.map"

val jsFile = File(jsFileDir, jsFileName)
val jsFileMap = File(jsFileDir, jsFileMapName)

war.apply {
    dependsOn(jsBrowserProductionWebpack, jvmJar)
    webInf {
        from("src/jvmMain/resources")
        into("classes") {
            from(jsFile)
            from(jsFileMap)
        }
    }
    group = "application"
    classpath(configurations["jvmRuntimeClasspath"], jvmJar)
}
