# Compose for Desktop.

## Basic information

Desktop port samples and support files. macOS, Windows and Linux JVM platforms are currently
supported. See https://github.com/jetbrains/compose-jb for information, documentation and tutorials.

## Building

Desktop port requires build in Kotlin Multiplatform mode, so when building please specify
`-Pandroidx.compose.multiplatformEnabled=true` flag.


## Running an example

To run an example:

    ./gradlew   :compose:desktop:desktop:desktop-samples:run \
    -Pandroidx.compose.multiplatformEnabled=true
