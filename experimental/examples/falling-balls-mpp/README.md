# Falling Balls game

Game can run on Android, iOS, desktop or in a browser.

*Prerequisites*: to run on iOS and Android, you should have "Kotlin Multiplatform Mobile" plugin installed either 
                 in Android Studio or in AppCode with [installed CocoaPods](https://kotlinlang.org/docs/native-cocoapods.html).


## How to run 

Choose a run configuration for an appropriate target in IDE and run it.

![run-configurations.png](run-configurations.png)

To run on iOS device, please correct `iosApp/Configuration/TeamId.xcconfig` with your Apple Team ID. 
Alternatively, you may setup signing within XCode opening `iosApp/iosApp.xcworkspace` and then 
using "Signing & Capabilities" tab of `iosApp` target. See also how to prepare XCode section below.

Then choose **iosApp** configuration in IDE and run it. 

## Run on desktop via Gradle

`./gradlew desktopApp:run`

## Run JS in browser with WebAssembly Skia via Gradle

`./gradlew jsApp:jsBrowserDevelopmentRun`

## Prepare Xcode to launch on iOS device

1) **Add your Apple ID.**  
Xcode -> Preferences... -> Accounts  
![apple-id.png](apple-id.png)  

2) **Create certificates with XCode in temporary project.**  
You can create a certificate in Xcode. Create a new iOS app in Xcode.  
File -> New -> Project  
Choose iOS, App:  
![ios-app.png](ios-app.png)  

3) **In next dialog set any product name and choose your teamID.**  

4) **Run project on iOS Device.**  
Xcode prompts you to install the certificate. Require password from login.keychain  

5) **Done**
After a successful launch on the device - you can delete this temporary project.  
Certificates will remain in login.keychain  

## Run native on MacOS
Choose **shared[macosX64]** or **shared[macosArm64]** configuration in IDE and run it.

