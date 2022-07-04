# Chat example app

## Run on Android:
- connect device or emulator
- `./gradlew installDebug`
- open app

## Run on Desktop jvm
  `./gradlew run`

## Run native on MacOS
  `./gradlew runDebugExecutableMacosX64` (Works on Intel processors)

## Run web assembly in browser
  `./gradlew jsBrowserDevelopmentRun`

## Run on iOS simulator
  `./gradlew iosDeployIPhone8Debug`
  `./gradlew iosDeployIPadDebug`

## Run on iOS device
- Read about iOS target in [falling-balls-mpp/README.md](../falling-balls-mpp/README.md)
- `./gradlew iosDeployDeviceRelease`
