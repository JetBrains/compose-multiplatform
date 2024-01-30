# Compose Multiplatform Development

## Core

Compose Multiplatform development is going
in [compose-multiplatform-core](https://github.com/JetBrains/compose-multiplatform-core) repository.
There Compose Multiplatform team and contributors adopt Jetpack Compose for iOS, Desktop and Web targets.

### [Get started](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/MULTIPLATFORM.md)

## Skiko

Compose Multiplatform uses [Skiko](https://github.com/JetBrains/skiko). A low-level library that hides platform
complexity and provides a simple interface for rendering, event handling, window management, and other features. Skiko
uses Skia as graphical API.

## Other parts

- [Gradle Plugin](https://github.com/JetBrains/compose-multiplatform/tree/master/gradle-plugins)
- [IDEA Plugin](https://github.com/JetBrains/compose-multiplatform/tree/master/idea-plugin)
- [Examples](https://github.com/JetBrains/compose-multiplatform/tree/master/examples)

## Publishing

Compose Multiplatform libraries can be published to local Maven with the following steps:

1. Set `COMPOSE_CUSTOM_VERSION` environment variable

```bash
export COMPOSE_CUSTOM_VERSION=0.0.0-custom-version
```

2. Publish core libraries
   using [instructions](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/MULTIPLATFORM.md#publishing)
3. Publish Gradle plugin

```bash
./scripts/publishGradlePluginToMavenLocal
```

4. Publish additional components
```bash
./scripts/publishComponentsToMavenLocal
```

5. Publish Compose HTML library
```bash
./scripts/publishHtmlLibraryToMavenLocal
```