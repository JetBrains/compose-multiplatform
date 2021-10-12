A simplified version of the [TodoApp example](https://github.com/JetBrains/compose-jb/tree/master/examples/todoapp), fully based on Jetpack Compose and without using any third-party libraries.

Supported targets: Android and Desktop.

### Running desktop application
```
./gradlew :desktop:run
```

### Building native desktop distribution
```
./gradlew :desktop:package
# outputs are written to desktop/build/compose/binaries
```

### Running Android application

Open project in IntelliJ IDEA or Android Studio and run "android" configuration.

![Desktop](screenshots/todoapplite.png)
