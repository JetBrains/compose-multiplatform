 ## Features

### Supported platforms
   * macOS (x86-64, arm64)
   * Windows (x86-64)
   * Linux (x86-64, arm64)
   * Web browsers

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

When the new version of Kotlin is released, the latest Compose Multiplatform version isn't supported yet.

But after some time we will release a version which supports the latest Kotlin.
Starting from 1.2.0, Compose Multiplatform supports multiple versions of Kotlin.

Compose version | Kotlin version
--- | ---
1.0.0 | 1.5.31
1.1.1 | 1.6.20
1.2.0 | 1.7.10 - 1.7.20 (Desktop, Android), 1.7.10 (JS)
