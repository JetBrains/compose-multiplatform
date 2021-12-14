# Development for Android 

## What is covered

This tutorial covers topic of using Compose Multiplatform for multiplatform build that includes Android

## Intro

Compose Multiplatform build doesn't contain any Android artifacts. 
Instead it references Jetpack Compose Android artifacts [published by Google](https://developer.android.com/jetpack/compose).
This way we ensure that libraries built for Android using Jetpack libraries are compatible with Compose Multiplatform.

## Usage

Jetpack Compose interoperability is enabled on the publication level - nothing needs to be explicitly enabled. 


The easiest way to start is to use Kotlin Project Wizard with Compose Multiplatform template - one of the target platform is Android. 


To see how it could be achieved see the [multiplatform template](https://github.com/JetBrains/compose-jb/tree/master/templates/multiplatform-template).


## Versioning

Compose Multiplatform `1.0.0` references Jetpack Compose `1.1.0-beta02`. If one wants to use a different version, they could explicitly 
override Jetpack Compose dependencies to the Android module (or to the androidMain sourceset in MPP module) like this:

``` kotlin
dependencies {
    implementation("androidx.compose.material:material:1.1.0-beta04")
}
```


However please note, that Kotlin Compiler version used for Android must match.  

## Android Gradle Plugin (AGP) version

For multiplatform Android modules using AGP `4.1.3` with the block like
``` kotlin
dependencies {
    classpath("com.android.tools.build:gradle:4.1.3")
}
```
is recommended. We're working on providing compatibility with more recent AGP versions, see [KT-49835](https://youtrack.jetbrains.com/issue/KT-49835) and 
[KT-49789](https://youtrack.jetbrains.com/issue/KT-49798). Also if one uses IntelliJ IDEA bundled Android plugin may have compatibility issues with recent AGP. 

## Useful reading about porting Android apps to Desktop

[Porting ViewModel](https://github.com/JetBrains/compose-jb/discussions/1587)
