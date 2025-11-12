import org.jetbrains.compose.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.dsl.AppCdsMode
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

compose.desktop {
    application {
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(%JAVA_VERSION%))
            vendor.set(DefaultJvmVendorSpec.of(JvmVendor.KnownJvmVendor.%JVM_VENDOR%))
        }.get().metadata.installationPath.asFile.absolutePath

        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageVersion = "1.0.0"
        }
        buildTypes.release {
            appCds {
                mode = %APP_CDS_MODE%
                logging = true
                exitAppOnCdsFailure = true
            }
        }
    }
}
