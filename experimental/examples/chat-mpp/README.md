# Chat example app

## Run on Android:
- connect device or emulator
- `./gradlew android:installDebug`
- open app

## Run on Desktop jvm
  `./gradlew client:run`

## Run native on MacOS
  `./gradlew client:runDebugExecutableMacosX64` (Works on Intel processors)

## Run web assembly in browser
  `./gradlew client:jsBrowserDevelopmentRun`

## Run on iOS simulator
  `./gradlew client:iosDeployIPhone8Debug`
  `./gradlew client:iosDeployIPadDebug`

## Run on iOS device
- Read about iOS target in [falling-balls-mpp/README.md](../falling-balls-mpp/README.md)
- `./gradlew client:iosDeployDeviceRelease`
