# Lazy Grid Image Demo

This project demonstrates how to display a grid of images across multiple platforms: Android, iOS, and Desktop. 

The project includes three different implementations of the same functionality:

1. **Kotlin Multiplatform with Compose** - a shared implementation using Compose Multiplatform
2. **Native Android** - a pure Android implementation using Jetpack Compose
3. **Native iOS** - a pure iOS implementation using SwiftUI

The application displays 999 images in a LazyVerticalGrid (or equivalent on each platform) with 3 columns. 
The images are loaded asynchronously from local resources:

- In the Compose Multiplatform version, images are loaded using [Coil library](https://coil-kt.github.io/coil/) from the Compose Multiplatform resources
- In the native Android version, images are loaded using [Coil library](https://coil-kt.github.io/coil/) from the Android assets
- In the native iOS version, images are loaded using `AsyncImage` from the local file system

The project is used to compare Compose Multiplatform performance metrics with native counter-parts such us 
size, startup time, FPS, CPU/GPU usage, etc.

## Project Structure

* `/composeApp` - Contains the Kotlin Multiplatform implementation using Compose Multiplatform
  - `commonMain` - Code shared across all platforms
  - `androidMain` - Android-specific code
  - `iosMain` - iOS-specific code
  - `desktopMain` - Desktop-specific code

* `/nativeAndroidApp` - Contains a native Android implementation using Jetpack Compose
  - Uses Android's resource system to load images

* `/nativeiosApp` - Contains a native iOS implementation using SwiftUI
  - Uses iOS asset catalog to load images

## Downloading Images

This project includes a script to download 999 different images 
from [Picsum Photos](https://picsum.photos/) with 512x512 resolution:

`./download_images.sh`

Please run the script before building the project.

## Building and Running

### Compose Multiplatform App
- For Android: Run the `composeApp` configuration from Android Studio
- For iOS: Open the Xcode project in the `iosApp` directory
- For Desktop: Run the `desktopApp` configuration from IntelliJ IDEA or Android Studio

### Native Android App
- Open the `nativeAndroidApp` directory in Android Studio and run the app

### Native iOS App
- Open the `nativeiosApp/nativeiosApp.xcodeproj` in Xcode and run the app
