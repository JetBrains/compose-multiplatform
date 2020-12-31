# Widget gallery

This example is derived from
[ComposeCookBook](https://github.com/Gurupreet/ComposeCookBook) project
by Gurupreet Singh ([@Gurupreet](https://github.com/Gurupreet))
published under [MIT license](third_party/ComposeCookBook_LICENSE.txt).

An example of Compose application for Desktop and Android platforms, 
demonstrating how to use various Material widgets.

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
