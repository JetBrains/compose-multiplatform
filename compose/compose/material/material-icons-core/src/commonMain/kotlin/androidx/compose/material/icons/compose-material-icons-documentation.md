# Module root

Compose Material Icons

# Package androidx.compose.material.icons

This is the entry point for using Material Icons in Compose, designed to provide icons that match those described at <a href="https://fonts.google.com/icons" class="external" target="_blank">fonts.google.com/icons</a>.

![Icons image](https://developer.android.com/images/reference/androidx/compose/material/icons/iconography.png)

In this page, you'll find documentation for types, properties, and functions available in the `androidx.compose.material.icons` package.

The most commonly used set of Material icons are provided by `androidx.compose.material:material-icons-core` - this library is also provided as an API dependency by `androidx.compose.material:material`.

A separate library, `androidx.compose.material:material-icons-extended`, contains the full set of Material icons. Due to the very large size of this library, make sure to use R8/Proguard to strip unused icons if you are including this library as a direct dependency. Alternatively you can make a local copy (by copy and pasting) the icon(s) you wish to keep, or using Android Studio's 'Import vector asset' feature.

## Overview

### System icons

<a href="https://material.io/design/iconography/system-icons.html" class="external" target="_blank">System icons</a> symbolize common actions, files, devices, and directories.

|      | **APIs** | **Description** | **Preview** |
| ---- | -------- | --------------- | :---------: |
| **Icons** | [Icons] | Icons | |
| **Default** | [Icons.Default] | Default icons | ![Default icon image](https://developer.android.com/images/reference/androidx/compose/material/icons/icon-filled.svg) |
| **Filled** | [Icons.Filled] | Filled icons | ![Filled icon image](https://developer.android.com/images/reference/androidx/compose/material/icons/icon-filled.svg) |
| **Outlined** | [Icons.Outlined] | Outlined icons | ![Outlined icon image](https://developer.android.com/images/reference/androidx/compose/material/icons/icon-outlined.svg) |
| **Rounded** | [Icons.Rounded] | Rounded icons | ![Rounded icon image](https://developer.android.com/images/reference/androidx/compose/material/icons/icon-rounded.svg) |
| **Two tone** | [Icons.TwoTone] | Two tone icons | ![Two tone icon image](https://developer.android.com/images/reference/androidx/compose/material/icons/icon-two-tone.svg) |
| **Sharp** | [Icons.Sharp] | Sharp icons | ![Sharp icon image](https://developer.android.com/images/reference/androidx/compose/material/icons/icon-sharp.svg) |

**Note:** [Icons.Default] is an alias for [Icons.Filled].

### Related components

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Icon** | [androidx.compose.material.Icon] | Icon |
|  | [androidx.compose.material3.Icon] | M3 icon |
| **Icon button** | [androidx.compose.material.IconButton] | Icon button |
|  | [androidx.compose.material3.IconButton] | M3 icon button |
| **Icon toggle button** | [androidx.compose.material.IconToggleButton] | Icon toggle button |
|  | [androidx.compose.material3.IconToggleButton] | M3 icon toggle button |
