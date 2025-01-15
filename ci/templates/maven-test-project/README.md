The purpose of this test project is to check if Compose Multiplatform is resolvable via pom files, which are used by JPS, which is used by IntelliJ

```
mvn exec:java -Dexec.mainClass="MainKt" -Dkotlin-version=2.0.21 -Dcompose-version=1.7.3
```

Known issues:
- Skiko isn't resolved, so it is not possible to run any Compose UI. Only Compose Runtime works