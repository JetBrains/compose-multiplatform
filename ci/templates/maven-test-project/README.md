The purpose of this test project is to check if Compose Multiplatform is resolvable via pom files, which are used by JPS, which is used by IntelliJ

```
mvn install exec:java -Dexec.mainClass="MainKt" -Dkotlin.version=2.1.0 -Dcompose.version=1.8.0-alpha02
```
