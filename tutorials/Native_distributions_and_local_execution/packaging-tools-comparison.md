# Packaging tools comparison 

The built-in Gradle tasks cover basic packaging needs:

* Creation of MSI files or NSIS installer EXEs for Windows, signed app bundles for macOS and DEB/RPM packages for Linux.
* Bundling of the JVM using `jlink`.
* Customization of packaging options on each OS.

Conveyor ([docs](https://conveyor.hydraulic.dev/)) is a tool available for download from [Hydraulic](https://www.hydraulic.software). It integrates with the Compose Gradle plugin and is useful if you want any of the following things:

* **Online updates.** The Compose plugin creates packages that users must update manually by reinstalling. Conveyor generates packages that can silently update themselves in the background on Windows/macOS, or which use apt/etc on Linux. 
* **Cross-building.** Creation, signing and notarization of packages for all supported targets from any OS (e.g. your developer laptop, a CI machine). The built-in tasks must be run from each OS you target.
* **Self-signed packages.** These don't require you to purchase signing certificates, but require the user to copy/paste terminal commands.
* **Download pages.** Generated static HTML that detects the user's OS and CPU architecture ([example](https://conveyor.hydraulic.dev/latest/#compose-multiplatform)).
* **Icon conversion.** The built in tasks require you to convert icons to platform specific formats manually.
* **Size optimization.** `jdeps` is used to shrink the download by stripping unused JDK modules.
* **Accessibility.** Automatic support for screen readers is added using the Java Accessibility Bridge.
* **Happy IT departments.** Conveyor uses MSIX, Microsoft's current-gen Windows 10/11 packaging system, which is deeply integrated with Windows network admin tools.
* **CLI support.** Supplementary command line tools included with your package.
* **Commercial support.**

This isn't a complete list of features, we suggest checking their docs to see what's available. It's free for open source projects, but after the introductory period ends it will require a license for commercial projects (see their [pricing page](https://www.hydraulic.software/pricing.html) for details).
