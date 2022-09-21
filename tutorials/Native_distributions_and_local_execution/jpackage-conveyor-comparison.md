# Packaging tools comparison 

Use [Conveyor](https://www.hydraulic.software) if you want any of the following features:

* Online updates. jpackage creates packages that users must update manually by reinstalling.
* Creation, signing and notarization of packages for all supported targets from any OS (e.g. your developer laptop, a CI machine). jpackage must be run on each platform you wish to support.
* Self-signed packages with `curl | bash` style installs, which don't require you to purchase signing certificates.
* Better support for JNI native libraries: 
  * jpackage doesn't know how to extract or sign native libraries bundled into library JARs, which can cause macOS notarization failures. 
  * Debian packages automatically get dependencies added to satisfy native libraries.  
* A generated static download page that detects the user's OS and CPU architecture.
* Conversion of icons from PNGs to the platform specific formats.
* Automatic size optimization of the bundled JVM using jdeps.
* Automatic support for screen readers using the Java Accessibility Bridge.
* JDK versions earlier than 15.
* Supplementary command line tools included with your package.
* Commercial support.

Use jpackage if you want:

* "Out of the box" integration with the Compose Gradle plugin.
* Distribution via the Mac App Store, RPMs or registration of file associations. These are not yet supported by Conveyor.
* DMG or PKG files for macOS. Conveyor generates zips - they extract much faster but don't support branding.
* User interaction during the install on Windows. Conveyor generates installs that don't ask the user any questions.
* An open source tool.
