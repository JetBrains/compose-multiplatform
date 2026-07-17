import org.jetbrains.compose.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.dsl.AotMode
import org.gradle.internal.jvm.inspection.JvmVendor
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(compose.desktop.currentOs)
}

val javaVersion = JavaLanguageVersion.of(%JAVA_VERSION%)
val javaVendor = DefaultJvmVendorSpec.of(JvmVendor.KnownJvmVendor.%JVM_VENDOR%)

kotlin {
    jvmToolchain {
        languageVersion.set(javaVersion)
        vendor.set(javaVendor)
    }
}

compose.desktop {
    application {
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(javaVersion)
            vendor.set(javaVendor)
        }.get().metadata.installationPath.asFile.absolutePath

        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageVersion = "1.0.0"
        }
        buildTypes.release {
            aot {
                mode = %AOT_MODE%
                logging = true
                exitAppOnAotFailure = true
            }
        }
    }
}
