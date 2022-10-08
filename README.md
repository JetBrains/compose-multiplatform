[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Latest release](https://img.shields.io/github/v/release/JetBrains/compose-jb?color=brightgreen&label=latest%20release)](https://github.com/JetBrains/compose-jb/releases/latest)
[![Latest build](https://img.shields.io/github/v/release/JetBrains/compose-jb?color=orange&include_prereleases&label=latest%20build)](https://github.com/JetBrains/compose-jb/releases)

# Compose Multiplatform, by JetBrains
![](artwork/readme/apps.png)
Compose Kotlin UI framework port for desktop platforms (macOS, Linux, Windows) and Web, components outside of the core [Compose repository](https://android.googlesource.com/platform/frameworks/support).

Preview functionality (check your application UI without building/running it) for desktop platforms is available via [IDEA plugin](https://plugins.jetbrains.com/plugin/16541-compose-multiplatform-ide-support).

## Tutorials
### Compose for Desktop
* [Getting started](tutorials/Getting_Started)
* [Image and icon manipulations](tutorials/Image_And_Icons_Manipulations)
* [Mouse events and hover](tutorials/Mouse_Events)
* [Scrolling and scrollbars](tutorials/Desktop_Components#scrollbars)
* [Tooltips](tutorials/Desktop_Components#tooltips)
* [Context Menu](tutorials/Context_Menu/README.md)
* [Top level windows management](tutorials/Window_API_new)
* [Menu, tray, notifications](tutorials/Tray_Notifications_MenuBar_new)
* [Keyboard support](tutorials/Keyboard)
* [Tab focus navigation](tutorials/Tab_Navigation)
* [Swing interoperability](tutorials/Swing_Integration)
* [Navigation](tutorials/Navigation)
* [Accessibility](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Accessibility)
* [Building a native distribution](tutorials/Native_distributions_and_local_execution)

Also, see [Foundation](https://developer.android.com/jetpack/compose/documentation#core) and [Design](https://developer.android.com/jetpack/compose/documentation#design) docs from Google. They were originally written for Android, but most of information applies to Compose for Desktop as well.

### Compose for Web
* [Getting started with Compose for Web](tutorials/Web/Getting_Started) 
* [Building web UI](tutorials/Web/Building_UI)
* [Handling Events](tutorials/Web/Events_Handling)
* [Controlled and Uncontrolled inputs](tutorials/Web/Controlled_Uncontrolled_Inputs)
* [Style DSL](tutorials/Web/Style_Dsl)
* [Using test-utils](tutorials/Web/Using_Test_Utils)

### Jetpack Compose for Android
Compose Multiplatform uses Jetpack Compose developed by Google when you target Android platform. See more info about it [here](tutorials/Development_for_Android).

[The docs](https://developer.android.com/jetpack/compose/documentation) published by Google are great and decribe how to develop on Compose for Android.

Note that when you use Compose Multiplatform, you setup your project differently. You can create a multiplatform project with Android support via IDEA Project Wizard, or by copying [multiplatform template](https://github.com/JetBrains/compose-jb/tree/master/templates/multiplatform-template). In `androidMain` source set you can use almost all information from the docs, and in `commonMain` source set you can use information from Foundation and Design sections.

## Examples
   * [codeviewer](examples/codeviewer) - File Browser and Code Viewer application for Android and Desktop
   * [imageviewer](examples/imageviewer) - Image Viewer application for Android and Desktop
   * [issues](examples/issues) - GitHub issue tracker with an adaptive UI and ktor-client
   * [Falling Balls](examples/falling-balls) - Simple game
   * [compose-bird](examples/web-compose-bird) - A flappy bird clone using Compose for Web
   * [notepad](examples/notepad) - Notepad, using the new experimental Composable Window API
   * [todoapp](examples/todoapp) - TODO items tracker with persistence and multiple screens, written with external navigation library
   * [todoapp-lite](examples/todoapp-lite) - A simplified version of [todoapp](examples/todoapp), fully based on Compose
   * [widgets gallery](examples/widgets-gallery) - Gallery of standard widgets
   * [IDEA plugin](examples/intellij-plugin) - Plugin for IDEA using Compose for Desktop

## Other ##
* [artwork](artwork) - design artifacts
* [benchmarks](benchmarks) - collection of benchmarks
* [compose](compose) - composite build of [Compose Multiplatform sources](https://github.com/JetBrains/androidx)
* [ci](ci) - Continuous Integration helpers
* [gradle-plugins](gradle-plugins) - a plugin, simplifying usage of Compose Multiplatform with Gradle
* [templates](templates) - new application templates
* [components](components) - custom components of Compose Multiplatform
   * [Split Pane](components/SplitPane)
* [experimental](experimental) - experimental components and examples
   * [cef](experimental/cef) - CEF integration in Jetpack Compose (somewhat outdated)
   * [Video Player](experimental/components/VideoPlayer)
   * [LWJGL integration](experimental/lwjgl-integration) - An example showing how to integrate Compose with [LWJGL](https://www.lwjgl.org)
   * [CLI example](experimental/build_from_cli) - An example showing how to build Compose without Gradle
       
## Versions ##

* [The latest stable release](https://github.com/JetBrains/compose-jb/releases/latest)
* [The latest dev release](https://github.com/JetBrains/compose-jb/releases)
* [Compatability and versioning overview](VERSIONING.md)
* [Changelog](CHANGELOG.md)
