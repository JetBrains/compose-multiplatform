### Compose Uber Jar

Merges all `org.jetbrains.compose*` artifacts into one uber jar `compose-full.jar`.

### Building

Specify a version of compose artifacts to merge:
* by providing version directly via `compose.version` property  
(e.g. `./gradlew -Pcompose.version=0.1.0-build49 ...`);
* or by providing a path to file containing a version via `compose.version.file` property
(e.g. `./gradlew -Pcompose.version.file=version.txt ...`);
* `compose.version.file` has a higher priority than `compose.version`.

Build a jar locally by running:
```
./gradlew shadowJar
```
The jar will be available at `build/libs/compose-full.jar`

### Publishing

```
export COMPOSE_REPO_USERNAME=<COMPOSE_REPO_USERNAME>
export COMPOSE_REPO_KEY=<COMPOSE_REPO_KEY>
./gradlew publishToComposeRepo
```