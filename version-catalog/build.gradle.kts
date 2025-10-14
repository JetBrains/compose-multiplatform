plugins {
  `version-catalog`
  `maven-publish`
}

repositories {
  google()
}

group = "org.jetbrains.compose"
version = "2025.10.00"

catalog {
  versionCatalog {
    // versions
    version("compose", "1.9.0")
    version("material3", "1.9.0-beta06")
    version("lifecycle", "2.9.4")
    version("navigation", "2.9.0")
    version("savedstate", "1.3.4")
    version("window", "1.4.0")

    // libraries
    library("animation", "org.jetbrains.compose.animation", "animation").versionRef("compose")
    library("animation-core", "org.jetbrains.compose.animation", "animation-core").versionRef("compose")
    library("animation-graphics", "org.jetbrains.compose.animation", "animation-graphics").versionRef("compose")

    library("annotation-internal", "org.jetbrains.compose.annotation-internal", "annotation").versionRef("compose")
    library("collection-internal", "org.jetbrains.compose.collection-internal", "collection").versionRef("compose")

    library("components-animatedimage", "org.jetbrains.compose.components", "components-animatedimage").versionRef("compose")
    library("components-resources", "org.jetbrains.compose.components", "components-resources").versionRef("compose")
    library("components-splitpane", "org.jetbrains.compose.components", "components-splitpane").versionRef("compose")
    library("components-ui-tooling-preview", "org.jetbrains.compose.components", "components-ui-tooling-preview").versionRef("compose")

    library("desktop", "org.jetbrains.compose.desktop", "desktop").versionRef("compose")
    library("desktop-jvm", "org.jetbrains.compose.desktop", "desktop-jvm").versionRef("compose")

    // per platform desktop artifacts
    library("desktop-jvm-macos-x64", "org.jetbrains.compose.desktop", "desktop-jvm-macos-x64").versionRef("compose")
    library("desktop-jvm-macos-arm64", "org.jetbrains.compose.desktop", "desktop-jvm-macos-arm64").versionRef("compose")
    library("desktop-jvm-windows-x64", "org.jetbrains.compose.desktop", "desktop-jvm-windows-x64").versionRef("compose")
    library("desktop-jvm-windows-arm64", "org.jetbrains.compose.desktop", "desktop-jvm-windows-arm64").versionRef("compose")
    library("desktop-jvm-linux-x64", "org.jetbrains.compose.desktop", "desktop-jvm-linux-x64").versionRef("compose")
    library("desktop-jvm-linux-arm64", "org.jetbrains.compose.desktop", "desktop-jvm-linux-arm64").versionRef("compose")

    library("foundation", "org.jetbrains.compose.foundation", "foundation").versionRef("compose")

    library("html-benchmark-core", "org.jetbrains.compose.html", "html-benchmark-core").versionRef("compose")
    library("html-core", "org.jetbrains.compose.html", "html-core").versionRef("compose")
    library("html-integration-core", "org.jetbrains.compose.html", "html-integration-core").versionRef("compose")
    library("html-svg", "org.jetbrains.compose.html", "html-svg").versionRef("compose")
    library("html-test-utils", "org.jetbrains.compose.html", "html-test-utils").versionRef("compose")
    library("html-internal-html-core-runtime", "org.jetbrains.compose.html", "internal-html-core-runtime").versionRef("compose")

    library("material", "org.jetbrains.compose.material", "material").versionRef("compose")
    library("material-navigation", "org.jetbrains.compose.material", "material-navigation").versionRef("compose")
    library("material-ripple", "org.jetbrains.compose.material", "material-ripple").versionRef("compose")

    library("runtime", "org.jetbrains.compose.runtime", "runtime").versionRef("compose")
    library("runtime-saveable", "org.jetbrains.compose.runtime", "runtime-saveable").versionRef("compose")

    library("ui", "org.jetbrains.compose.ui", "ui").versionRef("compose")
    library("ui-backhandler", "org.jetbrains.compose.ui", "ui-backhandler").versionRef("compose")
    library("ui-desktop", "org.jetbrains.compose.ui", "ui-desktop").versionRef("compose")
    library("ui-geometry", "org.jetbrains.compose.ui", "ui-geometry").versionRef("compose")
    library("ui-graphics", "org.jetbrains.compose.ui", "ui-graphics").versionRef("compose")
    library("ui-test", "org.jetbrains.compose.ui", "ui-test").versionRef("compose")
    library("ui-text", "org.jetbrains.compose.ui", "ui-text").versionRef("compose")
    library("ui-tooling", "org.jetbrains.compose.ui", "ui-tooling").versionRef("compose")
    library("ui-tooling-data", "org.jetbrains.compose.ui", "ui-tooling-data").versionRef("compose")
    library("ui-tooling-preview", "org.jetbrains.compose.ui", "ui-tooling-preview").versionRef("compose")
    library("ui-unit", "org.jetbrains.compose.ui", "ui-unit").versionRef("compose")
    library("ui-util", "org.jetbrains.compose.ui", "ui-util").versionRef("compose")

    library("material3", "org.jetbrains.compose.material3", "material3").versionRef("material3")
    library("material3-adaptive-navigation-suite", "org.jetbrains.compose.material3", "material3-adaptive-navigation-suite").versionRef("material3")
    library("material3-window-sizeclass", "org.jetbrains.compose.material3", "material3-window-size-class").versionRef("material3")
    library("material3-adaptive", "org.jetbrains.compose.material3.adaptive", "adaptive").versionRef("material3")
    library("material3-adaptive-layout", "org.jetbrains.compose.material3.adaptive", "adaptive-layout").versionRef("material3")
    library("material3-adaptive-navigation", "org.jetbrains.compose.material3.adaptive", "adaptive-navigation").versionRef("material3")

    library("lifecycle-common", "org.jetbrains.androidx.lifecycle", "lifecycle-common").versionRef("lifecycle")
    library("lifecycle-runtime", "org.jetbrains.androidx.lifecycle", "lifecycle-runtime").versionRef("lifecycle")
    library("lifecycle-runtime-compose", "org.jetbrains.androidx.lifecycle", "lifecycle-runtime-compose").versionRef("lifecycle")
    library("lifecycle-viewmodel", "org.jetbrains.androidx.lifecycle", "lifecycle-viewmodel").versionRef("lifecycle")
    library("lifecycle-viewmodel-compose", "org.jetbrains.androidx.lifecycle", "lifecycle-viewmodel-compose").versionRef("lifecycle")
    library("lifecycle-viewmodel-savedstate", "org.jetbrains.androidx.lifecycle", "lifecycle-viewmodel-savedstate").versionRef("lifecycle")

    library("navigation-common", "org.jetbrains.androidx.navigation", "navigation-common").versionRef("navigation")
    library("navigation-compose", "org.jetbrains.androidx.navigation", "navigation-compose").versionRef("navigation")
    library("navigation-runtime", "org.jetbrains.androidx.navigation", "navigation-runtime").versionRef("navigation")

    library("savedstate", "org.jetbrains.androidx.savedstate", "savedstate").versionRef("savedstate")
    library("savedstate-compose", "org.jetbrains.androidx.savedstate", "savedstate-compose").versionRef("savedstate")

    library("window-core", "org.jetbrains.androidx.window", "window-core").versionRef("window")
  }
}


publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["versionCatalog"])

      artifactId = "compose-multiplatform-version-catalog"
      pom {
        name = "Compose Multiplatform Version Catalog"
        description = "A compatible set of Compose Multiplatform libraries."
        url = "https://jetbrains.com/compose-multiplatform/"

        licenses {
          license {
            name = "The Apache License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
      }
    }
  }
}
