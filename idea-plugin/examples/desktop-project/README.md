1. From `<ROOT>/gradle-plugins`:
``` 
COMPOSE_VERSION=0.4.0-preview-annotation-build53
./gradlew publishToMavenLocal -Pcompose.version=$COMPOSE_VERSION -Pdeploy.version=$COMPOSE_VERSION
```
2. Run from `<ROOT>/idea-plugin`:
```
./gradlew runIde
```
3. Open this project in the test IDE.