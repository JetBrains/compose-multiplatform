[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Latest release](https://img.shields.io/github/v/release/JetBrains/compose-multiplatform?color=brightgreen&label=latest%20release)](https://github.com/JetBrains/compose-multiplatform/releases/latest)
[![Latest build](https://img.shields.io/github/v/release/JetBrains/compose-multiplatform?color=orange&include_prereleases&label=latest%20build)](https://github.com/JetBrains/compose-multiplatform/releases)



# Compose Multiplatform

[**Compose Multiplatform**](https://jb.gg/cmp) is a declarative framework for sharing UIs across multiple platforms with Kotlin. 
It is based on [Jetpack Compose](https://developer.android.com/jetpack/compose) and developed by [JetBrains](https://www.jetbrains.com/) and open-source contributors.

You can choose the platforms across which to share your UIs using Compose Multiplatform:

* [iOS](https://jb.gg/start-cmp) (Beta)
* [Android](https://jb.gg/start-cmp) 
* [Desktop](https://jb.gg/start-cmp) (Windows, MacOS, Linux)
* [Web](https://jb.gg/start-cmp) (Alpha)

For example, you can share UIs between iOS and Android or Windows and MacOS.

![Shared UIs of the iOS, Android, desktop, and web apps](artwork/readme/apps.png)

## iOS

> iOS support is in Beta. It is feature complete, and migration issues should be minimal. You may still encounter bugs, performance and developer experience issues, but not as much as in the Alpha stage.
> We would appreciate your feedback on it in the public Slack channel [#compose-ios](https://kotlinlang.slack.com/archives/C0346LWVBJ4/p1678888063176359). 
> If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).

Compose Multiplatform shares most of its API with Jetpack Compose, the Android UI framework developed by Google. 
You can use the same APIs to build user interfaces for both Android and iOS.

Since Compose is built on top of [Kotlin Multiplatform](https://jb.gg/kmp), 
you can easily access native APIs, such as the [Camera API](https://developer.apple.com/documentation/avfoundation/capture_setup/avcam_building_a_camera_app), 
and embed complex native UI views, such as [MKMapView](https://developer.apple.com/documentation/mapkit/mkmapview).

**[Get started with Compose Multiplatform](https://jb.gg/start-cmp)**

## Android

When Android is one of your targets, you can get the same experience for Android as if you were developing an Android app 
using [Jetpack Compose](https://developer.android.com/jetpack/compose).

**[Get started with Compose Multiplatform](https://jb.gg/start-cmp)**

## Desktop

Compose Multiplatform targets the JVM and supports high-performance hardware-accelerated UI rendering on all major desktop
platforms – macOS, Windows, and Linux.

It has desktop extensions for menus, keyboard shortcuts, window manipulation, and notification management.

**[Get started with Compose Multiplatform](https://jb.gg/start-cmp)**

> We would appreciate your feedback on Compose Multiplatform in the public Slack channel [#compose](https://kotlinlang.slack.com/archives/CJLTWPH7S/p1678882768039969).

## Web

> Web support is in Alpha. It may change incompatibly and require manual migration in the future.
> We would appreciate your feedback on it in the public Slack channel [#compose-web](https://kotlinlang.slack.com/archives/C01F2HV7868/p1678887590205449). 
> If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).

You can experiment with sharing your mobile or desktop UIs with the web. Compose for Web is based on [Kotlin/Wasm](https://kotl.in/wasm), 
the newest target for Kotlin Multiplatform projects. It allows Kotlin developers to run their code in the browser with 
all the benefits that WebAssembly has to offer, such as good and predictable performance for your applications.

**[Get started with Compose for Web](https://jb.gg/start-cmp)**

## Libraries

### Compose HTML

Compose HTML is a library targeting [Kotlin/JS](https://kotlinlang.org/docs/js-overview.html) that provides Composable building blocks 
for creating web user interfaces with HTML and CSS.    

> Note that Compose HTML is not a multiplatform library. It can be used only with Kotlin/JS.

## Learn more

* [FAQ](https://jb.gg/cmp-faq)
* [Samples](https://jb.gg/cmp-samples)
* [Tutorials](tutorials/README.md)
* [Compatibility and versioning](https://jb.gg/cmp-versioning)
* [Changelog](CHANGELOG.md)





