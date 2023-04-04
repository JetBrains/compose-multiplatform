# Code Viewer
MPP Code Viewer example for desktop/android/iOS written in Multiplatform Compose library.

## How to run

Choose a run configuration for an appropriate target in IDE and run it.

![run-configurations.png](run-configurations.png)

To run on iOS device, please correct `TEAM_ID` value in `iosApp/Configuration/Config.xcconfig` with your Apple Team ID.
Alternatively, you may setup signing within XCode opening `iosApp/iosApp.xcworkspace` and then
using "Signing & Capabilities" tab of `Codeviewer` target.

Then choose **iosApp** configuration in IDE and run it.

## Run on desktop via Gradle

`./gradlew desktopApp:run`

### Building native desktop distribution
```
./gradlew :desktop:packageDistributionForCurrentOS
# outputs are written to desktop/build/compose/binaries
```

![Desktop](screenshots/codeviewer.png)