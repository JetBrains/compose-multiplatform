# Compose Multiplatform Application

Сan run on desktop, Android and iOS.
Note that iOS target is experimental so far.

*Prerequisites*: to run on iOS and Android, you should have "Kotlin Multiplatform Mobile" plugin installed 
                 in Android Studio with [installed CocoaPods](https://kotlinlang.org/docs/native-cocoapods.html).

## How to run

Choose a run configuration for an appropriate target in IDE and run it.

## Configuring iOS application and running it on a real iOS device

*Before trying iOS target we strongly recommend
to create a sample iOS project in XCode and
manage to run it on a real iOS device.*

To deal with iOS target, this project contains `iosApp` folder 
with XCode project (`iosApp/iosApp.xcodeproj`) and entry point 
Swift source files (`iosApp/iosApp`). 
However, to not interact with XCode directly, settings that you need 
to configure are extracted to `iosApp/Configuration/Config.xcconfig`, namely:

- `APP_NAME` - target executable and application bundle name
- `BUNDLE_ID` - [bundle identifier](https://developer.apple.com/documentation/appstoreconnectapi/bundle_ids)
- `TEAM_ID` - [Team ID](https://developer.apple.com/help/account/manage-your-team/locate-your-team-id/#:~:text=A%20Team%20ID%20is%20a,developer%20in%20App%20Store%20Connect.)

Note that you need to configure `TEAM_ID` at least to be able 
to run on a real iOS device.

**Hint:** If you managed to run a sample iOS project in XCode on a real iOS device as we suggested above, 
you may find  your `TEAM_ID` opening XCode project in a text editor and searching
`DEVELOPMENT_TEAM` substring in it.

To configure `APP_NAME` setting, open `iosApp/Configuration/Config.xcconfig` 
in any text editor *before opening* the project in Android Studio
and correct respective value.
If you need to change the setting after you open the project in Android Studio,
please do the following:

 - close the project in Android Studio
 - run `./cleanup.sh` script in a terminal
 - correct the setting
 - open the project in Android Studio again

**Hint:** To configure advanced settings you need to use XCode. 
          To do that, open `iosApp/iosApp.xcworkspace` in XCode after opening the project in Android Studio.
 
## Desktop useful gradle tasks
- `./gradlew run` - run application
- `./gradlew package` - package native distribution into `build/compose/binaries`
