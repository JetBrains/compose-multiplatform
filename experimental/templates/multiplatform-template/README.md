# Compose Multiplatform Application

Сan run on desktop, Android and iOS.
Note that iOS target is experimental so far.

You may download this template as a [zip file](../archives/multiplatform-template.zip). 

## Desktop
This template contains  `desktopApp` run configuration that you can use to run the desktop target in Android Studio or Intellij IDEA.

Useful Gradle tasks:
- `./gradlew run` - run application
- `./gradlew package` - package native distribution into `build/compose/binaries`

## Android
To run on Android, use `androidApp` run configuration in [Android Studio](https://developer.android.com/studio)
or use Gradle:

`./gradlew installDebug` - install Android application on an Android device (on a real device or on an emulator)

## iOS

To work with the iOS target you need:
- [Xcode](https://developer.apple.com/xcode/) (to setup the environement)
- [Android Studio](https://developer.android.com/studio)
- ["Kotlin Multiplatform Mobile" plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) (to work with all supported targets, including iOS)
- [CocoaPods](https://kotlinlang.org/docs/native-cocoapods.html)

Before trying to open the project in Android Studio, make sure that you have a correct environment for iOS development. 
Try to follow official Apple tutorials how to create a sample project in Xcode 
and register a simulator or a real device there.

### Running on an iOS simulator

If you have a correct environment, Android Studio will show available iOS simulators on which you can run the application
in `iosApp` run configuration.

### Running on a real iOS device

To run on a real iOS device you need:
- [Team ID](https://developer.apple.com/help/account/manage-your-team/locate-your-team-id/#:~:text=A%20Team%20ID%20is%20a,developer%20in%20App%20Store%20Connect.)
- set this Team ID in `iosApp/Configuration/Config.xcconfig` in the `TEAM_ID` field
- the registered iOS device in Xcode

After that you can open the project in Android Studio, and it will show the registered iOS device in `iosApp` 
run configuration.

**Hint:**
If you use a free Personal Team for signing applications to run on a real device, 
and you do not have access to the link above to know your Team ID, you may still find your Team ID value,
using the following workaround:
- manage to run a sample iOS project in XCode on a real iOS device 
- open XCode project in a text editor and search `DEVELOPMENT_TEAM` substring in it
- the value of `DEVELOPMENT_TEAM` property is your personal Team ID


### Configuring the iOS application

This template contains `iosApp/Configuration/Config.xcconfig` that allows you to configure the most basic properties, without using Xcode. It contains:
- `APP_NAME` - target executable and application bundle name
- `BUNDLE_ID` - [bundle identifier](https://developer.apple.com/documentation/bundleresources/information_property_list/cfbundleidentifier#discussion)
- `TEAM_ID` - [Team ID](https://developer.apple.com/help/account/manage-your-team/locate-your-team-id/#:~:text=A%20Team%20ID%20is%20a,developer%20in%20App%20Store%20Connect.)

Note, that to configure the `APP_NAME` setting, you should open `Config.xcconfig` in any text editor *before opening* the project in Android Studio and correct respective value.

If you need to change this setting after you open the project in Android Studio, please do the following:
- close the project in Android Studio
- run `./cleanup.sh` script in a terminal
- correct the setting
- open the project in Android Studio again

To configure advanced settings you need to use Xcode. Open `iosApp/iosApp.xcworkspace` in Xcode after opening the project in Android Studio.
