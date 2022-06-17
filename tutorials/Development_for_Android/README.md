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

Compose Multiplatform `1.1.1` references Jetpack Compose `1.1.0`. If one wants to use a different version, they could explicitly 
override Jetpack Compose dependencies to the Android module (or to the androidMain sourceset in MPP module) like this:

``` kotlin
dependencies {
    implementation("androidx.compose.material:material:1.2.0-beta01")
}
```

## Useful reading about porting Android apps to Desktop

[Porting ViewModel](https://github.com/JetBrains/compose-jb/discussions/1587)
