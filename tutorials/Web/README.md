# Compose for Web

Compose for Web allows you to build reactive user interfaces for the web in Kotlin, using the concepts and APIs of Jetpack Compose to express the state, behavior, and logic of your application.

Compose for Web provides multiple ways of declaring user interfaces in Kotlin code, allowing you to have full control over your website layout with a declarative DOM API.

## Tutorials:

[Getting Started With Compose for Web](Getting_Started/README.md) - create and configure a simple project

[Building web UI](Building_UI/README.md) - overview of the basic features Compose for Web provides to build web UIs

[Handing Events](Events_Handling/README.md) - a short overview of Events handling with compose web

[Controlled and Uncontrolled inputs](Controlled_Uncontrolled_Inputs/README.md) - overview of Controlled and Uncontrolled inputs

[Style DSL](Style_Dsl/README.md) - about styling the composable components in web

[Using test-utils](Using_Test_Utils/README.md) - about unit testing for @Composable components based on HTML DOM

[Integration with Ktor](https://play.kotlinlang.org/hands-on/Full%20Stack%20Web%20App%20with%20Kotlin%20Multiplatform) - this is actually not a Compose tutorial, but since integration of Kotlin/JS with Ktor is very popular scenario, we decided to add it here

## Examples:
- Compose For Web [landing page](https://compose-web.ui.pages.jetbrains.team/). Also have a look at [source code](https://github.com/JetBrains/compose-jb/tree/master/examples/web-landing)
- Compose For Web and React integration -  [source code](https://github.com/JetBrains/compose-jb/tree/master/examples/web-with-react)
- Bird game - [source code](https://github.com/JetBrains/compose-jb/tree/master/examples/web-compose-bird)
- TODO app. (MPP: android, desktop, web) - [source code](https://github.com/JetBrains/compose-jb/tree/master/examples/todoapp-lite)
- Falling Balls game (uses deprecated widgets API) - [source code](https://github.com/JetBrains/compose-jb/tree/master/examples/falling-balls-web)

## What's included (modules):

### Module `compose.runtime`
It provides fundamental building blocks of Compose's programming model and state management.
`Compose for Web` uses Compose's runtime implementation.

**Dependency:**

``` kotlin
// it's assumed that plugin id("org.jetbrains.compose") is applied
dependencies {
    implementation(compose.runtime)
}
```

**Useful links:**

[Compose's runtime API reference and docs](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary)

### Module `compose.web.core`

It provides:
- DSL for building Composable components based on HTML DOM
- Comprehensive CSS-in-Kotlin/JS API

Please have a look at tutorials to learn more about `compose.web.core`

**Dependency:**

``` kotlin
// it's assumed that plugin id("org.jetbrains.compose") is applied
dependencies {
    implementation(compose.web.core)
}
```

### Module `compose.web.svg`

It provides a collection of Composable functions representing SVG elements.
These functions can be used to build Composable web UI components based on SVG.

**Experimental module:** The API in this module is not stabilized yet and breaking changes can be expected.

**Dependency:**

``` kotlin
// it's assumed that plugin id("org.jetbrains.compose") is applied
dependencies {
    implementation(compose.web.svg)
}
```

**Useful links:**
- [https://developer.mozilla.org/en-US/docs/Web/SVG](https://developer.mozilla.org/en-US/docs/Web/SVG])

### Module `compose.web.testUtils`
It provides a few util functions to simplify the unit testing of Composable components based on HTML DOM.

**Dependency:**

``` kotlin
// it's assumed that plugin id("org.jetbrains.compose") is applied
sourceSets {
    val jsTest by getting {
        implementation(kotlin("test-js"))
        implementation(compose.web.testUtils)
    }
}
```

**Useful links:**
- [Using test-utils](Using_Test_Utils/README.md) - tutorial

### Module `compose.web.widgets` (Deprecated)
It provides a collection of Composable components (based on compose.web.core) which try to conform to the API and behaviour of some widgets from Jetpack Compose UI:
`Column`, `Row`, etc.

## Important notes

#### Kotlin MPP gradle plugin needs to be applied
``` kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}
```
Compose for Web can be used only with kotlin multiplatform plugin applied. Otherwise, Compose plugin won't be configured properly. 
