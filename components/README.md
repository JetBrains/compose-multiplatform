# Libraries for Compose Multiplatform

## Resources
Library to load resources, like images.

### How to run demo project:
*Prerequisites*: to run on iOS and Android, you should have "Kotlin Multiplatform Mobile" plugin installed either
in Android Studio or in AppCode with [installed CocoaPods](https://kotlinlang.org/docs/native-cocoapods.html).

### Run on desktop via Gradle:
`./gradlew :resources:demo:desktopApp:run`

### Run JS in browser with WebAssembly Skia via Gradle:
`./gradlew :resources:demo:shared:jsBrowserDevelopmentRun`

### Run MacOS via Gradle:
 - on Intel CPU: `./gradlew :resources:demo:shared:runDebugExecutableMacosX64`
 - on Apple Silicon: `./gradlew :resources:demo:shared:runDebugExecutableMacosArm64`

# Tests
Run script: 
```bash
./test.sh
```
