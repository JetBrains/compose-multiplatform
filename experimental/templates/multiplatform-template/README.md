# Compose Multiplatform Application

Use this template to start developing your own Compose Multiplatform application targeting desktop, Android, and iOS (experimental).

## Desktop
This template contains  `desktopApp` run configuration that you can use to run the desktop target in Android Studio or IntelliJ IDEA.

Useful Gradle tasks:
- `./gradlew run` - run application
- `./gradlew package` - package native distribution into `build/compose/binaries`

## Android
To run on Android, use the `androidApp` run configuration in [Android Studio](https://developer.android.com/studio)
or use Gradle:

`./gradlew installDebug` - install Android application on an Android device (on a real device or on an emulator)

## iOS

Make sure you have set up your environment for mobile development with Kotlin Multiplatform. A detailed guide on how to set up your environment is available in the [Kotlin Multiplatform documentation](https://kotlinlang.org/docs/multiplatform-mobile-setup.html).

To work with the iOS target you need:
- A machine running a recent version of macOS
- [Xcode](https://developer.apple.com/xcode/) (to setup the environment)
- [Android Studio](https://developer.android.com/studio)
- [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) (to work with all supported targets, including iOS)
- [CocoaPods](https://kotlinlang.org/docs/native-cocoapods.html)

**Before opening the project in Android Studio**, make sure that your environment is set up for iOS and Kotlin Multiplatform development.
Use [`kdoctor`](https://github.com/Kotlin/kdoctor) to ensure your development environment is configured correctly.

We suggest going through the "Hello, World" steps of creating and deploying a sample project in Xcode to a simulator and/or your physical device.
A video tutorial for setting up Xcode and running your first "Hello, World" application is available in [this Standford CS193P lecture recording](https://youtu.be/bqu6BquVi2M?start=716&end=1399).

### Running on an iOS simulator

Once you have configured your environment correctly, you will be able to select which iOS simulator to run your application in Android Studio on by modifying the `iosApp` run configuration.

Select "Run" | "Edit Configurations..." and navigate to the "iOS Application" | "iosApp" run configuration. In the "Execution target" drop-down, select your target device.

### Running on a real iOS device

Running your Compose Multiplatform application on a physical device can be done for free. You need:
- an [Apple ID](https://support.apple.com/en-us/HT204316)
- the registered iOS device in Xcode

#### Running with a free Personal Team

If you use a free Personal Team for signing applications to run on a real device, you'll have to find your team ID.

The easiest way is to refer to your "Hello, World" project that you created while setting up your development environment. Use your terminal to navigate to the folder where you have created the Xcode project (`.xcodeproj`) and run the following command:

```bash
grep -r "DEVELOPMENT_TEAM"
```

In your multiplatform project, navigate to `iosApp/Configuration/Config.xcconfig` and set the `TEAM_ID` to the value you've gotten from the previous command.

<details>
<summary>Alternative approaches</summary>

To see your local team ID, you try running `security find-certificate -c "Apple Development" -p | openssl x509 -noout -text | grep --color 'OU=\w\w\w\w*'` in your terminal.

If you're running into trouble with the method described above, you can try this alternative method. 
- Run the `iosApp` run configuration from Android Studio (it will fail)
- Open the `iosApp/iosApp.xcworkspace` in Xcode
- Select `iosApp` in the menu on the left side
- Navigate to "Signing & Capabilities"
- Select your Personal Team in the "Team" dropdown. If you haven't set up your team, use the "Add account..." option and follow the steps inside Xcode.
</details>

After that you can open the project in Android Studio, and it will show the registered iOS device in the `iosApp`
run configuration.

#### Running with a paid Team

- Find your [Team ID](https://developer.apple.com/help/account/manage-your-team/locate-your-team-id/#:~:text=A%20Team%20ID%20is%20a,developer%20in%20App%20Store%20Connect.). 
- set this Team ID in `iosApp/Configuration/Config.xcconfig` in the `TEAM_ID` field

After that you can open the project in Android Studio, and it will show the registered iOS device in the `iosApp` 
run configuration.

### Configuring the iOS application

This template contains a `iosApp/Configuration/Config.xcconfig` configuration file that allows you to configure most basic properties without having to move to Xcode. It contains:
- `APP_NAME` - target executable and application bundle name
- `BUNDLE_ID` - [bundle identifier](https://developer.apple.com/documentation/bundleresources/information_property_list/cfbundleidentifier#discussion)
- `TEAM_ID` - [Team ID](https://developer.apple.com/help/account/manage-your-team/locate-your-team-id/#:~:text=A%20Team%20ID%20is%20a,developer%20in%20App%20Store%20Connect.)

Note: To configure the `APP_NAME` setting, open `Config.xcconfig` in any text editor *before opening* the project in Android Studio, and set the desired name.

If you need to change this setting after you open the project in Android Studio, please do the following:
- close the project in Android Studio
- run `./cleanup.sh` in your terminal
- change the setting
- open the project in Android Studio again

For configuring advanced settings, you can use Xcode. Open the `iosApp/iosApp.xcworkspace` in Xcode after opening the project in Android Studio, and use Xcode to make your changes.