 ## Features

Features currently available in Compose for Desktop
   * [Scrollbars support](tutorials/Scrollbars/README.md)
   * [Image loading support](tutorials/Image_And_Icons_Manipulations/README.md)
   * [Keyboard handling](tutorials/Keyboard/README.md)
   * [Mouse clicks and move](tutorials/Mouse_Events/README.md)
   * [Packaging to native distributions](tutorials/Native_distributions_and_local_execution/README.md)
   * [Tray, menu bar and notifications](tutorials/Tray_Notifications_MenuBar/README.md)
   * [Window properties handling](tutorials/Window_API/README.md)

Follow individual tutorials to understand how to use particular feature.

### Limitations

Following limitations apply to Milestone 3 (M3) release.

  * Only 64-bit Windows is supported
  * Only JDK 11 or later is supported due to the memory management scheme used in Skia bindings
  * Some Linux distributions require additional packages to be instaled, see [this issue](https://github.com/JetBrains/compose-jb/issues/273) for more information
  

[comment]: <> (__SUPPORTED_GRADLE_VERSIONS__)

### Gradle plugin compatibility

* M1 works only with Gradle 6.4 and 6.5;
* M2 works only with Gradle 6.4 or later (6.7 is the latest tested version).
* M3 works only with Gradle 6.4 or later (6.8 is the latest tested version).
