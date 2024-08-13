# Chat example app

## SwiftUI interop
This example shows how you can set up an interop between SwiftUI and Compose.
Pay attention to the file [ComposeViewControllerToSwiftUI.swift](iosApp%2FiosApp%2FComposeViewControllerToSwiftUI.swift).
This file helps to add Compose inside SwiftUI hierarchy.

Example can run on Android, iOS, desktop or in a browser.

## Setting up your development environment

To setup the environment, please consult these [instructions](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-setup.html).

## How to run 

Choose a run configuration for an appropriate target in Android Studio and run it.

![run-configurations.png](run-configurations.png)

## Run on desktop via Gradle

`./gradlew desktopApp:run`

### Run MacOS via Gradle:
- on Intel CPU: `./gradlew :shared:runDebugExecutableMacosX64`
- on Apple Silicon: `./gradlew :shared:runDebugExecutableMacosArm64`
