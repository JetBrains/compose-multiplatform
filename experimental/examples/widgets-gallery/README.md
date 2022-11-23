# Widgets gallery

This example is derived from
[ComposeCookBook](https://github.com/Gurupreet/ComposeCookBook) project
by Gurupreet Singh ([@Gurupreet](https://github.com/Gurupreet))
published under [MIT license](third_party/ComposeCookBook_LICENSE.txt).

An example of Compose application for Desktop, Android and iOS platforms, 
demonstrating how to use various Material widgets.

## How to run

Choose a run configuration for an appropriate target in IDE and run it.

![run-configurations.png](run-configurations.png)

To run on iOS device, please correct `iosApp/Configuration/TeamId.xcconfig` with your Apple Team ID.
Alternatively, you may setup signing within XCode opening `iosApp/WidgetsGallery.xcworkspace` and then
using "Signing & Capabilities" tab of `WidgetsGallery` target.

Then choose **iosApp** configuration in IDE and run it
(may also be referred as `WidgetsGallery` in the Run Configurations or `iosApp_` for Android studio).

## Run on desktop via Gradle

`./gradlew desktopApp:run`

### Building native desktop distribution
```
./gradlew :desktop:packageDistributionForCurrentOS
# outputs are written to desktop/build/compose/binaries
```
