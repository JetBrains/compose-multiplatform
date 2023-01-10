# Minesweeper

A simple game powered by Compose Multiplatform.

Game can run on Android, iOS, desktop or in a browser.

*Prerequisites*: to run on iOS and Android, you should have "Kotlin Multiplatform Mobile" plugin installed either 
                 in Android Studio or in AppCode with [installed CocoaPods](https://kotlinlang.org/docs/native-cocoapods.html).


## How to run 

Choose a run configuration for an appropriate target in IDE and run it.

![run-configurations.png](run-configurations.png)

To run on iOS device, please correct `iosApp/Configuration/TeamId.xcconfig` with your Apple Team ID. 
Alternatively, you may setup signing within XCode opening `iosApp/Minesweeper.xcworkspace` and then 
using "Signing & Capabilities" tab of `Minesweeper` target.

Then choose **iosApp** configuration in IDE and run it 
(may also be referred as `Minesweeper` in the Run Configurations or `iosApp (Android Studio)` for Android studio).

## Run on desktop via Gradle

`./gradlew desktopApp:run`

## Run JS in browser with WebAssembly Skia via Gradle

`./gradlew jsApp:jsBrowserDevelopmentRun`

## Run native on MacOS
`./gradlew runDebugExecutableMacosX64` (Works on Intel processors)

## Credits
<div>Icons made by <a href="https://www.flaticon.com/authors/creaticca-creative-agency" title="Creaticca Creative Agency">Creaticca Creative Agency</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>