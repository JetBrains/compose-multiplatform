# Widgets gallery

This example is derived from
[ComposeCookBook](https://github.com/Gurupreet/ComposeCookBook) project
by Gurupreet Singh ([@Gurupreet](https://github.com/Gurupreet))
published under [MIT license](third_party/ComposeCookBook_LICENSE.txt).

An example of Compose application for Desktop, Android and iOS platforms, 
demonstrating how to use various Material widgets.

## Setting up your development environment

To setup the environment, please consult these [instructions](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-setup.html).

## How to run

Choose a run configuration for an appropriate target in Android Studio and run it.

![run-configurations.png](run-configurations.png)

## Run on desktop via Gradle

`./gradlew desktopApp:run`

### Building native desktop distribution
```
./gradlew :desktop:packageDistributionForCurrentOS
# outputs are written to desktop/build/compose/binaries
```
