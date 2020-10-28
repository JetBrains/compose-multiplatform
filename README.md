# Jetpack Compose
## Intro
Jetpack Compose is a suite of libraries within the AndroidX ecosystem. For more information, see our [project page](https://developer.android.com/jetpackcompose)

## Syntax
Jetpack Compose uses composable functions instead of XML layouts to define UI components. You can
see this in action in the demos, like `androidx.compose.material.demos.ButtonDemo.kt`. More
information can be found in the [compiler README](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/compose/compiler/README.md).

## Compiler
Composable functions are built using a custom Kotlin compiler plugin. More information about the
compiler plugin is available in [this README](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/compose/compiler/README.md).

## Getting started
To try out Jetpack Compose you need to set up the toolchain for AndroidX development. Follow the process [here](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/README.md) to check out the code.

To start the required version of Android Studio, you need to run the `ANDROIDX_PROJECTS=COMPOSE ./gradlew studio`

    cd path/to/checkout/frameworks/support/
    ANDROIDX_PROJECTS=COMPOSE ./gradlew studio

Also if you would like to build from the command line, all gradle commands need to be run from the
`frameworks/support` folder.  E.g. to build the demo app, run:

    cd path/to/checkout/frameworks/support/
    ./gradlew :compose:integration-tests:demos:installDebug

## Currently available components
Jetpack Compose is in very early stages of development. Developers wanting to build sample apps will probably want to include the material, layout and framework modules. You can see how to setup your dependencies in `material/integration-tests/material-studies/build.gradle`.

Run the `demos` app to see examples of individual components.

A sample implementation of the [Material Rally app](https://material.io/design/material-studies/rally.html) is under `material/integration-tests/material-studies`. This can be viewed from inside the `demos` app, under the 'Material Studies' section.

## Structure
Library code for Jetpack Compose lives under the `frameworks/support/compose` directory. Additionally, sample code can be found within each module in the `integration-tests` subdirectories.

## Feedback
To provide feedback or report bugs, please refer to the main [AndroidX contribution guide](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/README.md) and report your bugs [here](https://issuetracker.google.com/issues/new?component=612128)

[Release notes](https://developer.android.com/jetpack/androidx/releases/compose)

[Browse source](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-master-dev:compose/)

[Existing open bugs](https://issuetracker.google.com/issues?q=componentid:612128%20status:open)

[File a new bug](https://issuetracker.google.com/issues/new?component=612128)
