# Development for Android 

## What is covered

This tutorial covers topic of using Compose Multiplatform for multiplatform build that includes Android

## Intro

Compose Multiplatform doesn't contain any Android artifacts. Instead it references Jetpack Compose Android artifacts published by Google

## Usage

Jetpack Compose interoperability is enabled on publication level - nothing needs to be explicitly enabled. 
The easiest way to start is to use Kotlin Project Wizard with Compose Multiplatform template - one of the target platfrom is Android.  

If you want to create it manually, the following needs to be done:  
- Create a Kotlin multiplatform project
- Enable Kotlin Gradle plugin (kotlin("multiplatform"))
- Enable Compose Multiplatform Gradle plugin (id("org.jetbrains.compose"))
- Add common Compose dependency to the Common module (e.g. api(compose.runtime)) 

This is it. During compilation for Android platform, Jetpack Compose artifacts will be picked automatically. 

## Versioning

Compose Multiplatform 1.0.0 references Jetpack Compose 1.1.0-beta02. If you want to use higher version, you could explicitly 
add Jetpack Compose dependencies to the Android module. However please note, that Kotlin Compiler compatibility should be considered.  

