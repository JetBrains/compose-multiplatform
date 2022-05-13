 ## Features

### Supported platforms
   * macOS (x86-64, arm64)
   * Windows (x86-64)
   * Linux (x86-64, arm64)
   * Web browsers

### Features currently available in Compose for Desktop
   * [Intro](tutorials/Getting_Started)
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
   * [Context Menu](tutorials/Context_Menu/README.md)  

### Features currently available in Compose for Web
   * [Intro: HTML DOM DSL](tutorials/Web/Building_UI/README.md)
   * [Event handling](tutorials/Web/Events_Handling/README.md)
   * [Style DSL](tutorials/Web/Style_Dsl/README.md)
   * [Controlled and Uncontrolled Inputs](tutorials/Web/Controlled_Uncontrolled_Inputs/README.md)
   * [Test Utils](tutorials/Web/Using_Test_Utils/README.md)


Follow individual tutorials to understand how to use particular feature.

### Limitations

Following limitations apply to 1.0 release.

  * Only 64-bit x86 Windows is supported
  * Only JDK 11 or later is supported due to the memory management scheme used in Skia bindings
  * Only JDK 15 or later is supported for packaging native distributions due to jpackage limitations

Knowing issues on older versions:
- OpenJDK 11.0.12 has [an issue](https://github.com/JetBrains/compose-jb/issues/940), when we switch keyboard layout on MacOs (isn't reproducible in OpenJDK 11.0.15)
  
[comment]: <> (__SUPPORTED_GRADLE_VERSIONS__)

### Kotlin compatibility

Compose version | Kotlin version
--- | ---
1.0.0+ | 1.5.31
1.0.1-rc2+ | 1.6.10


### Gradle plugin compatibility

* 1.0.0 works with Gradle 6.7 or later (7.2 is the latest tested version).
