# Compose Multiplatform FAQ

## What is Compose Multiplatform?

Compose Multiplatform is a modern declarative and reactive UI framework that provides a simple way to build user 
interfaces with a small amount of Kotlin code. It also allows you to write your UI once and run it on any of the supported
platforms – iOS, Android, desktop (Windows, macOS, Linux), and web.

## How does it relate to Jetpack Compose for Android?

Compose Multiplatform shares most of its API with [Jetpack Compose](https://developer.android.com/jetpack/compose), the Android UI framework developed by Google. 
In fact, when you are using Compose Multiplatform to target Android, your app simply runs on Jetpack Compose.

Other platforms targeted by Compose Multiplatform may have implementations under the hood that differ from those of 
Jetpack Compose on Android, but they still provide you with the same APIs.

## Between which platforms can I share my UI?

We want you to have the option to share your UI between any combination of popular platforms – Android, iOS, desktop 
(Linux, macOS, Windows), and web. Note, however, that only Compose for Android and Desktop are Stable at the moment.

## Can I use Compose Multiplatform in production?

The Android and Desktop targets of Compose Multiplatform are Stable. You can use them in production.

The iOS target is in Alpha, and we don’t recommend using it in production. Nevertheless, you are welcome to experiment 
with it at your own risk to see what benefits you get and how your application will look in the future.

The version of Compose for Web that is based on WebAssembly, and that has the same UI as Compose for iOS, Android, and 
Desktop, is Experimental. You can try it in your pet projects.

## When will Compose Multiplatform become Stable? What are the current stability guarantees?

Compose Multiplatform is Stable for Android and Desktop, while the iOS and Web targets are not Stable yet. The iOS target 
is in Alpha, and Web is Experimental.

Stable means that the framework provides a comprehensive API surface that allows you to write beautiful, production-ready
applications, without encountering performance or correctness issues in the framework itself. API-breaking changes can 
only be made 2 versions after an official deprecation announcement.

Alpha means that we have decided to productize the idea but it hasn't reached the final shape yet. You can use it at your 
own risk and expect migration issues.

Experimental means the framework is under development. It lacks some features and might have performance issues and bugs.
Some aspects might change in the future, and breaking changes can occur often.

The framework is built using [Kotlin Multiplatform](https://kotlinlang.org/lp/multiplatform/), which is not itself Stable yet. 
Because of that, you may encounter issues during builds.

## What IDE should I use for building apps with Compose Multiplatform?

We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for desktop-only, web-only, and combined desktop-web 
Multiplatform applications. If you target Android or Android and iOS, we recommend using [Android Studio](https://developer.android.com/studio).

## Where can I find documentation and tutorials?

You can find the Getting Started tutorials on the [main project page](https://github.com/JetBrains/compose-multiplatform#readme) 
and more tutorials on [this page](tutorials/README.md).

## Can I play with a demo application? Where can I find it?

Sure, we have [multiple examples for all platforms](examples/README.md).

## Does Compose Multiplatform come with widgets?

Yes, Compose Multiplatform provides the full support for [Material 3](https://m3.material.io/) widgets.

## To what extent can I customize the appearance of Material widgets?

You can use Material’s theming capabilities to customize colors, fonts, and paddings. If you want to create a unique 
design, you can create completely custom widgets and layouts.

## How does Compose Multiplatform work with Kotlin Multiplatform?

[Kotlin Multiplatform technology](https://kotlinlang.org/lp/multiplatform/) simplifies the development of cross-platform projects. 
It reduces time spent writing and maintaining the same code for different platforms while retaining the flexibility and 
benefits of native programming.

For business logic, Kotlin Multiplatform is already a well-established and effective approach. Compose Multiplatform 
completes the puzzle when you don’t want to build and maintain separate native UIs for your target platforms. Maybe you 
don’t have enough people, or perhaps you don’t have enough time. Maybe you just really want to get your app out the door 
and into the hands of as many people as quickly as possible.

Compose Multiplatform gives you the option to push the sharing capabilities of Kotlin Multiplatform beyond business logic. It allows you to implement your user interface once and then use it for all the platforms you target.

## Can I share the UI in my existing Kotlin Multiplatform app?

If your application uses a native API for its UI (which is the most popular case), you can indeed gradually rewrite some 
parts to Compose Multiplatform, as it provides interoperability for that. You can remove some parts from native UIs and 
replace them with a special interop view that wraps a common UI written with Compose.

## I have an existing Android application that uses Jetpack Compose. What should I do to run it on other platforms?

First, you must separate common code from platform-specific logic, such as interactions with Android APIs or uses of 
Android-only dependencies. Next, you need to implement this logic on the new platform, either by using a multiplatform 
library or writing platform-specific code.

## Can I integrate Compose screens into an existing iOS app?

Yes. Compose Multiplatform supports different integration scenarios.

## Can I integrate UIKit or SwiftUI components into a Compose screen?

Yes, you can.

## What happens when my mobile OS updates and introduces new platform capabilities?

You can use them in platform-specific parts of your codebase as soon as they’re introduced. All new Android capabilities 
provide Kotlin or Java APIs, and wrappers over iOS APIs are generated automatically.

## What happens when my mobile OS updates and changes the visual style of the system components or their behavior?

Your UI will stay the same after OS updates because all the components are drawn on a canvas. If you embed native iOS 
components into your screen, updates may affect their appearance.
