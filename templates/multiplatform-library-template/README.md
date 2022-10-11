Compose Multiplatform Application

**Common**
- `./gradlew publishToMavenLocal` - publish the library to the local Maven repository (`~/.m2`)

- `./gradlew publish -Ppublish.url=http://repo2.mycompany.com/maven2 -Ppublish.username=user -Ppublish.password=pass` - publish the library to the external maven repository.

**Desktop**
- `./gradlew run` - run desktop demo application

**Android**
- `./gradlew installDebug` - install Android demo application on an Android device (on a real device or on an emulator)