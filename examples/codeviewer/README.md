MPP Code Viewer example for desktop/android written in Multiplatform Compose library.

### Running desktop application
```
./gradlew :desktop:run
```

### Building native desktop distribution
```
./gradlew :desktop:package
# outputs are written to desktop/build/compose/binaries
```

### Installing Android application on device/emulator
```
./gradlew installDebug
```