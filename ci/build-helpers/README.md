## Publish as a library
This library exists to help to publish artifacts to Maven Central for CMP projects and its dependencies.

For exmaple, for [Skiko](https://github.com/JetBrains/skiko/blob/29d227acb6893171940b120114ac1cbc95f50987/skiko/settings.gradle.kts#L19)
```
./gradlew publish
```

It is published automatically to https://maven.pkg.jetbrains.space/public/p/compose/internal on any change in the CMP sources by this CI task:
https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Compose_PublishBuildHelpers

## Publish as a local library
```
./gradlew publishToMavenLocal
```

## Use from sources
```
./gradlew -p=cli reuploadArtifactsToMavenCentral -Pmaven.central.sign=true \
  -Pmaven.central.coordinates=org.jetbrains.compose*:*:%version.COMPOSE%,org.jetbrains.compose.material:material-navigation*:%version.COMPOSE_MATERIAL_NAVIGATION% \
  -Pmaven.central.stage=org.jetbrains.compose \
  -Pmaven.central.description="Compose %version.COMPOSE% and associated libs" \
  -Pmaven.central.staging.close.after.upload=true
```