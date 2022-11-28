MPP Code Viewer example for desktop/android written in Multiplatform Compose library.

### Running desktop application

* To run, launch command: `./gradlew :desktop:run`
* Or choose **desktop** configuration in IDE and run it.  
  ![desktop-run-configuration.png](screenshots/desktop-run-configuration.png)

### Building native desktop distribution
```
./gradlew :desktop:packageDistributionForCurrentOS
# outputs are written to desktop/build/compose/binaries
```

### Installing Android application on device/emulator
```
./gradlew installDebug
```

![Desktop](screenshots/codeviewer.png)