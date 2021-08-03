 ## Features

### Supported platforms
   * macOS (x86-64, arm64)
   * Windows (x86-64)
   * Linux (x86-64, arm64)
   * Web browsers

### Features currently available in Compose for Desktop
   * [Desktop Components](tutorials/Desktop_Components/README.md)
   * [Image loading support](tutorials/Image_And_Icons_Manipulations/README.md)
   * [Keyboard handling](tutorials/Keyboard/README.md)
   * [Mouse clicks and move](tutorials/Mouse_Events/README.md)
   * [Packaging to native distributions](tutorials/Native_distributions_and_local_execution/README.md)
   * [Signing and notarization](tutorials/Signing_and_notarization_on_macOS/README.md)
   * [Swing interoperability](tutorials/Swing_Integration/README.md)
   * [Keyboard navigation](tutorials/Tab_Navigation/README.md)
   * [Tray, menu bar and notifications](tutorials/Tray_Notifications_MenuBar_new/README.md)
   * [Window properties handling](tutorials/Window_API_new/README.md)

### Features currently available in Compose for Web
   * [Intro](tutorials/Web/Building_UI/README.md)
   * [Event handling](tutorials/Web/Events_Handling/README.md)
   * [CSS](tutorials/Web/Style_Dsl/README.md)


Follow individual tutorials to understand how to use particular feature.

### Limitations

Following limitations apply to Alpha release.

  * Only 64-bit x86 Windows is supported
  * Only JDK 11 or later is supported due to the memory management scheme used in Skia bindings
  * Only JDK 15 or later is supported for packaging native distributions due to jpackage limitations
  * Some Linux distributions require additional packages to be installed, see [this issue](https://github.com/JetBrains/compose-jb/issues/273) for more information
  
[comment]: <> (__SUPPORTED_GRADLE_VERSIONS__)

### Gradle plugin compatibility

* M1 works only with Gradle 6.4 and 6.5;
* M2 works only with Gradle 6.4 or later (6.7 is the latest tested version).
* M3 works only with Gradle 6.4 or later (6.8 is the latest tested version).
* Alpha works with Gralde 6.7 or later (7.1 is the latest tested version).
