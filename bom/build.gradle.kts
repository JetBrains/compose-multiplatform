plugins {
  `maven-publish`
  `java-platform`
}

repositories {
  google()
}

group = "org.jetbrains.compose"
version = "2025.10.00"

dependencies {
    constraints {
        api(libs.jetpack.compose.bom)

        api(libs.animation)
        api(libs.animation.core)
        api(libs.animation.graphics)

        api(libs.annotation.internal)
        api(libs.collection.internal)

        api(libs.components.animatedimage)
        api(libs.components.resources)
        api(libs.components.splitpane)
        api(libs.components.ui.tooling.preview)

        api(libs.desktop)
        api(libs.desktop.jvm)

        api(libs.foundation)

        api(libs.html.benchmark.core)
        api(libs.html.core)
        api(libs.html.integration.core)
        api(libs.html.svg)
        api(libs.html.test.utils)
        api(libs.html.internal.html.core.runtime)

        api(libs.material)
        api(libs.material.navigation)
        api(libs.material.ripple)

        api(libs.runtime)
        api(libs.runtime.saveable)

        api(libs.ui)
        api(libs.ui.backhandler)
        api(libs.ui.desktop)
        api(libs.ui.geometry)
        api(libs.ui.graphics)
        api(libs.ui.test)
        api(libs.ui.text)
        api(libs.ui.tooling)
        api(libs.ui.tooling.data)
        api(libs.ui.tooling.preview)
        api(libs.ui.unit)
        api(libs.ui.util)

        api(libs.material3)
        api(libs.material3.adaptive.navigation.suite)
        api(libs.material3.window.sizeclass)
        api(libs.material3.adaptive)
        api(libs.material3.adaptive.layout)
        api(libs.material3.adaptive.navigation)

        api(libs.lifecycle.common)
        api(libs.lifecycle.runtime)
        api(libs.lifecycle.runtime.compose)
        api(libs.lifecycle.viewmodel)
        api(libs.lifecycle.viewmodel.compose)
        api(libs.lifecycle.viewmodel.savedstate)

        api(libs.navigation.common)
        api(libs.navigation.compose)
        api(libs.navigation.runtime)

        api(libs.savedstate)
        api(libs.savedstate.compose)

        api(libs.window.core)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])

            artifactId = "compose-multiplatform-bom"
            pom {
                name = "Compose Multiplatform Version BOM"
                description = "A compatible set of Compose Multiplatform libraries."
                url = "https://jetbrains.com/compose-multiplatform/"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        name = "Compose Multiplatform Team"
                        email = "compose@jetbrains.com"
                    }
                }
            }
        }
    }
}
