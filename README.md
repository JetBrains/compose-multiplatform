[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Latest release](https://img.shields.io/github/v/release/JetBrains/compose-jb?color=brightgreen&label=latest%20release)](https://github.com/JetBrains/compose-jb/releases/latest)
[![Latest build](https://img.shields.io/github/v/release/JetBrains/compose-jb?color=orange&include_prereleases&label=latest%20build)](https://github.com/JetBrains/compose-jb/releases)

# Compose for Desktop and Web, by JetBrains
![](artwork/readme/apps.png)
Compose Kotlin UI framework port for desktop platforms (macOS, Linux, Windows), components outside of the core Compose repository
at https://android.googlesource.com/platform/frameworks/support.

## Repository organization ##

   * [artwork](artwork) - design artifacts
   * [benchmarks](benchmarks) - collection of benchmarks
   * [compose](compose) - composite build of [Compose-jb sources](https://github.com/JetBrains/androidx)
   * [ci](ci) - Continuous Integration helpers
   * [cef](cef) - CEF integration in Jetpack Compose
   * [examples](examples) - examples of multiplatform Compose applications for Desktop and Android
       * [codeviewer](examples/codeviewer) - File Browser and Code Viewer application for Android and Desktop
       * [imageviewer](examples/imageviewer) - Image Viewer application for Android and Desktop
       * [issues](examples/issues) - GitHub issue tracker with an adaptive UI and ktor-client
       * [game](examples/falling_balls) - Simple game
       * [game](examples/falling_balls_with_web) - Simple game for web target
       * [compose-bird](examples/compose-bird) - A flappy bird clone using Compose for Web
       * [notepad](examples/notepad) - Notepad, using the new experimental Composable Window API
       * [todoapp](examples/todoapp) - TODO items tracker with persistence and multiple screens
       * [widgetsgallery](examples/widgetsgallery) - Gallery of standard widgets
       * [IDEA plugin](examples/intelliJPlugin) - Plugin for IDEA using Compose for Desktop      
   * [gradle-plugins](gradle-plugins) - a plugin, simplifying usage of Compose with Gradle
   * [templates](templates) - new application templates (see `desktop-template/build_and_run_from_cli_example.sh` for using without Gradle)
   * [tutorials](tutorials) - tutorials on using Compose for Desktop
       * [Getting started](tutorials/Getting_Started)
       * [Image and icon manipulations](tutorials/Image_And_Icons_Manipulations)
       * [Mouse events and hover](tutorials/Mouse_Events)
       * [Scrolling and scrollbars](tutorials/Desktop_Components)
       * [Tooltips](tutorials/Desktop_Components#tooltips)
       * [Top level windows management](tutorials/Window_API)
       * [Top level windows management (new Composable API, experimental)](tutorials/Window_API_new)
       * [Menu, tray, notifications](tutorials/Tray_Notifications_MenuBar)
       * [Menu, tray, notifications (new Composable API, experimental)](tutorials/Tray_Notifications_MenuBar_new)
       * [Keyboard support](tutorials/Keyboard)
       * [Building native distribution](tutorials/Native_distributions_and_local_execution)
       * [Signing and notarization](tutorials/Signing_and_notarization_on_macOS)
       * [Swing interoperability](tutorials/Swing_Integration)
       * [Navigation](tutorials/Navigation)
   * [components](components) - custom components of Compose for Desktop
       * [Video Player](components/VideoPlayer)
       * [Split Pane](components/SplitPane)
       
## Getting latest version of Multiplatform Compose ##

See https://github.com/JetBrains/compose-jb/tags for the latest build number.
