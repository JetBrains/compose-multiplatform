# Getting Started with Compose for Desktop

## What is covered

In this tutorial we will create a simple desktop UI application
using the Compose UI framework.

## Prerequisites

 Compose for Desktop can produce applications for macOS, Linux and Windows platforms,
and so any of these platforms can be used for this tutorial.

The following software has to be preinstalled:
   * JDK 11 or later
   * IntelliJ IDEA Community Edition or Ultimate Edition 20.2 or later (other editors could be used, but we assume you are using IntelliJ IDEA in this tutorial)

## Creating a new project

Kotlin plugin has Compose new project wizard, but it is possible to create project manually.

The recommended way of building Compose for Desktop projects is by using Gradle.
JetBrains provides a simple way of building Compose for Desktop projects
using a special Gradle plugin.

One could clone an existing template [desktop](https://github.com/JetBrains/compose-jb/tree/master/templates/desktop-template) or
[multiplatform](https://github.com/JetBrains/compose-jb/tree/master/templates/multiplatform-template) application templates, or create it from scratch.

First create a new directory, named `sample`.
```shell script
mkdir sample
cd sample
```

Create `settings.gradle.kts` as follows:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
```
Then create `build.gradle.kts` with the following content:
```kotlin
import org.jetbrains.compose.compose

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.compose") version "0.1.0-m1-build62"
}

repositories {
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
```
Then create file `src/main/kotlin/main.kt` and put there:
```kotlin
import androidx.compose.desktop.Window
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun main() = Window(title = "Compose for Desktop", size = IntSize(300, 300)) {
    var count = remember { mutableStateOf(0) }
    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                   onClick = {
                       count.value++
                   }) {
                Text(if (count.value == 0) "Hello World" else "Clicked ${count.value}!")
            }
            Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                   onClick = {
                       count.value = 0
                    }) {
                Text("Reset")
            }
        }
    }
}
```
## Running the project

Open `build.gradle.kts` as a project in IntelliJ IDEA.

![New project](screen1.png)

After you download the Compose for Desktop dependencies from the Maven repositories your new project is ready
to go. Open the Gradle toolbar on the right, and select `sample/Tasks/applications/run`.
The first run may take some time, but afterwards following dialog will be shown:

![Application running](screen2.gif)

You can click on the button several times and see that application reacts and
updates the UI.
