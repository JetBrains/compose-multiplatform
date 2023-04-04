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

To run on iOS device, please correct `TEAM_ID` value in `iosApp/Configuration/Config.xcconfig` with your Apple Team ID.
Alternatively, you may setup signing within XCode opening `iosApp/iosApp.xcworkspace` and then
using "Signing & Capabilities" tab of `iosApp` target.

Then choose **iosApp** configuration in IDE and run it.

## Run on desktop via Gradle

`./gradlew desktopApp:run`

### Building native desktop distribution
```
./gradlew :desktop:packageDistributionForCurrentOS
# outputs are written to desktop/build/compose/binaries
```
