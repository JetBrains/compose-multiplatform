# 1.7.0-alpha03 (August 2024)

_Changes since 1.7.0-alpha02_

## Features
### Multiple Platforms
- [Skia is updated to m126](https://github.com/JetBrains/compose-multiplatform-core/pull/1486)
- [Commonized `org.jetbrains.compose.material3:material3-window-size-class` module](https://github.com/JetBrains/compose-multiplatform-core/pull/1466)
- [Commonized `org.jetbrains.compose.material3.adaptive:adaptive*` modules](https://github.com/JetBrains/compose-multiplatform-core/pull/1468)

### Resources
- [Added utility functions to decode `Bitmap ByteArray as ImageVector` and `XML ByteArray as ImageVector` in the common code and `SVG ByteArray as Painter` in the non-android code](https://github.com/JetBrains/compose-multiplatform/pull/5098)

## Fixes
### Desktop
- [[macOS] Fix crash when right-clicking an empty `SelectionContainer` or on the padding of a `Text` inside a `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform-core/pull/1439)
- [_(prerelease fix)_ Fix input methods position on the screen and `NullPointerException: Cannot read field`](https://github.com/JetBrains/compose-multiplatform-core/pull/1491)

### iOS
- [_(prerelease fix)_  Fix the bug where only the changed touches were sent Compose, while all tracked touches were expected](https://github.com/JetBrains/compose-multiplatform-core/pull/1477)

### Gradle Plugin
- [_(prerelease fix)_ Fix broken configuration cache due Android Studio + AGP issues. Now Android Studio previews require latest AGP versions (8.5.2, 8.6.0-rc01, 8.7.0-alpha04): https://issuetracker.google.com/issues/348208777](https://github.com/JetBrains/compose-multiplatform/pull/5118)

## Dependencies
- Gradle Plugin `org.jetbrains.compose`, version `1.7.0-alpha02`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.0-beta06)
  - [UI 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.0-beta06)
  - [Foundation 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0-beta06)
  - [Material 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0-beta06)
  - [Material3 1.3.0-beta05](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0-beta05)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0`. Based on [Jetpack Lifecycle 2.8.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha09`. Based on [Jetpack Navigation 2.8.0-beta05](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-beta05)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive-*:1.0.0-alpha01`. Based on [Jetpack Material3 Adaptive 1.0.0-beta04](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0-beta04)

To use Material3 Adaptive add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:
```Kotlin
dependencies {
  implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.0.0-alpha01")
  implementation("org.jetbrains.compose.material3.adaptive:adaptive-layout:1.0.0-alpha01")
  implementation("org.jetbrains.compose.material3.adaptive:adaptive-navigation:1.0.0-alpha01")
}
```

___

# 1.7.0-alpha02 (July 2024)

_Changes since 1.7.0-alpha01_

## Features

### Multiple Platforms
- [The `clickable` modifier now responds to NumPadEnter and Spacebar, too, in addition to Enter](https://github.com/JetBrains/compose-multiplatform-core/pull/1464)
- [`LocalLifecycleOwner` moved from Compose UI to `lifecycle-runtime-compose` so that its Compose-based helper APIs can be used outside of Compose UI](https://github.com/JetBrains/compose-multiplatform-core/pull/1449)

### iOS
- [Improvements in touches processing to detect if touches were meant to be delivered to interop views, or should be processed by Compose](https://github.com/JetBrains/compose-multiplatform-core/pull/1440)

### Desktop
- [Implemented Drag-and-Drop from AOSP: `Modifier.dragAndDropSource` and `Modifier.dragAndDropTarget`](https://github.com/JetBrains/compose-multiplatform-core/pull/1433)

### Resources
- [Now the gradle plugin generates resources map to find a resource by a string ID](https://github.com/JetBrains/compose-multiplatform/pull/5068)
- [To avoid constant reading raw font bytes on each Font usage on non-android targets, there was added the font cache. Android has own font cache inside the platform implementation](https://github.com/JetBrains/compose-multiplatform/pull/5109)

## Fixes

### Multiple Platforms
- [_(prerelease fix)_ Restore missing `SearchBar` changes from `material3` 1.3](https://github.com/JetBrains/compose-multiplatform-core/pull/1455)

### iOS
- [Interop views are now correctly clipped when their measured clipped and unclipped bounding boxes don't match](https://github.com/JetBrains/compose-multiplatform-core/pull/1430)
- [Touches inside interop views are not exclusive to them and are processed on Compose side as well.](https://github.com/JetBrains/compose-multiplatform-core/pull/1426)
- [Fix `material3.ModalBottomSheet` safe area usage](https://github.com/JetBrains/compose-multiplatform-core/pull/1438)
- [Fix hiding interop element during quick scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1425)
- [_(prerelease fix)_ Fixed floating cursor isn't working](https://github.com/JetBrains/compose-multiplatform-core/pull/1443)
- [Fixed the keyboard appearing when selecting from SelectionContainer](https://github.com/JetBrains/compose-multiplatform-core/pull/1448)
- [Fix status bar padding on iPad devices](https://github.com/JetBrains/compose-multiplatform-core/pull/1442)
- [VoiceOver doesn't allow to perform a11y actions (scrolling, activate, customActions) when disabled() semantics is present in affected element](https://github.com/JetBrains/compose-multiplatform-core/pull/1446)

### Desktop
- [Fix scrolling non-same direction nested scrolls with trackpad](https://github.com/JetBrains/compose-multiplatform-core/pull/1434)
- [Fix fling velocity for precise wheel scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1402)
- [_(prerelease fix)_ Fix remaining focus indication after a click](https://github.com/JetBrains/compose-multiplatform-core/pull/1467)

### Resources
- [_(prerelease fix)_ Fix an android app compose resources packaging broken after introduction AS previews](https://github.com/JetBrains/compose-multiplatform/pull/5090)
- [Now drawables from upper DPIs will be downscalled to the expected size. (the same behavior as on Android)](https://github.com/JetBrains/compose-multiplatform/pull/5101)

### Gradle plugin
- [_(prerelease fix)_ Fix "InvalidUserDataException: Cannot change hierarchy of dependency configuration" on Gradle sync](https://github.com/JetBrains/compose-multiplatform/pull/5076)

## Dependencies
- Gradle Plugin `org.jetbrains.compose`, version `1.7.0-alpha02`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.0-beta05](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.0-beta05)
  - [UI 1.7.0-beta05](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.0-beta05)
  - [Foundation 1.7.0-beta05](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0-beta05)
  - [Material 1.7.0-beta05](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0-beta05)
  - [Material3 1.3.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0-beta03)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0`. Based on [Jetpack Lifecycle 2.8.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha08`. Based on [Jetpack Navigation 2.8.0-beta03](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-beta03)

___

# 1.7.0-alpha01 (July 2024)

_Changes since 1.6.11_

## Highlights

- [Compose Multiplatform resources are stored in the android assets now. This fixes Android Studio Preview and cases such as a rendering resource files in WebViews or Media Players](https://github.com/JetBrains/compose-multiplatform/pull/4965)
- [Shared Element Transitions](https://developer.android.com/develop/ui/compose/animation/shared-elements)
- [Safe Args in Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety)

## Breaking changes

### Android

- [Minimal supported AGP raised to 8.1.0](https://github.com/JetBrains/compose-multiplatform/pull/4840)

## Features

### iOS

- [Initial iOS floating cursor support](https://github.com/JetBrains/compose-multiplatform-core/pull/1312)
- [Added `accessibilityEnabled: Boolean = true` argument to `UIKitView` and `UIKitViewController`](https://github.com/JetBrains/compose-multiplatform-core/pull/1350)
- [`preferredStatusBarStyle`, `preferredStatysBarAnimation` and `prefersStatusBarHidden` are added to `ComposeUIViewControllerDelegate` to allow status bar appearance modification](https://github.com/JetBrains/compose-multiplatform-core/pull/1378)

### Desktop

- [Add constructor with `RenderSettings` to `ComposePanel`. Added a class `RenderSettings` with  `val isVsyncEnabled: Boolean?`. When set to `true` gives a hint to renderer implementation of the particular `ComposePanel` to reduce the latency between the input and visual changes in exchange for possible screen tearing](https://github.com/JetBrains/compose-multiplatform-core/pull/1377)
- [Add public `moveEnabled` and `positionPercentage` setters in `SplitPaneState`](https://github.com/JetBrains/compose-multiplatform/pull/3974)

### Resources

- [Speed resources web rendering up by the reading a cached value instantly](https://github.com/JetBrains/compose-multiplatform/pull/4893)
- [If there is no resource with suitable density, use resource with the most suitable density, otherwise use default (similar to the Android logic)](https://github.com/JetBrains/compose-multiplatform/pull/4969)
- [Add a customization for resources directories. Now it is possible to use e.g downloaded resources](https://github.com/JetBrains/compose-multiplatform/pull/5016)

## Fixes

### Multiple Platforms

- [Fix "ComposePanel. Focus moves to child after focusing/unfocusing the main window"](https://github.com/JetBrains/compose-multiplatform-core/pull/1398)
- [Don't show code completion for non-existenst API in `commonMain` that fails on Android with `NoSuchMethodException`](https://github.com/JetBrains/compose-multiplatform-core/pull/1328)
- [Fix order of interop elements in some cases](https://github.com/JetBrains/compose-multiplatform-core/pull/1340)
- [Fixed `Popup` jerking during ripple effect animation](https://github.com/JetBrains/compose-multiplatform-core/pull/1385)
- [Fix applying `ShaderBrush` to part of `AnnotatedString`](https://github.com/JetBrains/compose-multiplatform-core/pull/1389)
- [Fix text `brush` animation and optimized updating some visual text properties (applying time is reduced up to 40%)](https://github.com/JetBrains/compose-multiplatform-core/pull/1395)
- [Fix initial cursor position in the empty `TextField` with explicitly set `TextAlignment`](https://github.com/JetBrains/compose-multiplatform-core/pull/1354)
- [Fix focus for editable `TextField` inside `ExposedDropdownMenuBox`](https://github.com/JetBrains/compose-multiplatform-core/pull/1423)

### iOS

- [Pressing directional keys on a physical keyboard connected to iOS device doesn't cause a crash](https://github.com/JetBrains/compose-multiplatform-core/pull/1383)
- [Dismissing popup or dialogue within a very short timespan after its creation doesn't cause a crash](https://github.com/JetBrains/compose-multiplatform-core/pull/1384)
- [Fix missing invalidations during native view resize](https://github.com/JetBrains/compose-multiplatform-core/pull/1387)
- [Fixed a memory spike when continuously resizing the `ComposeUIViewController` (such as when used in modal sheet presentation context with different detents)](https://github.com/JetBrains/compose-multiplatform-core/pull/1390)
- [visibility of selection handles in single-line textfields with LTR + RTL text in iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1331)

### Desktop

- [Fix possible `UninitializedPropertyAccessException` in `desktopTest`](https://github.com/JetBrains/compose-multiplatform-core/pull/1343)
- [Fixed `ComposePanel.requestFocus()`, making it correctly assign focus to the first focusable child](https://github.com/JetBrains/compose-multiplatform-core/pull/1352)
- [When using `ComposePanel` inside a Swing application on macOS, VoiceOver will now correctly go into the `ComposePanel` when traversing accessible elements](https://github.com/JetBrains/compose-multiplatform-core/pull/1362)
- [When using `ComposePanel` inside a Swing application on Windows with NVDA turned on, focus will now correctly go into the `ComposePanel` when traversing with (ctrl)-shift-tab](https://github.com/JetBrains/compose-multiplatform-core/pull/1363)
- [Correctly save `WindowState` with unspecified `size` instead of crashing](https://github.com/JetBrains/compose-multiplatform-core/pull/1394)
- [Fix `IndexOutOfBoundsException` crash on Windows when traversing a11y elements](https://github.com/JetBrains/compose-multiplatform-core/pull/1415)

### Web

- [Process `keydown` and `keyup` keys for identified keys from virtual keyboard](https://github.com/JetBrains/compose-multiplatform-core/pull/1380)
- [Allow preloading the fallback fonts. This enables the usage of emojis and other unicode characters without manually composing the Text with AnnotatedString](https://github.com/JetBrains/compose-multiplatform-core/pull/1400)
- [Make sure the web app distribution doesn't contain a duplicate `skiko.wasm`](https://github.com/JetBrains/compose-multiplatform/pull/4958)

### Resources

- [Delete `contextClassLoader` usage on JVM targets](https://github.com/JetBrains/compose-multiplatform/pull/4895)
- [Create an empty resource dir with "podspec" task instead "podInstall"](https://github.com/JetBrains/compose-multiplatform/pull/4900)
- [Fix resource accessors escaping. Now it is possible to use resources with names: "package", "is", "item_$xxx" etc](https://github.com/JetBrains/compose-multiplatform/pull/4901)
- [Read exactly requested count of bytes from InputStream on jvm platforms](https://github.com/JetBrains/compose-multiplatform/pull/4943)

### Gradle Plugin

- [Make sure tryGetSkikoRuntimeIfNeeded is executed only during the task execution](https://github.com/JetBrains/compose-multiplatform/pull/4918)
- [Delete outdated build services](https://github.com/JetBrains/compose-multiplatform/pull/4959)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.7.0-alpha01`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.14](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.14)
  - [Runtime 1.7.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.0-beta03)
  - [UI 1.7.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.0-beta03)
  - [Foundation 1.7.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0-beta03)
  - [Material 1.7.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0-beta03)
  - [Material3 1.3.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0-beta03)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0`. Based on [Jetpack Lifecycle 2.8.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha08`. Based on [Jetpack Navigation 2.8.0-beta03](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-beta03)

___

# 1.6.11 (June 2024)

_Changes since 1.6.10_

## Fixes

### Multiple Platforms

- [Fix endless re-layout when layout is invalidated by measure, which includes measuring `TextField(singleLine=true)` with `IntrinsicSize`](https://github.com/JetBrains/compose-multiplatform-core/pull/1355)
- [Fix container size for `Dialog` centering inside `ImageComposeScene`](https://github.com/JetBrains/compose-multiplatform-core/pull/1375)

### iOS

- [Fix crash on iOS 12 caused by usage unavailable `UIMenuController` API](https://github.com/JetBrains/compose-multiplatform-core/pull/1361)

### Desktop

- [Fix `DropdownMenu`/`Popup` positioning when a window is moved to a screen with a different density](https://github.com/JetBrains/compose-multiplatform-core/pull/1333)
- [Fix possible scrolling without animation on some mouse models](https://github.com/JetBrains/compose-multiplatform-core/pull/1326)

### Web

- [Fixed crash when `DatePicker` text field receives illegal input](https://github.com/JetBrains/compose-multiplatform-core/pull/1368)

### Resources

- [Fix a cached font if the resource acessor was changed](https://github.com/JetBrains/compose-multiplatform/pull/4864)

### Gradle Plugin

- [Fix Compose Compiler configuration for Kotlin < 2.0 when kotlin-android or kotlin-js gradle plugins are applied](https://github.com/JetBrains/compose-multiplatform/pull/4879)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.11`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.14](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.14)
  - [Runtime 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.7)
  - [UI 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.7)
  - [Foundation 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.7)
  - [Material 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.7)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0`. Based on [Jetpack Lifecycle 2.8.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.7.0-alpha07`. Based on [Jetpack Navigation 2.7.7](https://developer.android.com/jetpack/androidx/releases/navigation#2.7.7)

___

# 1.6.10 (May 2024)

_Changes since 1.6.2_

## Highlights

- Experimental multiplatform support of `Lifecycle` and `ViewModel`. See [documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-lifecycle.html) for details
- Experimental multiplatform support of Jetpack Navigation. See [documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html) for details or check out the [nav_cupcake project](https://github.com/JetBrains/compose-multiplatform/tree/master/examples/nav_cupcake/) which was converted from the [Navigate between screens with Compose](https://developer.android.com/codelabs/basic-android-kotlin-compose-navigation) Android codelab
- [Support multi-module projects and libraries publication with Compose resources](https://github.com/JetBrains/compose-multiplatform/pull/4454) <sub>_(Kotlin `2.0.0-Beta05` or higher is required)_</sub>

## Breaking changes

- Since Kotlin `2.0-RC2`, the additional `org.jetbrains.kotlin.plugin.compose` Gradle plugin is required. See [the migration guide](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compiler.html#migrating-a-compose-multiplatform-project)

## Features

### Multiple Platforms

- [Add font rasterization settings in paragraph style](https://github.com/JetBrains/compose-multiplatform-core/pull/1102)
- [Localize internal strings in `ui`, `material` and `material3`](https://github.com/JetBrains/compose-multiplatform-core/pull/1158)

### iOS

- [Magnifier for iOS 17+](https://github.com/JetBrains/compose-multiplatform-core/pull/1000)
- [Support software keyboard inset in `Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/1067)
- [iOS a11y dialogues and popups integration](https://github.com/JetBrains/compose-multiplatform-core/pull/1091)
- [OS logging integrated with `trace`](https://github.com/JetBrains/compose-multiplatform-core/pull/1140)
- [Support accessibility scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1169)
- [Improve iOS a11y sync behavior](https://github.com/JetBrains/compose-multiplatform-core/pull/1170)
- [Add throttle for text context menu updates](https://github.com/JetBrains/compose-multiplatform-core/pull/1182)
- [Support a11y for interop views](https://github.com/JetBrains/compose-multiplatform-core/pull/1241)
- [Support `HapticFeedback` on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1255)
- [iOS support `LiveRegion` semantics in a11y](https://github.com/JetBrains/compose-multiplatform-core/pull/1258)

### Desktop

- [Add an `alwaysOnTop` flag to `DialogWindow`](https://github.com/JetBrains/compose-multiplatform-core/pull/1120)
- Basic support of `BasicTextField2`: [#1227](https://github.com/JetBrains/compose-multiplatform-core/pull/1227), [#1264](https://github.com/JetBrains/compose-multiplatform-core/pull/1264) <sub>_full support and support for other platforms is planned for 1.7.0_</sub>

### Web

- [Listen to browser clipboard events and bind them with Compose TextFieldSelectionManager and SelectionManager](https://github.com/JetBrains/compose-multiplatform-core/pull/1206)
- [Introduce ComposeViewport function that renders content in parent container](https://github.com/JetBrains/compose-multiplatform-core/pull/1211)
- [Introduce minimal virtual keyboard support](https://github.com/JetBrains/compose-multiplatform-core/pull/1259)
- [Basic IME keyboard support](https://github.com/JetBrains/compose-multiplatform-core/pull/1297)
- [Some experimental Compose Multiplatform Gradle plugin APIs for web app configuration were deprecated. Their usage is not required anymore.](https://github.com/JetBrains/compose-multiplatform/pull/4796)

### Resources

- [Support three letters locales](https://github.com/JetBrains/compose-multiplatform/pull/4394)
- [Add DSL to configure compose resources](https://github.com/JetBrains/compose-multiplatform/pull/4482)
- [Support plural string resource](https://github.com/JetBrains/compose-multiplatform/pull/4519)
- [Add option to disable compose resources generation](https://github.com/JetBrains/compose-multiplatform/pull/4526)
- [XML resource optimizations](https://github.com/JetBrains/compose-multiplatform/pull/4559)
- [Get resource files as URI](https://github.com/JetBrains/compose-multiplatform/pull/4576)
- [Support source set's hierarchy for compose resources](https://github.com/JetBrains/compose-multiplatform/pull/4589)
- [Support SVG drawables for non android platforms](https://github.com/JetBrains/compose-multiplatform/pull/4605)
- [Delete the experimental mark from the stable resources library API](https://github.com/JetBrains/compose-multiplatform/pull/4623)
- [Add functions to retrieve bytes from drawable or font resources](https://github.com/JetBrains/compose-multiplatform/pull/4651)
- [To disable the Compose Resources publication and the multimodule support in cases of problems add `org.jetbrains.compose.resources.multimodule.disable=true` to the root `gradle.properties`](https://github.com/JetBrains/compose-multiplatform/pull/4771)

### Gradle Plugin

- [Add possibility to modify the macOS minimum version](https://github.com/JetBrains/compose-multiplatform/pull/4271)
- [Make desktop preview task fully configuration cache compliant](https://github.com/JetBrains/compose-multiplatform/pull/4410)
- [Option to pack jars as uber JAR, support Proguard for uber JAR](https://github.com/JetBrains/compose-multiplatform/pull/4136)

## Fixes

### Multiple Platforms

- [Fix `NaN` font metrics by initializing typeface for `TextStyle`](https://github.com/JetBrains/compose-multiplatform-core/pull/1087)
- [Fix render order of interop views](https://github.com/JetBrains/compose-multiplatform-core/pull/1145)
- [Reimplement SortedSet for JS/Native to improve performance](https://github.com/JetBrains/compose-multiplatform-core/pull/1167)
- [Allow drawing outside of platform layers](https://github.com/JetBrains/compose-multiplatform-core/pull/1190)
- [Prevent a few unnecessary re-compositions in `Popup` and `DesktopMenu`](https://github.com/JetBrains/compose-multiplatform-core/pull/1225)
- [Propagate composition locals to layers in the (re)composition phase](https://github.com/JetBrains/compose-multiplatform-core/pull/1233)
- [Move the effects and synthetic events dispatching to after the draw phase in the render loop](https://github.com/JetBrains/compose-multiplatform-core/pull/1260)
- [Fix Kotlin/Native can't use `T::class` in inline function of `@Composable`](https://github.com/JetBrains/compose-multiplatform/issues/3147)
- [Fix missing recomposition after showing `Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/1272)

### iOS

- [Fix keyboard disappear on IME action](https://github.com/JetBrains/compose-multiplatform-core/pull/1118)
- [Fix nested scroll when `Pager` involved in scrolling process](https://github.com/JetBrains/compose-multiplatform-core/pull/1154)
- [Fix a11y wrong bounds calculation](https://github.com/JetBrains/compose-multiplatform-core/pull/1165)
- [Delay tap indication inside scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1173)
- [Fix keyboard opening when scrolling begins within a `TextField`](https://github.com/JetBrains/compose-multiplatform-core/pull/1176)
- [Fix IME window insets and view offset when keyboard appears](https://github.com/JetBrains/compose-multiplatform-core/pull/1199)
- [Fix animation frozen after app went background](https://github.com/JetBrains/compose-multiplatform-core/pull/1263)
- Fixed appearing of text editing menu ([#1269](https://github.com/JetBrains/compose-multiplatform-core/pull/1269), [#1293](https://github.com/JetBrains/compose-multiplatform-core/pull/1293))
- [Fix content rect calculation in `SelectionManager`](https://github.com/JetBrains/compose-multiplatform-core/pull/1270)

### Desktop

- [Fix nested scrolling on mouse wheel](https://github.com/JetBrains/compose-multiplatform-core/pull/1055)
- [Trigger fling callbacks on mouse wheel scroll (fixes `Pager` and lazy column/row spanning)](https://github.com/JetBrains/compose-multiplatform-core/pull/1100)
- [Fix `Pager` direction detection for mouse wheel](https://github.com/JetBrains/compose-multiplatform-core/pull/1103)
- [Fix missing clicks inside `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform-core/pull/1106)
- [Fix clipping bounds of `SwingPanel`](https://github.com/JetBrains/compose-multiplatform-core/pull/1147)
- [Locale-aware date formatting for desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/1159)
- [Pass the id of the node whose layout changed to accessibility controllers](https://github.com/JetBrains/compose-multiplatform-core/pull/1162)
- `WINDOW` layer fixes: [#1181](https://github.com/JetBrains/compose-multiplatform-core/pull/1181), [#1185](https://github.com/JetBrains/compose-multiplatform-core/pull/1185), [#1187](https://github.com/JetBrains/compose-multiplatform-core/pull/1187), [#1189](https://github.com/JetBrains/compose-multiplatform-core/pull/1189)
- [Fix crash when we resize `ComposePanel` after re-adding it to the hierarchy](https://github.com/JetBrains/compose-multiplatform-core/pull/1195)
- [Fix propagation of `LocalLocalization`](https://github.com/JetBrains/compose-multiplatform-core/pull/1202)
- [Re-show the tooltip on mouse-move following a click in TooltipArea](https://github.com/JetBrains/compose-multiplatform-core/pull/1209)
- [Fix the direction of scrolling when pressing on the scrollbar track with `reverseLayout=true`](https://github.com/JetBrains/compose-multiplatform-core/pull/1221)
- [Fix crash and allow selection in `SelectionContainer` to start when drag starts below the bounds of the visible text](https://github.com/JetBrains/compose-multiplatform-core/pull/1230)
- [Fix a crash on Windows without `dcomp.dll`](https://github.com/JetBrains/skiko/pull/909)
- [Fix crash when creating SwingRedrawer on DirectX](https://github.com/JetBrains/skiko/pull/917)
- [Option to not throw `RenderException` when use OpenGL on macOS](https://github.com/JetBrains/skiko/pull/915)
- [Sync all AccessibilityControllers when an a11y query is received](https://github.com/JetBrains/compose-multiplatform-core/pull/1283)
- [Fix crash when modifying Compose state from a non-UI thread](https://github.com/JetBrains/compose-multiplatform-core/pull/1288)
- [Close `Popup`/`Dialog` by clicking any mouse button outside](https://github.com/JetBrains/compose-multiplatform-core/pull/1280)

### Web

- [Implement actual `fun isCopyKeyEvent`](https://github.com/JetBrains/compose-multiplatform-core/pull/1101)
- [Fix metadata compilation](https://github.com/JetBrains/compose-multiplatform-core/pull/1123)
- [Support `sendKey` test utility function on wasm target](https://github.com/JetBrains/compose-multiplatform-core/pull/1150)
- [Send correct window sized on resize](https://github.com/JetBrains/compose-multiplatform-core/pull/1166)
- [Dispose all listened events alongside with the application being disposed](https://github.com/JetBrains/compose-multiplatform-core/pull/1239)
- [Modify `buttonFlags` only on `Press` and `Release`](https://github.com/JetBrains/compose-multiplatform-core/pull/1243)
- [Fix keyboard mappings](https://github.com/JetBrains/compose-multiplatform-core/pull/1249)
- [Correct density rounding in `ComposeWindow::resize`](https://github.com/JetBrains/compose-multiplatform-core/pull/1268)
- [Fix `MouseEvent` to `PointerButton` mapping](https://github.com/JetBrains/compose-multiplatform-core/pull/1274)
- [Correct virtual keyboard mode resolution](https://github.com/JetBrains/compose-multiplatform-core/pull/1295)
- [Fix browser clipboard events handling on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/1329)
- [Rename `viewportContainer` to `viewportContainerId` parameter in `ComposeViewport`](https://github.com/JetBrains/compose-multiplatform-core/pull/1334)

### Resources

- [Fix resource accessors compilation when there are huge number of resource files](https://github.com/JetBrains/compose-multiplatform/pull/4294)
- [Init resource accessors lazily and in external function to avoid `MethodTooLargeException`](https://github.com/JetBrains/compose-multiplatform/pull/4404)
- [Fix `Res` class generation if the library is declared as 'api'](https://github.com/JetBrains/compose-multiplatform/pull/4406)
- [Fix resource packaging in APK when 'build' task is used with AGP 8.2+](https://github.com/JetBrains/compose-multiplatform/pull/4408)
- [Use first of preferred locales instead of a current on iOS](https://github.com/JetBrains/compose-multiplatform/pull/4507)
- [Fix `readResourceBytes` function on Android if font is located under qualified directory](https://github.com/JetBrains/compose-multiplatform/pull/4512)
- [Fix android fonts in APKs](https://github.com/JetBrains/compose-multiplatform/pull/4509)
- [Handle special characters for quantity strings](https://github.com/JetBrains/compose-multiplatform/pull/4543)
- [Select default resource if there are no exact language+region or default language match](https://github.com/JetBrains/compose-multiplatform/pull/4577)
- [Add validation checks on invalid XML or item type](https://github.com/JetBrains/compose-multiplatform/pull/4680)

### Gradle plugin

- [Fix `Task with name 'podBuildGoogleMapsIphonesimulator' not found` in a project with Cococapods"](https://github.com/JetBrains/compose-multiplatform/pull/4707)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.10`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.14](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.14)
  - [Runtime 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.7)
  - [UI 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.7)
  - [Foundation 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.7)
  - [Material 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.7)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0`. Based on [Jetpack Lifecycle 2.8.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.7.0-alpha07`. Based on [Jetpack Navigation 2.7.7](https://developer.android.com/jetpack/androidx/releases/navigation#2.7.7)

___

# 1.6.10-rc03 (May 2024)

_Changes since 1.6.10-rc02_

## Fixes

### iOS

- _(prerelease fix)_ [Fix `org.jetbrains.kotlin.backend.konan.llvm.NativeCodeGeneratorException: Exception during generating code for following declaration: private fun $init_global()`](https://github.com/JetBrains/compose-multiplatform/issues/4809)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.10-rc03`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.14](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.14)
  - [Runtime 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.7)
  - [UI 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.7)
  - [Foundation 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.7)
  - [Material 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.7)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0-rc03`. Based on [Jetpack Lifecycle 2.8.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.7.0-alpha06`. Based on [Jetpack Navigation 2.7.7](https://developer.android.com/jetpack/androidx/releases/navigation#2.7.7)

___

# 1.6.10-rc02 (May 2024)

_Changes since 1.6.10-rc01_

## Known issues

- iOS compilation might fail on some projects with [`org.jetbrains.kotlin.backend.konan.llvm.NativeCodeGeneratorException: Exception during generating code for following declaration: private fun $init_global()`](https://github.com/JetBrains/compose-multiplatform/issues/4809)

## Features

### Web

- [Some experimental Compose Multiplatform Gradle plugin APIs for web app configuration were deprecated. Their usage is not required anymore.](https://github.com/JetBrains/compose-multiplatform/pull/4796)

## Fixes

### iOS

- _(prerelease fix)_ [Fixes a crash when `UIKitView` or `UIKitViewController` is inside a tree with `SemanticsModifierNode` having `shouldMergeDescendantSemantics = true` and Accessibility sync is on](https://github.com/JetBrains/compose-multiplatform-core/pull/1347)

### Resources

- [To disable the Compose Resources publication and the multimodule support in cases of problems add `org.jetbrains.compose.resources.multimodule.disable=true` to the root `gradle.properties`](https://github.com/JetBrains/compose-multiplatform/pull/4771)
- _(prerelease fix)_ [Fix resources with cocoapods integration](https://github.com/JetBrains/compose-multiplatform/pull/4783)
- _(prerelease fix)_ [Fix AGP lint tasks dependency issues](https://github.com/JetBrains/compose-multiplatform/pull/4784)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.10-rc02`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.13](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.13)
  - [Runtime 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.7)
  - [UI 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.7)
  - [Foundation 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.7)
  - [Material 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.7)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0-rc02`. Based on [Jetpack Lifecycle 2.8.0-rc01](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0-rc01)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.7.0-alpha05`. Based on [Jetpack Navigation 2.7.7](https://developer.android.com/jetpack/androidx/releases/navigation#2.7.7)

___

# 1.6.10-rc01 (May 2024)

_Changes since 1.6.10-beta03_

## Known issues

- [A project with Android flavors cannot be imported into any IDE if it has Compose Multiplatform resources 1.6.10 + Kotlin 2.0.0](https://youtrack.jetbrains.com/issue/KT-67806)
- If you experience the `org.jetbrains.compose.resources.MissingResourceException: Missing resource with path: ...` error after switching your Kotlin version from 1.9 to 2.0 (or the other way around), this might be resolved by manually cleaning the `build` directories in your project - this includes the `build` folders located in the root and module folders of your project.

## Fixes

### Desktop

- _(prerelease fix)_ [Fix inconsistency in closing `Dialog` by mouse clicking on scrim that was introduced by `1.6.10-beta02`](https://github.com/JetBrains/compose-multiplatform-core/pull/1336)

### Web

- [Fix browser clipboard events handling on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/1329)
- [Rename `viewportContainer` to `viewportContainerId` parameter in `ComposeViewport`](https://github.com/JetBrains/compose-multiplatform-core/pull/1334)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.10-rc01`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.11](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.11)
  - [Runtime 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.7)
  - [UI 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.7)
  - [Foundation 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.7)
  - [Material 1.6.7](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.7)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0-rc01`. Based on [Jetpack Lifecycle 2.8.0-rc01](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0-rc01)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.7.0-alpha04`. Based on [Jetpack Navigation 2.7.7](https://developer.android.com/jetpack/androidx/releases/navigation#2.7.7)

___

# 1.6.10-beta03 (April 2024)

_Changes since 1.6.10-beta02_

## Highlights

- [Support Kotlin `2.0-RC2`](https://github.com/JetBrains/compose-multiplatform/pull/4604)

## Breaking changes

- Since Kotlin `2.0-RC2`, the additional `org.jetbrains.kotlin.plugin.compose` Gradle plugin is required. See [the migration guide](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compiler.html#migrating-a-compose-multiplatform-project)
- `org.jetbrains.androidx.navigation` is downgraded to `2.7` from `2.8`. Some API can no longer be available

## Known issues

- [`lifecycle-runtime` breaks Compose UI compatibility with Java 11 on desktop, it requires Java 17 or above now. Expected to be fixed in `1.6.10-rc01`](http://r.android.com/3047339)
- [A project with Android flavors cannot be imported into any IDE if it has Compose Multiplatform resources 1.6.10 + Kotlin 2.0.0](https://youtrack.jetbrains.com/issue/KT-67806)

## Features

### Multiple Platforms

- _(prerelease fix)_ [Publish additional targets for `lifecycle-runtime-compose`](https://github.com/JetBrains/compose-multiplatform-core/pull/1322)

### Lifecycle

- _(prerelease fix)_ [Update Lifecycle handling on iOS based on feedback](https://github.com/JetBrains/compose-multiplatform-core/pull/1319)

### Web

- [Basic IME keyboard support](https://github.com/JetBrains/compose-multiplatform-core/pull/1297)

## Fixes

### Multiple Platforms

- _(prerelease fix)_ [Fix frozen composition with pager and text field](https://github.com/JetBrains/compose-multiplatform-core/pull/1316)

### Desktop

- [Fix crash when creating SwingRedrawer on DirectX](https://github.com/JetBrains/skiko/pull/917)
- [Option to not throw `RenderException` when use OpenGL on macOS](https://github.com/JetBrains/skiko/pull/915)

### Resources

- [Add validation checks on invalid XML or item type](https://github.com/JetBrains/compose-multiplatform/pull/4680)

### Gradle plugin

- [Fix `Task with name 'podBuildGoogleMapsIphonesimulator' not found` in a project with Cococapods"](https://github.com/JetBrains/compose-multiplatform/pull/4707)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.10-beta03`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.11](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.11)
  - [Runtime 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.6)
  - [UI 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.6)
  - [Foundation 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.6)
  - [Material 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.6)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0-beta02`. Based on [Jetpack Lifecycle 2.8.0-beta01](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0-beta01)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.7.0-alpha03`. Based on [Jetpack Navigation 2.7.7](https://developer.android.com/jetpack/androidx/releases/navigation#2.7.7)

___

# 1.6.10-beta02 (April 2024)

_Changes since 1.6.10-beta01_

## Known issues

- [`lifecycle-runtime` breaks Compose UI compatibility with Java 11 on desktop, it requires Java 17 or above now. Expected to be fixed in `1.6.10-rc01`](http://r.android.com/3047339)
- [`navigation-compose:2.8.0-*` on Android brings Compose `1.7.*` dependency and might cause compatibility issues with `1.6.*`](https://github.com/JetBrains/compose-multiplatform/issues/4677)

## Features

### Lifecycle

- [Publish additional targets for non-compose modules](https://github.com/JetBrains/compose-multiplatform-core/pull/1282)
- [Commonize `inline fun <reified VM> viewModel(..)`](https://github.com/JetBrains/compose-multiplatform-core/pull/1290)
- [Provide `ViewModelStoreOwner` by Compose view](https://github.com/JetBrains/compose-multiplatform-core/pull/1291)

### Resources

- [Delete the experimental mark from the stable resources library API](https://github.com/JetBrains/compose-multiplatform/pull/4623)
- [Add functions to retrieve bytes from drawable or font resources](https://github.com/JetBrains/compose-multiplatform/pull/4651)

### Navigation

- Support parsing navigation arguments ([#1277](https://github.com/JetBrains/compose-multiplatform-core/pull/1277), [#1289](https://github.com/JetBrains/compose-multiplatform-core/pull/1289))

## Fixes

### Multiple Platforms

- [Fix Kotlin/Native can't use `T::class` in inline function of `@Composable`](https://github.com/JetBrains/compose-multiplatform/issues/3147)
- [Fix missing recomposition after showing `Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/1272)
- _(prerelease fix)_ [Fix shadow behind dialogs in platform-layer mode](https://github.com/JetBrains/compose-multiplatform-core/pull/1285)

### iOS

- Fixed appearing of text editing menu ([#1269](https://github.com/JetBrains/compose-multiplatform-core/pull/1269), [#1293](https://github.com/JetBrains/compose-multiplatform-core/pull/1293))
- [Fix content rect calculation in `SelectionManager`](https://github.com/JetBrains/compose-multiplatform-core/pull/1270)
- _(prerelease fix)_ [Fix crash on loading `UITextLoupeSession` on old iOS versions](https://github.com/JetBrains/compose-multiplatform-core/pull/1278)
- _(prerelease fix)_ Fix memory leaks ([#1292](https://github.com/JetBrains/compose-multiplatform-core/pull/1292), [#1302](https://github.com/JetBrains/compose-multiplatform-core/pull/1302))

### Desktop

- [Sync all AccessibilityControllers when an a11y query is received](https://github.com/JetBrains/compose-multiplatform-core/pull/1283)
- [Fix crash when modifying Compose state from a non-UI thread](https://github.com/JetBrains/compose-multiplatform-core/pull/1288)
- [Close `Popup`/`Dialog` by clicking any mouse button outside](https://github.com/JetBrains/compose-multiplatform-core/pull/1280)
- _(prerelease fix)_ [Fix `KeyEvent` binary compatibility](https://github.com/JetBrains/compose-multiplatform-core/pull/1310)

### Web

- [Fix `MouseEvent` to `PointerButton` mapping](https://github.com/JetBrains/compose-multiplatform-core/pull/1274)
- [Correct virtual keyboard mode resolution](https://github.com/JetBrains/compose-multiplatform-core/pull/1295)
- _(prerelease fix)_ [Correct `Key.isTypedEvent` behavior](https://github.com/JetBrains/compose-multiplatform-core/pull/1281)

### Navigation

- _(prerelease fix)_ [Fix overriding dependency visibility](https://github.com/JetBrains/compose-multiplatform-core/pull/1275)
- _(prerelease fix)_ [Fix finding graph without route](https://github.com/JetBrains/compose-multiplatform-core/pull/1311)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.10-beta02`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.11](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.11)
  - [Runtime 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.6)
  - [UI 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.6)
  - [Foundation 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.6)
  - [Material 1.6.6](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.6)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0-beta01`. Based on [Jetpack Lifecycle 2.8.0-beta01](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0-beta01)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha02`. Based on [Jetpack Navigation 2.8.0-alpha05](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-alpha05)

___

# 1.6.10-beta01 (April 2024)

_Changes since 1.6.2_

## Highlights

- Experimental multiplatform support of `Lifecycle` and `ViewModel`. See [documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-lifecycle.html) for details
- Experimental multiplatform support of Jetpack Navigation. See [documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html) for details or check out the [nav_cupcake project](https://github.com/JetBrains/compose-multiplatform/tree/master/examples/nav_cupcake) which was converted from the [Navigate between screens with Compose](https://developer.android.com/codelabs/basic-android-kotlin-compose-navigation#2) Android codelab
- [Support multi-module projects and libraries publication with Compose resources](https://github.com/JetBrains/compose-multiplatform/pull/4454) <sub>_(Kotlin `2.0.0-Beta05` or higher is required)_</sub>

## Known issues

- [**⚠️ Crash at startup on pre-iOS 17 devices due to loading `UITextLoupeSession`**](https://github.com/JetBrains/compose-multiplatform/issues/4644)
- `lifecycle-runtime` breaks Compose UI compatibility with Java 11 on desktop, it requires Java 17 or above now.
- `inline fun <reified VM> viewModel(...)` is not available from common due to [compiler bug](https://github.com/JetBrains/compose-multiplatform/issues/3147). Please use `fun <VM> viewModel(KClass, ...)` overload instead
- Compose Multiplatform doesn't provide default `ViewModelStoreOwner` yet. For using `ViewModel`s outside of `NavHost` you need to provide custom store owner via `LocalViewModelStoreOwner`

## Features

### Multiple Platforms

- [Add font rasterization settings in paragraph style](https://github.com/JetBrains/compose-multiplatform-core/pull/1102)
- [Localize internal strings in `ui`, `material` and `material3`](https://github.com/JetBrains/compose-multiplatform-core/pull/1158)

### iOS

- [Magnifier for iOS 17+](https://github.com/JetBrains/compose-multiplatform-core/pull/1000)
- [Support software keyboard inset in `Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/1067)
- [iOS a11y dialogues and popups integration](https://github.com/JetBrains/compose-multiplatform-core/pull/1091)
- [OS logging integrated with `trace`](https://github.com/JetBrains/compose-multiplatform-core/pull/1140)
- [Support accessibility scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1169)
- [Improve iOS a11y sync behavior](https://github.com/JetBrains/compose-multiplatform-core/pull/1170)
- [Add throttle for text context menu updates](https://github.com/JetBrains/compose-multiplatform-core/pull/1182)
- [Support a11y for interop views](https://github.com/JetBrains/compose-multiplatform-core/pull/1241)
- [Support `HapticFeedback` on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1255)
- [iOS support `LiveRegion` semantics in a11y](https://github.com/JetBrains/compose-multiplatform-core/pull/1258)

### Desktop

- [Add an `alwaysOnTop` flag to `DialogWindow`](https://github.com/JetBrains/compose-multiplatform-core/pull/1120)
- Basic support of `BasicTextField2`: [#1227](https://github.com/JetBrains/compose-multiplatform-core/pull/1227), [#1264](https://github.com/JetBrains/compose-multiplatform-core/pull/1264) <sub>_full support and support for other platforms is planned for 1.7.0_</sub>

### Web

- [Listen to browser clipboard events and bind them with Compose TextFieldSelectionManager and SelectionManager](https://github.com/JetBrains/compose-multiplatform-core/pull/1206)
- [Introduce ComposeViewport function that renders content in parent container](https://github.com/JetBrains/compose-multiplatform-core/pull/1211)
- [Introduce minimal virtual keyboard support](https://github.com/JetBrains/compose-multiplatform-core/pull/1259)

### Resources

- [Support three letters locales](https://github.com/JetBrains/compose-multiplatform/pull/4394)
- [Add DSL to configure compose resources](https://github.com/JetBrains/compose-multiplatform/pull/4482)
- [Support plural string resource](https://github.com/JetBrains/compose-multiplatform/pull/4519)
- [Add option to disable compose resources generation](https://github.com/JetBrains/compose-multiplatform/pull/4526)
- [XML resource optimizations](https://github.com/JetBrains/compose-multiplatform/pull/4559)
- [Get resource files as URI](https://github.com/JetBrains/compose-multiplatform/pull/4576)
- [Support source set's hierarchy for compose resources](https://github.com/JetBrains/compose-multiplatform/pull/4589)
- [Support SVG drawables for non android platforms](https://github.com/JetBrains/compose-multiplatform/pull/4605)

### Gradle Plugin

- [Add possibility to modify the macOS minimum version](https://github.com/JetBrains/compose-multiplatform/pull/4271)
- [Make desktop preview task fully configuration cache compliant](https://github.com/JetBrains/compose-multiplatform/pull/4410)
- [Option to pack jars as uber JAR, support Proguard for uber JAR](https://github.com/JetBrains/compose-multiplatform/pull/4136)

## Fixes

### Multiple Platforms

- [Fix `NaN` font metrics by initializing typeface for `TextStyle`](https://github.com/JetBrains/compose-multiplatform-core/pull/1087)
- [Fix render order of interop views](https://github.com/JetBrains/compose-multiplatform-core/pull/1145)
- [Reimplement SortedSet for JS/Native to improve performance](https://github.com/JetBrains/compose-multiplatform-core/pull/1167)
- [Allow drawing outside of platform layers](https://github.com/JetBrains/compose-multiplatform-core/pull/1190)
- [Prevent a few unnecessary re-compositions in `Popup` and `DesktopMenu`](https://github.com/JetBrains/compose-multiplatform-core/pull/1225)
- [Propagate composition locals to layers in the (re)composition phase](https://github.com/JetBrains/compose-multiplatform-core/pull/1233)
- [Move the effects and synthetic events dispatching to after the draw phase in the render loop](https://github.com/JetBrains/compose-multiplatform-core/pull/1260)

### iOS

- [Fix keyboard disappear on IME action](https://github.com/JetBrains/compose-multiplatform-core/pull/1118)
- [Fix nested scroll when `Pager` involved in scrolling process](https://github.com/JetBrains/compose-multiplatform-core/pull/1154)
- [Fix a11y wrong bounds calculation](https://github.com/JetBrains/compose-multiplatform-core/pull/1165)
- [Delay tap indication inside scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1173)
- [Fix keyboard opening when scrolling begins within a `TextField`](https://github.com/JetBrains/compose-multiplatform-core/pull/1176)
- [Fix IME window insets and view offset when keyboard appears](https://github.com/JetBrains/compose-multiplatform-core/pull/1199)
- [Fix animation frozen after app went background](https://github.com/JetBrains/compose-multiplatform-core/pull/1263)

### Desktop

- [Fix nested scrolling on mouse wheel](https://github.com/JetBrains/compose-multiplatform-core/pull/1055)
- [Trigger fling callbacks on mouse wheel scroll (fixes `Pager` and lazy column/row spanning)](https://github.com/JetBrains/compose-multiplatform-core/pull/1100)
- [Fix `Pager` direction detection for mouse wheel](https://github.com/JetBrains/compose-multiplatform-core/pull/1103)
- [Fix missing clicks inside `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform-core/pull/1106)
- [Fix clipping bounds of `SwingPanel`](https://github.com/JetBrains/compose-multiplatform-core/pull/1147)
- [Locale-aware date formatting for desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/1159)
- [Pass the id of the node whose layout changed to accessibility controllers](https://github.com/JetBrains/compose-multiplatform-core/pull/1162)
- `WINDOW` layer fixes: [#1181](https://github.com/JetBrains/compose-multiplatform-core/pull/1181), [#1185](https://github.com/JetBrains/compose-multiplatform-core/pull/1185), [#1187](https://github.com/JetBrains/compose-multiplatform-core/pull/1187), [#1189](https://github.com/JetBrains/compose-multiplatform-core/pull/1189)
- [Fix crash when we resize `ComposePanel` after re-adding it to the hierarchy](https://github.com/JetBrains/compose-multiplatform-core/pull/1195)
- [Fix propagation of `LocalLocalization`](https://github.com/JetBrains/compose-multiplatform-core/pull/1202)
- [Re-show the tooltip on mouse-move following a click in TooltipArea](https://github.com/JetBrains/compose-multiplatform-core/pull/1209)
- [Fix the direction of scrolling when pressing on the scrollbar track with `reverseLayout=true`](https://github.com/JetBrains/compose-multiplatform-core/pull/1221)
- [Fix crash and allow selection in `SelectionContainer` to start when drag starts below the bounds of the visible text](https://github.com/JetBrains/compose-multiplatform-core/pull/1230)
- [Fix a crash on Windows without `dcomp.dll`](https://github.com/JetBrains/skiko/pull/909)

### Web

- [Implement actual `fun isCopyKeyEvent`](https://github.com/JetBrains/compose-multiplatform-core/pull/1101)
- [Fix metadata compilation](https://github.com/JetBrains/compose-multiplatform-core/pull/1123)
- [Support `sendKey` test utility function on wasm target](https://github.com/JetBrains/compose-multiplatform-core/pull/1150)
- [Send correct window sized on resize](https://github.com/JetBrains/compose-multiplatform-core/pull/1166)
- [Dispose all listened events alongside with the application being disposed](https://github.com/JetBrains/compose-multiplatform-core/pull/1239)
- [Modify `buttonFlags` only on `Press` and `Release`](https://github.com/JetBrains/compose-multiplatform-core/pull/1243)
- [Fix keyboard mappings](https://github.com/JetBrains/compose-multiplatform-core/pull/1249)
- [Correct density rounding in `ComposeWindow::resize`](https://github.com/JetBrains/compose-multiplatform-core/pull/1268)

### Resources

- [Fix resource accessors compilation when there are huge number of resource files](https://github.com/JetBrains/compose-multiplatform/pull/4294)
- [Init resource accessors lazily and in external function to avoid `MethodTooLargeException`](https://github.com/JetBrains/compose-multiplatform/pull/4404)
- [Fix `Res` class generation if the library is declared as 'api'](https://github.com/JetBrains/compose-multiplatform/pull/4406)
- [Fix resource packaging in APK when 'build' task is used with AGP 8.2+](https://github.com/JetBrains/compose-multiplatform/pull/4408)
- [Use first of preferred locales instead of a current on iOS](https://github.com/JetBrains/compose-multiplatform/pull/4507)
- [Fix `readResourceBytes` function on Android if font is located under qualified directory](https://github.com/JetBrains/compose-multiplatform/pull/4512)
- [Fix android fonts in APKs](https://github.com/JetBrains/compose-multiplatform/pull/4509)
- [Handle special characters for quantity strings](https://github.com/JetBrains/compose-multiplatform/pull/4543)
- [Select default resource if there are no exact language+region or default language match](https://github.com/JetBrains/compose-multiplatform/pull/4577)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.6.10-beta01`. Based on Jetpack Compose libraries:
  - [Compiler 1.5.11](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.11)
  - [Runtime 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.4)
  - [UI 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.4)
  - [Foundation 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.4)
  - [Material 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.4)
  - [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0-alpha01`. Based on:
  - [Lifecycle 2.8.0-alpha04](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0-alpha04)
  - [Lifecycle Runtime Compose 2.8.0-alpha02](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0-alpha02) <sub>_(due to Compose `1.6.*` compatibility)_</sub>

- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha01`. Based on [Jetpack Navigation 2.8.0-alpha05](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-alpha05)

___

# 1.6.2 (April 2024)

_Changes since 1.6.1_

## Features
#### Common
- [Support Kotlin `2.0.0-RC1`](https://github.com/JetBrains/compose-multiplatform/pull/4596)

## Fixes
#### iOS, Desktop, Web
- [Propagate composition locals to layers in the (re)composition phase](https://github.com/JetBrains/compose-multiplatform-core/pull/1233)
- [Add transactions to `FocusOwnerImpl` `takeFocus` and `releaseFocus` to prevent crash when a window is re-shown](https://github.com/JetBrains/compose-multiplatform-core/pull/1231)
- [Fix crash and allow selection in `SelectionContainer` to start when drag starts below the bounds of the visible text](https://github.com/JetBrains/compose-multiplatform-core/pull/1230)

#### iOS
- [Fixed crash in `TextField` when a last symbol is a carriage return symbol](https://github.com/JetBrains/compose-multiplatform-core/pull/1229)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.11](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.11)
- [Runtime 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.4)
- [UI 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.4)
- [Foundation 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.4)
- [Material 1.6.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.4)
- [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

# 1.6.1 (March 2024)

_Changes since 1.6.0_

## Features
#### Common
- [Support Kotlin 1.9.23](https://github.com/JetBrains/compose-multiplatform/pull/4452)

## Fixes
#### iOS, Desktop, Web
- [Consume less CPU for text field cursor blinking](https://github.com/JetBrains/compose-multiplatform-core/pull/1113)
- [Fix Material3 slider getting stuck while dragging](https://github.com/JetBrains/compose-multiplatform-core/pull/1135)
- [Fix `ExposedDropdownMenuBox` recomposing unnecessarily](https://github.com/JetBrains/compose-multiplatform-core/pull/1156)
#### iOS
- [Fix AppStore/TestFlight verification failure due to skiko/skia conflicting names with ICU library](https://github.com/JetBrains/compose-multiplatform-core/pull/1157)
- [Fix `Pager` scroll with scrollable content on top](https://github.com/JetBrains/compose-multiplatform-core/pull/1098)
- [Fix crash when selecting a character before the punctuation sign](https://github.com/JetBrains/compose-multiplatform-core/pull/1151)
- [Fix unintended state reset](https://github.com/JetBrains/compose-multiplatform-core/pull/1152)
- [Context menu isn't showing in empty textfield](https://github.com/JetBrains/compose-multiplatform-core/pull/1142)
- [Fix a logical error causing Compose tree corruption and consequent crash](https://github.com/JetBrains/compose-multiplatform-core/pull/1163)
- [Fix `TextField` crash when using speech-to-text](https://github.com/JetBrains/compose-multiplatform-core/pull/1183)
#### Web
- [Fix browser key events consumption](https://github.com/JetBrains/compose-multiplatform-core/pull/1124)
- [Fix `Popup` on resize](https://github.com/JetBrains/compose-multiplatform-core/pull/1166)
#### Desktop
- [Fix additional windows creation with `COMPONENT` layer type](https://github.com/JetBrains/compose-multiplatform-core/pull/1131)
- [Keep adding interop order consistent between platforms](https://github.com/JetBrains/compose-multiplatform-core/pull/1143)
#### Resources
- [Support three letters locales](https://github.com/JetBrains/compose-multiplatform/pull/4394)
- [Init resource accessors lazily and in external function to avoid MethodTooLargeException](https://github.com/JetBrains/compose-multiplatform/pull/4404)
- [Fix `Res` class generation if the library is declared as 'api'](https://github.com/JetBrains/compose-multiplatform/pull/4406)
- [Fix `regionCode` crash on iOS before 17](https://github.com/JetBrains/compose-multiplatform/pull/4473)
- [Fix package name of generated `Res` file when project is building for `JsTarget`](https://github.com/JetBrains/compose-multiplatform/pull/4300)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.10](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.10)
- [Runtime 1.6.3](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.3)
- [UI 1.6.3](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.3)
- [Foundation 1.6.3](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.3)
- [Material 1.6.3](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.3)
- [Material3 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.1)

# 1.6.0 (February 2024)

_Changes since 1.5.12_

## Highlights

An overview of highlights for this release is also available in the Compose Multiplatform documentation: [What's new in 1.6.0](https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose-1-6-0.html).

#### Common
- Resource library improvements ([an example project](https://github.com/JetBrains/compose-multiplatform/tree/8ee7531c424421657842a24a5c365db53ba19e18/components/resources/demo))
  - [Compile-time checking of resources through a generated `Res` class](https://github.com/JetBrains/compose-multiplatform/pull/3961)
  - [Introduce top level `composeResources` dir with `drawable`, `font`, `files`, `values/strings.xml` support](https://github.com/JetBrains/compose-multiplatform/pull/4127)
  - [Support for various screen densities, multiple languages and regions, and light and dark themes](https://github.com/JetBrains/compose-multiplatform/pull/4018)
- [Experimental support is available for tests in common code](https://github.com/JetBrains/compose-multiplatform-core/pull/978)
- [Support Kotlin 2.0.0-Beta4](https://github.com/JetBrains/compose-multiplatform/pull/4332)

#### iOS, Desktop, Web
- [Support `LineHeightStyle.Trim`](https://github.com/JetBrains/compose-multiplatform-core/pull/897)
- [Add a way to use fonts installed on the system](https://github.com/JetBrains/compose-multiplatform-core/pull/898)
- [Add support for text decoration line styles via PlatformTextStyle](https://github.com/JetBrains/compose-multiplatform-core/pull/999)

#### iOS
- Native-like caret behaviour by long/single taps in textfields([1](https://github.com/JetBrains/compose-multiplatform-core/pull/913), [2](https://github.com/JetBrains/compose-multiplatform-core/pull/858))
- [Basic accessibility support](https://github.com/JetBrains/compose-multiplatform-core/pull/1025)
- [`Popup`s/`Dialog`s can now be displayed outside a `ViewController` over native components](https://github.com/JetBrains/compose-multiplatform-core/pull/1031)
- [Allow selecting `Text` in `SelectionContainer` by double and triple tap](https://github.com/JetBrains/compose-multiplatform-core/pull/984)

#### Web
- [Compose for Web (Wasm) artifacts are available in Maven Central](https://github.com/JetBrains/compose-multiplatform-core/pull/914) <sub>_**Warning**: Kotlin 1.9.21 has [an issue](https://github.com/JetBrains/compose-multiplatform/issues/4230) with web target. Use Kotlin 1.9.22._</sub>

#### Desktop
- [Proper clipping of `SwingPanel` interop](https://github.com/JetBrains/compose-multiplatform-core/pull/915) <sub>_(under an experimental flag, see the link)_</sub>
- [`Popup`s/`Dialog`s can now be displayed outside the main window or panel and over native components](https://github.com/JetBrains/compose-multiplatform-core/pull/992) <sub>_(under an experimental flag, see the link)_</sub>

## Breaking changes

#### Common
- Resource library (`compose.components.resources`) changes
  - resources from `*Main\resources` should be moved to `*Main\composeResources\drawable`, `commonMain\composeResources\font` or `*Main\composeResources\files` depending on the resource type
  - `painterResource("resource.xml")` should be replaced by `painterResource(Res.drawable.resource)`
- `google()` maven repository is now required. Add this to `build.gradle.kts`:
  ```
  repositories {
      ...
      google()
  }
  ```
  If the project doesn't have it, there will be an error `Could not find androidx.annotation:annotation:...` or `Could not find org.jetbrains.compose.collection-internal:collection`.
- [Text with `lineHeight` set is trimmed by default](https://github.com/JetBrains/compose-multiplatform-core/pull/897)
- [Text with `fontSize` set without `lineHeight` inside `MaterialTheme` has different line height](https://issuetracker.google.com/issues/321872412)

#### iOS/Desktop/Web
- [Deprecate `public ComposeScene` in favour of `@InternalComposeUiApi MultiLayerComposeScene`](https://github.com/JetBrains/compose-multiplatform-core/pull/908)
- [Hide deprecated DropdownMenu overloads](https://github.com/JetBrains/compose-multiplatform-core/pull/1003)

#### iOS

- Separate platform views for `Popup`s/`Dialog`s that are enabled by default, unable to draw anything out of their own bounds (for example, a shadow of the topmost container). It will be fixed in a future version, but if you're relying on this behavior, you can switch back to the old behavior by setting the `platformLayers` parameter to `false`:
  ```kt
  ComposeUIViewController(configure = {
      platformLayers = false
  }) {
      // ...
  }
  ```

#### Desktop
- [Remove deprecated APIs in `TooltipArea` and `PointerEvent`](https://github.com/JetBrains/compose-multiplatform-core/pull/1029)

#### Web (k/js)
- Libraries which depend on earlier Compose Multiplatform version are not compatible with 1.6.0 anymore. This is because decoys generation was disabled. Projects which apply Compose Compiler plugin manually need to remove this argument: `plugin:androidx.compose.compiler.plugins.kotlin:generateDecoys=true`.

## Features

#### Common
- [Publish new platforms for `runtime-saveable`](https://github.com/JetBrains/compose-multiplatform-core/pull/894)

#### iOS
- [Adjust overscroll effect params to match iOS animations](https://github.com/JetBrains/compose-multiplatform-core/pull/1010/files)
- [Add ability to change opacity for compose view](https://github.com/JetBrains/compose-multiplatform-core/pull/1022)
- [Introduce `@Composable fun UIKitViewController`](https://github.com/JetBrains/compose-multiplatform-core/pull/882)
- [Remove iOS experimental flag in gradle.properties](https://github.com/JetBrains/compose-multiplatform/pull/3896)

#### Desktop
- [Support select till the end of the file / till the start of the file keyboard actions on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/989)
- [Add LinuxArm64 target to Compose runtime](https://github.com/JetBrains/compose-multiplatform-core/pull/977)
- [Add dedicated feature flags class for desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/945)

#### Web
- [Change the embedded font to Roboto Regular](https://github.com/JetBrains/skiko/pull/830)

#### HTML library
- [Add opportunity to use custom prefixes in `StyleSheet`](https://github.com/JetBrains/compose-multiplatform/pull/3015)
- [Add `sub`, `sup` and `blockquote` elements](https://github.com/JetBrains/compose-multiplatform/pull/3325)

#### Gradle Plugin
- [Add `ui-tooling-preview` alias](https://github.com/JetBrains/compose-multiplatform/pull/4190)
- [Add `compose.uiTest` dependency](https://github.com/JetBrains/compose-multiplatform/pull/4100)
- [Add `compose.components.uiUtil` dependency](https://github.com/JetBrains/compose-multiplatform/pull/3895)

## Fixes

#### iOS/Desktop/Web
- [Fix "DropdownMenu performs onDismissRequest twice"](https://github.com/JetBrains/compose-multiplatform-core/pull/1057)
- [Use a large rectangle for the picture bounds in RenderNodeLayer.drawLayer to prevent clipping](https://github.com/JetBrains/compose-multiplatform-core/pull/1090)
- [Fix closing scene during scroll animation](https://github.com/JetBrains/compose-multiplatform-core/pull/1096)
- [Fix "Using `painterResource(:DrawableResource)` outside desktop Window can produce `UnsatisfiedLinkError`"](https://github.com/JetBrains/skiko/pull/866)
- [Fix "Rect::makeLTRB expected l <= r" crashes](https://github.com/JetBrains/skiko/pull/867)
- [Fix "`TextLayoutResult#getLineForVerticalPosition` returns wrong value + slow performance"](https://github.com/JetBrains/compose-multiplatform-core/pull/1012)
- [Run all effects before sending synthetic events](https://github.com/JetBrains/compose-multiplatform-core/pull/1034)
- [`Font` constructor with lazy data loading](https://github.com/JetBrains/compose-multiplatform-core/pull/906)
- [Optimise `TextLayoutResult#getLineForOffset`](https://github.com/JetBrains/compose-multiplatform-core/pull/934)
- [Fix "SwingPanel/UIKitView doesn't apply Modifier.offset if it's after Modifier.size"](https://github.com/JetBrains/compose-multiplatform-core/pull/922)
- [DatePicker. Fix empty row](https://github.com/JetBrains/compose-multiplatform-core/pull/921)
- [DatePicker. Fix selection of the current day](https://github.com/JetBrains/compose-multiplatform-core/pull/877)
- [Fix `LayoutCoordinates.localToWindow` coordinates conversion for non-full Compose components](https://github.com/JetBrains/compose-multiplatform-core/pull/956)

#### iOS
- [Fixed unexpected fling animation over scrolling content](https://github.com/JetBrains/compose-multiplatform-core/pull/1039)
- [Fix UIKitView z-order](https://github.com/JetBrains/compose-multiplatform-core/pull/965)
- [Fix missing case for loading `SystemFont` on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1013)
- [Fix selection container crash](https://github.com/JetBrains/compose-multiplatform-core/pull/1016)
- [Fix `WindowInfo.containerSize` without `platformLayers` flag](https://github.com/JetBrains/compose-multiplatform-core/pull/1028)
- [Disable encoding on separate thread for iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/907)
- [Synchronise IME insets with iOS keyboard](https://github.com/JetBrains/compose-multiplatform-core/pull/875)

#### Desktop
- [Protect against `MouseInfo.getPointerInfo()` returning null in `WindowDraggableArea`](https://github.com/JetBrains/compose-multiplatform-core/pull/1049)
- [Support Rtl in `SplitPane`](https://github.com/JetBrains/compose-multiplatform/pull/4265)
- [Fix a native crash on `makeGL`](https://github.com/JetBrains/skiko/pull/869)
- [Add Arial and Consolas as backup fonts on Linux and mention font name when one fails to load](https://github.com/JetBrains/compose-multiplatform-core/pull/994)
- [Fix blurry app icon in the system application switcher](https://github.com/JetBrains/compose-multiplatform-core/pull/890)
- [Insert a new line on `NumPadEnter`](https://github.com/JetBrains/compose-multiplatform-core/pull/988)
- [Don't restart the drag gesture when the `onDrag(matcher=` changes](https://github.com/JetBrains/compose-multiplatform-core/pull/976)
- [Fix "Resizing window on desktop sometimes triggers onClick handlers of Composables"](https://github.com/JetBrains/compose-multiplatform-core/pull/949)
- [Fix "`ComposePanel` doesn't calculate its preferredSize correctly when it's called before doLayout"](https://github.com/JetBrains/compose-multiplatform-core/pull/884)
- [Fix input methods on JBR, disable input methods when we lose focus](https://github.com/JetBrains/compose-multiplatform-core/pull/881)
- [Fix "BasicTextField could not input any Chinese character when using JBR"](https://github.com/JetBrains/compose-multiplatform-core/pull/885)
- [Take into account `enabled` in `scrollable` for mouse input](https://github.com/JetBrains/compose-multiplatform-core/pull/880)
- [Fix NPE for getComponentAfter/Before in ComposePanel](https://github.com/JetBrains/compose-multiplatform-core/pull/878)
- [Transparency support for D3D](https://github.com/JetBrains/skiko/pull/837) <sub>_(previously it supported via fallback on OpenGL)_</sub>

#### Web
- [Add a `SystemThemeObserver` implementation for wasmJs](https://github.com/JetBrains/compose-multiplatform-core/pull/998)
- [Fix keyboard events with meta key on wasm/js targets](https://github.com/JetBrains/compose-multiplatform-core/pull/1088)
- [Added WASM to `components.uiToolingPreview` library](https://github.com/JetBrains/compose-multiplatform/pull/4286)
- [Fix "The cursor is invisible in compose web"](https://github.com/JetBrains/skiko/pull/846)
- [Use an alternative implementation of `Image.toBitmap`](https://github.com/JetBrains/compose-multiplatform-core/pull/917)

#### Gradle Plugin
- [Fix failing when `org.jetbrains.compose` is applied from a script plugin](https://github.com/JetBrains/compose-multiplatform/pull/3951)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.8](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.8)
- [Runtime 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.1)
- [UI 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.1)
- [Foundation 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.1)
- [Material 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.1)
- [Material3 1.2.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.0)

See [the announce](https://android-developers.googleblog.com/2024/01/whats-new-in-jetpack-compose-january-24-release.html) of Jetpack Compose 1.6. Notes:
- `Changes to default font padding` affects only Android target.
- `Added support for selection by mouse, including text.` concerns only Android. Other targets supported it before 1.6.
- Some of the features are not ported yet (they will be ported in the next versions):
  - [BasicTextField2](https://github.com/JetBrains/compose-multiplatform/issues/4218)
  - [Support for nonlinear font scaling](https://github.com/JetBrains/compose-multiplatform/issues/4305)
  - [Multipaltform Drag and drop](https://github.com/JetBrains/compose-multiplatform/issues/4235). It works only for Android right now. For desktop there is the old API `Modifier.onExternalDrag`
  - [MultiParagraph.fillBoundingBoxes](https://github.com/JetBrains/compose-multiplatform/issues/4236)

See [the announce](https://material.io/blog/material-3-compose-1-2) of Material 1.2.

# 1.6.0-rc03 (February 2024)
_Changes since 1.6.0-rc02_

- **[Support Kotlin 2.0.0-Beta4](https://github.com/JetBrains/compose-multiplatform/pull/4332)** <sub>Common</sub>
- _(prerelease fix)_ [Resources. Don't return a cached value when pass new args](https://github.com/JetBrains/compose-multiplatform/pull/4333) <sub>Common</sub>
- _(prerelease fix)_ [Fix crash "ComposeUIViewController.view should be attached to window](https://github.com/JetBrains/compose-multiplatform-core/pull/1110) <sub>iOS</sub>
- _(prerelease fix)_ [Fix App crashes when Compose SwfitUI View container removed from hierarchy](https://github.com/JetBrains/compose-multiplatform-core/pull/1114) <sub>iOS</sub>
- _(prerelease fix)_ [Fix Keyboard disappears on IME action](https://github.com/JetBrains/compose-multiplatform-core/pull/1118) <sub>iOS</sub>
- _(prerelease fix)_ [Fix `SelectionContainer` occasionally crashes on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1121) <sub>iOS</sub>
- _(prerelease fix)_ [Fix crash after open/close dropdown on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1127) <sub>iOS</sub>
- _(prerelease fix)_ [Fix Password popup disappears only on the second focused TextField](https://github.com/JetBrains/compose-multiplatform-core/pull/1128) <sub>iOS</sub>
- _(prerelease fix)_ [Fix mouse input above SwingPanel](https://github.com/JetBrains/compose-multiplatform-core/pull/1119) <sub>Desktop</sub>
- _(prerelease fix)_ [Fix non working accessibility on Desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/1129) <sub>Desktop</sub>
- _(prerelease fix)_ [Resources. Fix import of Android flavors](https://github.com/JetBrains/compose-multiplatform/pull/4319) <sub>Android</sub>

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.8](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.8)
- [Runtime 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.1)
- [UI 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.1)
- [Foundation 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.1)
- [Material 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.1)
- [Material3 1.2.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.0)

# 1.6.0-rc02 (February 2024)
_Changes since 1.6.0-rc01_

## Breaking changes
_since 1.6.0-beta01_
- When the resource library is used, resources should be imported explicitly

> To quickly change your code to a new state, replace this:
> ```
> import <modulePackage>.generated.resources.Res
> ```
> by this:
> ```
> import <modulePackage>.generated.resources.*
> ```
> and perform "Code - Optimize Imports" to apply the project code style. If code style doesn't allow wildcrads, `import <modulePackage>.generated.resources.*` will be replaced by explicit imports.

## iOS/desktop/web

### Fixes
- _(prerelease fix)_ [Fix "AlertDialog doesn't work with ProvidableCompositionLocal"](https://github.com/JetBrains/compose-multiplatform-core/pull/1104)

## Resource library

### Fixes
- _(prerelease fix)_ [Fix resource accessors compilation when there are huge number of resource files](https://github.com/JetBrains/compose-multiplatform/pull/4294)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.8](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.8)
- [Runtime 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.1)
- [UI 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.1)
- [Foundation 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.1)
- [Material 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.1)
- [Material3 1.2.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.0)

# 1.6.0-rc01 (February 2024)
_Changes since 1.6.0-beta02_

## Known issues and solutions
### Could not find "org.jetbrains.compose.annotation-internal:annotation"
_(or org.jetbrains.compose.collection-internal:collection)_, _[link](https://github.com/JetBrains/compose-multiplatform/issues/4277)_

It happens because some library depends on `1.6.0-beta02` which isn't binary compatible with `1.6.0-rc01`.
To find this library, call `./gradlew shared:dependencies` (replace `shared` by your main module). Downgrade this library or ask the library author to upgrade it to `1.6.0-rc01`.

### Could not find androidx.annotation:annotation:...
_(or org.jetbrains.compose.collection-internal:collection)_

It happens because `1.6.0` depends on [collection](https://developer.android.com/jetpack/androidx/releases/collection) and [annnotation](https://developer.android.com/jetpack/androidx/releases/annotation) libraries that are available only in the Google Maven repository.

To solve this, add `google()` maven repository  to `build.gradle.kts`:
```
repositories {
    ...
    google()
}
```
## iOS/desktop/web

### Fixes
- [Fix "DropdownMenu performs onDismissRequest twice"](https://github.com/JetBrains/compose-multiplatform-core/pull/1057)
- [Use a large rectangle for the picture bounds in RenderNodeLayer.drawLayer to prevent clipping](https://github.com/JetBrains/compose-multiplatform-core/pull/1090)
- [Fix closing scene during scroll animation](https://github.com/JetBrains/compose-multiplatform-core/pull/1096)
- [Fix "Using `painterResource(:DrawableResource)` outside desktop Window can produce `UnsatisfiedLinkError`"](https://github.com/JetBrains/skiko/pull/866)
- [Fix "Rect::makeLTRB expected l <= r" crashes](https://github.com/JetBrains/skiko/pull/867)
- _(prerelease fix)_ [Commonize BasicTooltipBox](https://github.com/JetBrains/compose-multiplatform-core/pull/1092)
- _(prerelease fix)_ [Fix sharing composition locals with new platform layers](https://github.com/JetBrains/compose-multiplatform-core/pull/1086)

## iOS

### Fixes
- [Fixed unexpected fling animation over scrolling content](https://github.com/JetBrains/compose-multiplatform-core/pull/1039)
- _(prerelease fix)_ [Fix "Wrong scroll behaviour of LazyColumn inside HorizontalPager"](https://github.com/JetBrains/compose-multiplatform-core/pull/1097)
- _(prerelease fix)_ [Fix scene size after sending the app to background or changing orientation with open modal](https://github.com/JetBrains/compose-multiplatform-core/pull/1093)

## Desktop

### Fixes
- [Protect against `MouseInfo.getPointerInfo()` returning null in `WindowDraggableArea`](https://github.com/JetBrains/compose-multiplatform-core/pull/1049)
- [Support Rtl in `SplitPane`](https://github.com/JetBrains/compose-multiplatform/pull/4265)
- [Fix a native crash on `makeGL`](https://github.com/JetBrains/skiko/pull/869)
- _(prerelease fix)_ [Fix "Skiko RenderException" when creating `ComposePanel`](https://github.com/JetBrains/skiko/pull/858)

## Web

### Fixes
- [Add a `SystemThemeObserver` implementation for wasmJs](https://github.com/JetBrains/compose-multiplatform-core/pull/998)
- [Fix keyboard events with meta key on wasm/js targets](https://github.com/JetBrains/compose-multiplatform-core/pull/1088)
- [Added WASM to `components.uiToolingPreview` library](https://github.com/JetBrains/compose-multiplatform/pull/4286)
- [Fix "The cursor is invisible in compose web"](https://github.com/JetBrains/skiko/pull/846)

## Gradle Plugin

### Fixes
- _(prerelease fix)_ [Relocate a bundled `KotlinPoet` to the internal package](https://github.com/JetBrains/compose-multiplatform/pull/4239)

## Resource library

### Fixes
- _(prerelease fix)_ [Add a type name to the resource initializers](https://github.com/JetBrains/compose-multiplatform/pull/4240)
- _(prerelease fix)_ [Don't make resource IDs lowercased](https://github.com/JetBrains/compose-multiplatform/pull/4253)
- _(prerelease fix)_ [Clean code-gen directory if there was deleted a dependency on the res library](https://github.com/JetBrains/compose-multiplatform/pull/4257)
- _(prerelease fix)_ [Register all hierarchical compose resources in android compilation](https://github.com/JetBrains/compose-multiplatform/pull/4274)
- _(prerelease fix)_ [Fix fonts duplication in android app](https://github.com/JetBrains/compose-multiplatform/pull/4284)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.8](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.8)
- [Runtime 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.1)
- [UI 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.1)
- [Foundation 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.1)
- [Material 1.6.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.1)
- [Material3 1.2.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.0)

# 1.6.0-beta02 (February 2024)

_Changes since 1.6.0-beta01_

## Highlights
- [Basic accessibility support](https://github.com/JetBrains/compose-multiplatform-core/pull/1025) <sub>_iOS_</sub>
- [Popups/Dialogs can now be displayed outside a ViewController over native components on iOS by default](https://github.com/JetBrains/compose-multiplatform-core/pull/1031) <sub>_iOS_</sub>
- [Allow selecting `Text` in `SelectionContainer` by double and triple tap](https://github.com/JetBrains/compose-multiplatform-core/pull/984) <sub>_iOS_</sub>
- [Add support for text decoration line styles via PlatformTextStyle](https://github.com/JetBrains/compose-multiplatform-core/pull/999) <sub>_iOS, desktop, web_</sub>
- Bugfixes in the resource library (see below for the details)

## iOS/desktop/web

### Fixes
- [Fix "`TextLayoutResult#getLineForVerticalPosition` returns wrong value + slow performance"](https://github.com/JetBrains/compose-multiplatform-core/pull/1012)
- [Run all effects before sending synthetic events](https://github.com/JetBrains/compose-multiplatform-core/pull/1034)
- _(prerelease bug)_ [Fix the pointer icon in `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform-core/pull/1014)

## iOS

### Features
- [Adjust overscroll effect params to match iOS animations](https://github.com/JetBrains/compose-multiplatform-core/pull/1010/files)
- [Add ability to change opacity for compose view](https://github.com/JetBrains/compose-multiplatform-core/pull/1022)

### Fixes
- [Fix UIKitView z-order](https://github.com/JetBrains/compose-multiplatform-core/pull/965)
- [Fix missing case for loading `SystemFont` on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1013)
- [Fix selection container crash](https://github.com/JetBrains/compose-multiplatform-core/pull/1016)
- [Fix `WindowInfo.containerSize` without `platformLayers` flag](https://github.com/JetBrains/compose-multiplatform-core/pull/1028)
- _(prerelease fix)_ [Fix "textfield with visual transformation crashes after single tap"](https://github.com/JetBrains/compose-multiplatform-core/pull/1045)
- _(prerelease fix)_ [Fix selection handles crossed](https://github.com/JetBrains/compose-multiplatform-core/pull/1017)
- _(prerelease fix)_ [Fix CMPViewControllerMisuse error](https://github.com/JetBrains/compose-multiplatform-core/pull/1027)
- _(prerelease fix)_ [Fix selection handles with platformLayers=true](https://github.com/JetBrains/compose-multiplatform-core/pull/1023)
- _(prerelease fix)_ [Fix interaction handling for interop views](https://github.com/JetBrains/compose-multiplatform-core/pull/1032)

## Desktop

### Fixes
- [Add Arial and Consolas as backup fonts on Linux and mention font name when one fails to load](https://github.com/JetBrains/compose-multiplatform-core/pull/994)

### Breaking changes and deprecated API
- [Remove deprecated APIs in `TooltipArea` and `PointerEvent`](https://github.com/JetBrains/compose-multiplatform-core/pull/1029)

## HTML library
### Features
- [Add opportunity to use custom prefixes in `StyleSheet`](https://github.com/JetBrains/compose-multiplatform/pull/3015)

## Gradle Plugin

### Features
- [Add `ui-tooling-preview` alias](https://github.com/JetBrains/compose-multiplatform/pull/4190)

## Resource library

### Fixes
- _(prerelease fix)_ [Configure Android resources after AGP is applied and ignore hidden files in resources](https://github.com/JetBrains/compose-multiplatform/commit/3040ea85bbc81cb6d1e22d6928646509ee8b601f)
- _(prerelease fix)_ [Generate Res class if there is no common composeResource dir](https://github.com/JetBrains/compose-multiplatform/pull/4176)
- _(prerelease fix)_ [Support Res class generation in JVM only compose projects](https://github.com/JetBrains/compose-multiplatform/pull/4183)
- _(prerelease fix)_ [Support Compose resources for iOS tests](https://github.com/JetBrains/compose-multiplatform/pull/4185)
- _(prerelease fix)_ [Fix sub-module gradle properties for res class generation](https://github.com/JetBrains/compose-multiplatform/commit/ee26bf8beea595dce67fbe880aa86a8363d428ae)
- _(prerelease fix)_ [Fix Native xml parser](https://github.com/JetBrains/compose-multiplatform/pull/4207)
- _(prerelease fix)_ [Generate initializer functions in the Res file to avoid the `MethodTooLargeException`](https://github.com/JetBrains/compose-multiplatform/pull/4205)
- _(prerelease fix)_ [Improve handling of special characters in string resources](https://github.com/JetBrains/compose-multiplatform/pull/4220)
- _(prerelease fix)_ [Add a `ttf` font to the resources demo app](https://github.com/JetBrains/compose-multiplatform/commit/3c7260ea51157d423b3799bd339b682ffabdce06)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.8](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.8)
- [Runtime 1.6.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.0)
- [UI 1.6.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.0)
- [Foundation 1.6.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.0)
- [Material 1.6.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.0)
- [Material3 1.2.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.0-rc01)

# 1.5.12

## Common

### Features
- [Support Kotlin 1.9.22](https://github.com/JetBrains/compose-multiplatform/pull/4075)

## Desktop

### Fixes
- [Fix "BasicTextField NPE-crash on JBR 17.0.9 on Linux when clicked"](https://github.com/JetBrains/compose-multiplatform-core/pull/973)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
* [Compiler 1.5.8](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.8)
* [Runtime 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.4)
* [UI 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.4)
* [Foundation 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.4)
* [Material 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.4)
* [Material3 1.1.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.2)

# 1.6.0-beta01 (January 2024)

## Highlights

- Resource library improvements ([an example project](https://github.com/JetBrains/compose-multiplatform/tree/8ee7531c424421657842a24a5c365db53ba19e18/components/resources/demo))
  - [Compile-time checking of resources through a generated `Res` class](https://github.com/JetBrains/compose-multiplatform/pull/3961)
  - [Introduce top level `composeResources` dir with `drawable`, `font`, `files`, `values/strings.xml` support](https://github.com/JetBrains/compose-multiplatform/pull/4127)
  - [Support for various screen densities, multiple languages and regions, and light and dark themes](https://github.com/JetBrains/compose-multiplatform/pull/4018)
- [Experimental support is available for tests in common code](https://github.com/JetBrains/compose-multiplatform-core/pull/978)
- [Compose for Web (Wasm) artifacts are available in Maven Central](https://github.com/JetBrains/compose-multiplatform-core/pull/914). **Warning**: Kotlin 1.9.21 has [an issue](https://github.com/JetBrains/compose-multiplatform/issues/4230) with web target. Use Kotlin 1.9.22.
- iOS. Native-like caret behaviour by long/single taps in textfields([1](https://github.com/JetBrains/compose-multiplatform-core/pull/913), [2](https://github.com/JetBrains/compose-multiplatform-core/pull/858))
- [Support `LineHeightStyle.Trim`](https://github.com/JetBrains/compose-multiplatform-core/pull/897)
- [Desktop. Proper clipping of `SwingPanel` interop](https://github.com/JetBrains/compose-multiplatform-core/pull/915) _(under an experimental flag, see the link)_
- Popups/Dialogs can now be displayed outside the main window or panel over native components on iOS and desktop _(under experimental flags, see the links)_
  - [iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/920)
  - [Desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/992)
- [Add a way to use fonts installed on the system](https://github.com/JetBrains/compose-multiplatform-core/pull/898) _(desktop/web in this version, iOS in the next version, Android isn't supported)_

### Breaking changes
- [Text with `lineHeight` set is trimmed by default](https://github.com/JetBrains/compose-multiplatform-core/pull/897)
- [Text with `fontSize` set without `lineHeight` inside `MaterialTheme` has different line height](https://issuetracker.google.com/issues/321872412)
- Resource library (`compose.components.resources`) changes
  - resources from `*Main\resources` should be moved to `*Main\composeResources\drawable`, `commonMain\composeResources\font` or `*Main\composeResources\files` depending on the resource type
  - `painterResource("resource.xml")` should be replaced by `painterResource(Res.drawable.resource)`

## Known issues
- `compose.components.resources` library doesn't work yet if you apply `kotlin("jvm")` Gradle plugin. For now, it only works with `kotlin("multiplatform")`

## Common

### Features
- [Support Kotlin 1.9.22](https://github.com/JetBrains/compose-multiplatform/pull/4075)
- [Publish new platforms for `runtime-saveable`](https://github.com/JetBrains/compose-multiplatform-core/pull/894)

## iOS/desktop/web

### Features
- [`Font` constructor with lazy data loading](https://github.com/JetBrains/compose-multiplatform-core/pull/906)

### Fixes
- [Optimise `TextLayoutResult#getLineForOffset`](https://github.com/JetBrains/compose-multiplatform-core/pull/934)
- [Fix "SwingPanel/UIKitView doesn't apply Modifier.offset if it's after Modifier.size"](https://github.com/JetBrains/compose-multiplatform-core/pull/922)
- [DatePicker. Fix empty row](https://github.com/JetBrains/compose-multiplatform-core/pull/921)
- [DatePicker. Fix selection of the current day](https://github.com/JetBrains/compose-multiplatform-core/pull/877)
- [Fix `LayoutCoordinates.localToWindow` coordinates conversion for non-full Compose components](https://github.com/JetBrains/compose-multiplatform-core/pull/956)

### Breaking changes and deprecated API
- [Deprecate `public ComposeScene` in favour of `@InternalComposeUiApi MultiLayerComposeScene`](https://github.com/JetBrains/compose-multiplatform-core/pull/908)
- [Hide deprecated DropdownMenu overloads](https://github.com/JetBrains/compose-multiplatform-core/pull/1003)

## iOS

### Features
- [Introduce `@Composable fun UIKitViewController`](https://github.com/JetBrains/compose-multiplatform-core/pull/882)
- [Remove iOS experimental flag in gradle.properties](https://github.com/JetBrains/compose-multiplatform/pull/3896)

### Fixes
- [Disable encoding on separate thread for iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/907)
- [Synchronise IME insets with iOS keyboard](https://github.com/JetBrains/compose-multiplatform-core/pull/875)

## Desktop

### Features
- [Support select till the end of the file / till the start of the file keyboard actions on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/989)
- [Add LinuxArm64 target to Compose runtime](https://github.com/JetBrains/compose-multiplatform-core/pull/977)
- [Add dedicated feature flags class for desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/945)

### Fixes
- [Fix blurry app icon in the system application switcher](https://github.com/JetBrains/compose-multiplatform-core/pull/890)
- [Insert a new line on `NumPadEnter`](https://github.com/JetBrains/compose-multiplatform-core/pull/988)
- [Don't restart the drag gesture when the `onDrag(matcher=` changes](https://github.com/JetBrains/compose-multiplatform-core/pull/976)
- [Fix "BasicTextField NPE-crash on JBR 17.0.9 on Linux when clicked"](https://github.com/JetBrains/compose-multiplatform-core/pull/973)
- [Fix "Resizing window on desktop sometimes triggers onClick handlers of Composables"](https://github.com/JetBrains/compose-multiplatform-core/pull/949)
- [Fix "`ComposePanel` doesn't calculate its preferredSize correctly when it's called before doLayout"](https://github.com/JetBrains/compose-multiplatform-core/pull/884)
- [Fix input methods on JBR, disable input methods when we lose focus](https://github.com/JetBrains/compose-multiplatform-core/pull/881)
- [Fix "BasicTextField could not input any Chinese character when using JBR"](https://github.com/JetBrains/compose-multiplatform-core/pull/885)
- [Take into account `enabled` in `scrollable` for mouse input](https://github.com/JetBrains/compose-multiplatform-core/pull/880)
- [Fix NPE for getComponentAfter/Before in ComposePanel](https://github.com/JetBrains/compose-multiplatform-core/pull/878)
- [Transparency support for D3D](https://github.com/JetBrains/skiko/pull/837) _(previously it supported via fallback on OpenGL)_

## Web
### Features
- [Change the embedded font to Roboto Regular](https://github.com/JetBrains/skiko/pull/830)

### Fixes
- [Use an alternative implementation of `Image.toBitmap`](https://github.com/JetBrains/compose-multiplatform-core/pull/917)

## HTML library
### Features
- [Add `sub`, `sup` and `blockquote` elements](https://github.com/JetBrains/compose-multiplatform/pull/3325)

## Gradle Plugin

### Features
- [Add `compose.uiTest` dependency](https://github.com/JetBrains/compose-multiplatform/pull/4100)
- [Add `compose.components.uiUtil` dependency](https://github.com/JetBrains/compose-multiplatform/pull/3895)

### Fixes
- [Fix failing when `org.jetbrains.compose` is applied from a script plugin](https://github.com/JetBrains/compose-multiplatform/pull/3951)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.5.8](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.8)
- [Runtime 1.6.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.6.0-rc01)
- [UI 1.6.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.0-rc01)
- [Foundation 1.6.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.6.0-rc01)
- [Material 1.6.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material#1.6.0-rc01)
- [Material3 1.1.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.2)

# 1.5.11 (November 2023)

## Common
### Features
* [Support Kotlin 1.9.21](https://github.com/JetBrains/compose-multiplatform/pull/3966)
* [Support Kotlin 2.0.0-Beta1](https://github.com/JetBrains/compose-multiplatform/pull/3934)

## iOS

### Fixes
* [Fix crash at GrGpuResource::release()](https://github.com/JetBrains/compose-multiplatform/issues/3862)
* [Fix crash with custom font](https://github.com/JetBrains/compose-multiplatform/issues/3822)
* [Composable functions returning null cause crash](https://github.com/JetBrains/compose-multiplatform/issues/3900) (fixed in Kotlin 1.9.21 and JetBrains Compose Compiler 1.5.4)

## Desktop

### Fixes
* [Fix race condition in animation tests](https://github.com/JetBrains/compose-multiplatform-core/pull/910)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
* [Compiler 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.4)
* [Runtime 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.4)
* [UI 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.4)
* [Foundation 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.4)
* [Material 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.4)
* [Material3 1.1.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.2)

# 1.5.10 (October 2023)
> This is a combined changelog from the prerelease versions:
> - [1.5.0-beta01](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.0-beta01)
> - [1.5.0-beta02](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.10-beta02)
> - [1.5.0-rc01](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.10-rc01)
> - [1.5.0-rc02](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.10-rc02)

## Common
### Features
* [Support Kotlin 1.9.20](https://github.com/JetBrains/compose-multiplatform/pull/3884)
* Introduce Material 3 components in common
  * [`ModalBottomSheet`](https://github.com/JetBrains/compose-multiplatform-core/pull/794)
  * [`SearchBar` and `DockedSearchBar`](https://github.com/JetBrains/compose-multiplatform-core/pull/801)
  * [`ExposedDropDownMenu`](https://github.com/JetBrains/compose-multiplatform-core/pull/787)
* [Introduce Material component `ExposedDropDownMenu` in common](https://github.com/JetBrains/compose-multiplatform-core/pull/793)
* [Introduce `WindowInfo.containerSize` experimental api](https://github.com/JetBrains/compose-multiplatform-core/pull/785)
* [Implement `defaultTimePickerLayoutType` based on screen orientation](https://github.com/JetBrains/compose-multiplatform-core/pull/817)
* [Add an option to disable insets in `Popup`/`Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/833)
* [Commonize insets `Modifier`'s \(additionally to `WindowInsets.*`\)](https://github.com/JetBrains/compose-multiplatform/issues/3563)

### Fixes
* [`ExposedDropdownMenuBox.onExpandedChange` was not recomposed](https://github.com/JetBrains/compose-multiplatform/issues/3686)
* [Override `RootLayout` insets only in case of `usePlatformInsets`](https://github.com/JetBrains/compose-multiplatform-core/pull/854)
* [Don't send synthetic Move events before Press/Release for touch](https://github.com/JetBrains/compose-multiplatform-core/pull/870)

## iOS

### Breaking changes
* [Having `kotlin.native.cacheKind = none` will result in a build error.](https://github.com/JetBrains/compose-multiplatform/pull/3667)

### Features
* [Compilation speed up due to enabling compiler caches for Kotlin 1.9.20+](https://github.com/JetBrains/compose-multiplatform/pull/3648)
* [Added crossfade animation during orientation change when used within UIKit hierarchy](https://github.com/JetBrains/compose-multiplatform-core/pull/778)
* [Compose Multiplatform should warn when `CADisableMinimumFrameDurationOnPhone` is not configured properly](https://github.com/JetBrains/compose-multiplatform/issues/3634)
* [Fast delete mode on software keyboard. When you hold a backspace, “turbo mode” is enabled after deleting the first 21 symbols. In turbo mode each tick deletes two words.](https://github.com/JetBrains/compose-multiplatform/issues/2991)
* [On a long scrollable TextFields, If it’s scrolled up to caret position while typing. Then it stopped on the line above the line with a caret.](https://github.com/JetBrains/compose-multiplatform-core/pull/804)
* [Add `UIViewController` lifetime hooks](https://github.com/JetBrains/compose-multiplatform-core/pull/779)
* [Implement iOS native feel scrolls for large text fields](https://github.com/JetBrains/compose-multiplatform-core/pull/771)
* Improve rendering performance
  * [Avoid redundant compositing](https://github.com/JetBrains/compose-multiplatform-core/pull/813)
  * [Don't send redundant synthetic moves](https://github.com/JetBrains/compose-multiplatform-core/pull/819)
  * [Postpone `CAMetalDrawable` acquisition](https://github.com/JetBrains/compose-multiplatform-core/pull/820)
  * [Move frame encoding to separate thread when possible](https://github.com/JetBrains/compose-multiplatform-core/pull/829)
* [Double tap and triple tap gesture handling in `TextField`s](https://github.com/JetBrains/compose-multiplatform/issues/2682)

### Fixes
* [Rendering synchronization of multiple `UIKitView`s within a screen](https://github.com/JetBrains/compose-multiplatform/issues/3534)
* [Today's date is not highlighted with a circle in the material3 datePicker on iOS](https://github.com/JetBrains/compose-multiplatform/issues/3591)
* [Fix text-to-speech crash in iOS 16.0.*](https://github.com/JetBrains/compose-multiplatform/issues/2984)
* [Compose window is shown before the first frame is rendered](https://github.com/JetBrains/compose-multiplatform/issues/3492)
* [iOS TextField, Compound emojis are being treated as many symbols](https://github.com/JetBrains/compose-multiplatform/issues/3104)
* [Use `CADisplayLink.targetTimestamp` value as the time for animation frames](https://github.com/JetBrains/compose-multiplatform-core/pull/796)
* [iOS. Improved performance on 120 hz devices](https://github.com/JetBrains/compose-multiplatform-core/pull/797)
* [Expanded `ModalBottomSheet`: scrim doesn't occupy complete screen](https://github.com/JetBrains/compose-multiplatform/issues/3701)
* [Fix interop view intercepting touches for popups](https://github.com/JetBrains/compose-multiplatform-core/pull/835)
* [Fix applying `WindowInsets` inside `Popup`/`Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/832)
* [Scrolling behavior bugs](https://github.com/JetBrains/compose-multiplatform/issues/3335)
* [`OutlinedTextField` label is clipped](https://github.com/JetBrains/compose-multiplatform/issues/3737)
* [Black screens with `UIKitView` after navigating away and navigating back](https://github.com/JetBrains/compose-multiplatform/issues/3749)
* [Long text field overscroll effect not clipped correctly](https://github.com/JetBrains/compose-multiplatform-core/pull/859)
* [First screen is recomposed twice](https://github.com/JetBrains/compose-multiplatform/issues/3778)
* [Bug with selection handle](https://github.com/JetBrains/compose-multiplatform-core/pull/869)
* [Ignore unpressed events during velocity calculation](https://github.com/JetBrains/compose-multiplatform-core/pull/848)
* [Crash with Asian languages in `TextField`](https://github.com/JetBrains/compose-multiplatform-core/pull/872/files)

## Desktop
### Features
* Improve accessibility support
  * [Implement `Role.DropdownList` via `AccessibleRole.COMBO_BOX`](https://github.com/JetBrains/compose-multiplatform-core/pull/822)
  * [Fix Compose `Role.Tab` to correctly translate to Java's `AccessibleRole.PAGE_TAB`](https://github.com/JetBrains/compose-multiplatform-core/pull/821)
  * [Implement support for `SemanticsProperties.ProgressBarRangeInfo`](https://github.com/JetBrains/compose-multiplatform-core/pull/830)

### Fixes
* [`LocalLayoutDirection` isn't propagated into `DialogWindow`](https://github.com/JetBrains/compose-multiplatform/issues/3382)
* [CompositionLocals given in application scope are not take into account in window scope (such as `LocalLayoutDirection`)](https://github.com/JetBrains/compose-multiplatform/issues/3571)
* [Fix accessibility issue with actions in popups](https://github.com/JetBrains/compose-multiplatform-core/pull/792)
* [Apply custom Dialog's scrim blend mode only when window is transparent](https://github.com/JetBrains/compose-multiplatform-core/pull/812)
* [Can't type in `TextField` placed in `ModalBottomSheet`](https://github.com/JetBrains/compose-multiplatform/issues/3703)
* [Accessibility not reporting changes](https://github.com/JetBrains/compose-multiplatform-core/pull/842)
* [Crash "LayoutNode should be attached to an owner exception"](https://github.com/JetBrains/compose-multiplatform/issues/3728)
* [Window loses its focus after recomposition of another window](https://github.com/JetBrains/compose-multiplatform/issues/2994)
* [Report semantic `ProgressBarRangeInfo` changes for accessibility](https://github.com/JetBrains/compose-multiplatform-core/pull/862)
* [Fix NPE for getComponentAfter/Before in ComposePanel](https://github.com/JetBrains/compose-multiplatform-core/pull/878)
* [Take into account `enabled` in `scrollable` for mouse input](https://github.com/JetBrains/compose-multiplatform-core/pull/880)
* [Improve accessibility on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/885)
* [Fix Chinese characters input when using JBR](https://github.com/JetBrains/compose-multiplatform-core/pull/881)

## Gradle Plugin
### Features
* [Add API to not apply the Compose Compiler plugin](https://github.com/JetBrains/compose-multiplatform/pull/3722)

### Fixes
* [Increase Kotlinx Serialization version used by the Compose Gradle Plugin](https://github.com/JetBrains/compose-multiplatform/issues/3479)
* [Switch to notarytool for notarization](https://github.com/JetBrains/compose-multiplatform/pull/3642)
* [Fix configuration cache for `syncComposeResourcesForIos`](https://github.com/JetBrains/compose-multiplatform/pull/3764)

## HTML library
### Features
* [SVG - Add fillOpacity attribute](https://github.com/JetBrains/compose-multiplatform/pull/3725)

## Web
### Features
* [Allow resources routing configuration (resources library)](https://github.com/JetBrains/compose-multiplatform/pull/3852)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
* [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
* [Runtime 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.4)
* [UI 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.4)
* [Foundation 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.4)
* [Material 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.4)
* [Material3 1.1.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.2)

# 1.5.10-rc02 (October 2023)

## Common
### Features

* [Support Kotlin 1.9.20-RC2](https://github.com/JetBrains/compose-multiplatform/pull/3861)

## Desktop
### Fixes
* [Fix NPE for getComponentAfter/Before in ComposePanel](https://github.com/JetBrains/compose-multiplatform-core/pull/878)
* [Take into account `enabled` in `scrollable` for mouse input](https://github.com/JetBrains/compose-multiplatform-core/pull/880)
* [Improve accessibility on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/885)
* [Fix Chinese characters input when using JBR](https://github.com/JetBrains/compose-multiplatform-core/pull/881)

## Web
### Features
* [Allow resources routing configuration (resources library)](https://github.com/JetBrains/compose-multiplatform/pull/3852)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
* [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
* [Runtime 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.4)
* [UI 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.4)
* [Foundation 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.4)
* [Material 1.5.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.4)
* [Material3 1.1.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.2)

# 1.5.10-rc01 (October 2023)

## Common

### Features
* [Support Kotlin 1.9.20-RC](https://github.com/JetBrains/compose-multiplatform/pull/3804)

### Fixes
* [Override `RootLayout` insets only in case of `usePlatformInsets`](https://github.com/JetBrains/compose-multiplatform-core/pull/854)
* [Don't send synthetic Move events before Press/Release for touch](https://github.com/JetBrains/compose-multiplatform-core/pull/870)

## iOS

### Fixes
* [Scrolling behavior bugs](https://github.com/JetBrains/compose-multiplatform/issues/3335)
* [`OutlinedTextField` label is clipped](https://github.com/JetBrains/compose-multiplatform/issues/3737)
* [Black screens with `UIKitView` after navigating away and navigating back](https://github.com/JetBrains/compose-multiplatform/issues/3749)
* [Long text field overscroll effect not clipped correctly](https://github.com/JetBrains/compose-multiplatform-core/pull/859)
* [First screen is recomposed twice](https://github.com/JetBrains/compose-multiplatform/issues/3778)
* [Bug with selection handle](https://github.com/JetBrains/compose-multiplatform-core/pull/869)
* [Ignore unpressed events during velocity calculation](https://github.com/JetBrains/compose-multiplatform-core/pull/848)
* [Crash with Asian languages in `TextField`](https://github.com/JetBrains/compose-multiplatform-core/pull/872/files)

## Desktop

### Fixes
* [Accessibility not reporting changes](https://github.com/JetBrains/compose-multiplatform-core/pull/842)
* [Crash "LayoutNode should be attached to an owner exception"](https://github.com/JetBrains/compose-multiplatform/issues/3728)
* [Window loses its focus after recomposition of another window](https://github.com/JetBrains/compose-multiplatform/issues/2994)
* [Report semantic `ProgressBarRangeInfo` changes for accessibility](https://github.com/JetBrains/compose-multiplatform-core/pull/862)

## Gradle Plugin

### Fixes
* [Fix configuration cache for `syncComposeResourcesForIos`](https://github.com/JetBrains/compose-multiplatform/pull/3764)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

* [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
* [Runtime 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.1)
* [UI 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.1)
* [Foundation 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.1)
* [Material 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.1)
* [Material3 1.1.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.2)


# 1.5.3 (October 2023)

## Desktop

### Fixes
- [Crash on text hover when accessibility is enabled on Windows](https://github.com/JetBrains/compose-multiplatform/issues/3742)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
- [Runtime 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.0)
- [UI 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.0)
- [Foundation 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.0)
- [Material 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.0)
- [Material3 1.1.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.1)

# 1.5.10-beta02 (September 2023)

## Common

### Features
* [Support kotlin 1.9.20-Beta2 with basic K2 support](https://github.com/JetBrains/compose-multiplatform/commit/393cfdd6638eee465b3c974fe9e6b3b0a1db57c1)
* [Implement `defaultTimePickerLayoutType` based on screen orientation](https://github.com/JetBrains/compose-multiplatform-core/pull/817)
* [Add an option to disable insets in `Popup`/`Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/833)
* [Commonize insets `Modifier`'s \(additionally to `WindowInsets.*`\)](https://github.com/JetBrains/compose-multiplatform/issues/3563)

### Fixes
* [`ExposedDropdownMenuBox.onExpandedChange` was not recomposed](https://github.com/JetBrains/compose-multiplatform/issues/3686)

## iOS

### Features
* Improve rendering performance
  * [Avoid redundant compositing](https://github.com/JetBrains/compose-multiplatform-core/pull/813)
  * [Don't send redundant synthetic moves](https://github.com/JetBrains/compose-multiplatform-core/pull/819)
  * [Postpone `CAMetalDrawable` acquisition](https://github.com/JetBrains/compose-multiplatform-core/pull/820)
  * [Move frame encoding to separate thread when possible](https://github.com/JetBrains/compose-multiplatform-core/pull/829)
* [Double tap and triple tap gesture handling in `TextField`s](https://github.com/JetBrains/compose-multiplatform/issues/2682)

### Fixes
* [Expanded `ModalBottomSheet`: scrim doesn't occupy complete screen](https://github.com/JetBrains/compose-multiplatform/issues/3701)
* [Fix interop view intercepting touches for popups](https://github.com/JetBrains/compose-multiplatform-core/pull/835) 
* [Fix applying `WindowInsets` inside `Popup`/`Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/832)


## Desktop

### Features

* Improve accessibility support
  * [Implement `Role.DropdownList` via `AccessibleRole.COMBO_BOX`](https://github.com/JetBrains/compose-multiplatform-core/pull/822)
  * [Fix Compose `Role.Tab` to correctly translate to Java's `AccessibleRole.PAGE_TAB`](https://github.com/JetBrains/compose-multiplatform-core/pull/821)
  * [Implement support for `SemanticsProperties.ProgressBarRangeInfo`](https://github.com/JetBrains/compose-multiplatform-core/pull/830)

### Fixes
* [Can't type in `TextField` placed in `ModalBottomSheet`](https://github.com/JetBrains/compose-multiplatform/issues/3703)

## Gradle Plugin

### Features
* [Add API to not apply the Compose Compiler plugin](https://github.com/JetBrains/compose-multiplatform/pull/3722)

### Fixes
* [Switch to notarytool for notarization](https://github.com/JetBrains/compose-multiplatform/pull/3642)

## HTML library

### Features
* [SVG - Add fillOpacity attribute](https://github.com/JetBrains/compose-multiplatform/pull/3725)


## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

* [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
* [Runtime 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.1)
* [UI 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.1)
* [Foundation 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.1)
* [Material 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.1)
* [Material3 1.1.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.2)


# 1.5.2 (September 2023)

## Desktop

### Fixes
- [Application crash on touch (using touchscreen) when onClick modifier is used](https://github.com/JetBrains/compose-multiplatform/issues/3655)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
- [Runtime 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.0)
- [UI 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.0)
- [Foundation 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.0)
- [Material 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.0)
- [Material3 1.1.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.1)


# 1.5.10-beta01 (September 2023)

## Common

### Features
* [Support Kotlin 1.9.20-Beta](https://github.com/JetBrains/compose-multiplatform/pull/3656)
* Introduce Material 3 components in common
  * [`ModalBottomSheet`](https://github.com/JetBrains/compose-multiplatform-core/pull/794)
  * [`SearchBar` and `DockedSearchBar`](https://github.com/JetBrains/compose-multiplatform-core/pull/801)
  * [`ExposedDropDownMenu`](https://github.com/JetBrains/compose-multiplatform-core/pull/787)
* [Introduce Material component `ExposedDropDownMenu` in common](https://github.com/JetBrains/compose-multiplatform-core/pull/793)
* [Introduce `WindowInfo.containerSize` experimental api](https://github.com/JetBrains/compose-multiplatform-core/pull/785)

## iOS

### Breaking changes
* [Having `kotlin.native.cacheKind = none` will result in a build error.](https://github.com/JetBrains/compose-multiplatform/pull/3667)

### Features

* [Compilation speed up due to enabling compiler caches for Kotlin 1.9.20+](https://github.com/JetBrains/compose-multiplatform/pull/3648)
* [Added crossfade animation during orientation change when used within UIKit hierarchy](https://github.com/JetBrains/compose-multiplatform-core/pull/778)
* [Compose Multiplatform should warn when `CADisableMinimumFrameDurationOnPhone` is not configured properly](https://github.com/JetBrains/compose-multiplatform/issues/3634)
* [Fast delete mode on software keyboard. When you hold a backspace, “turbo mode” is enabled after deleting the first 21 symbols. In turbo mode each tick deletes two words.](https://github.com/JetBrains/compose-multiplatform/issues/2991)
* [On a long scrollable TextFields, If it’s scrolled up to caret position while typing. Then it stopped on the line above the line with a caret.](https://github.com/JetBrains/compose-multiplatform-core/pull/804)
* [Add `UIViewController` lifetime hooks](https://github.com/JetBrains/compose-multiplatform-core/pull/779)
* [Implement iOS native feel scrolls for large text fields](https://github.com/JetBrains/compose-multiplatform-core/pull/771)

### Fixes
* [Rendering synchronization of multiple `UIKitView`s within a screen](https://github.com/JetBrains/compose-multiplatform/issues/3534)
* [Today's date is not highlighted with a circle in the material3 datePicker on iOS](https://github.com/JetBrains/compose-multiplatform/issues/3591)
* [Fix text-to-speech crash in iOS 16.0.*](https://github.com/JetBrains/compose-multiplatform/issues/2984)
* [Compose window is shown before the first frame is rendered](https://github.com/JetBrains/compose-multiplatform/issues/3492)
* [iOS TextField, Compound emojis are being treated as many symbols](https://github.com/JetBrains/compose-multiplatform/issues/3104)
* [Use `CADisplayLink.targetTimestamp` value as the time for animation frames](https://github.com/JetBrains/compose-multiplatform-core/pull/796)
* [iOS. Improved performance on 120 hz devices](https://github.com/JetBrains/compose-multiplatform-core/pull/797)

## Desktop

### Fixes
* [`LocalLayoutDirection` isn't propagated into `DialogWindow`](https://github.com/JetBrains/compose-multiplatform/issues/3382)
* [CompositionLocals given in application scope are not take into account in window scope (such as `LocalLayoutDirection`)](https://github.com/JetBrains/compose-multiplatform/issues/3571)
* [Fix accessibility issue with actions in popups](https://github.com/JetBrains/compose-multiplatform-core/pull/792)
* [Apply custom Dialog's scrim blend mode only when window is transparent](https://github.com/JetBrains/compose-multiplatform-core/pull/812)

## Gradle Plugin

### Fixes
* [Increase Kotlinx Serialization version used by the Compose Gradle Plugin](https://github.com/JetBrains/compose-multiplatform/issues/3479)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
* [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
* [Runtime 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.1)
* [UI 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.1)
* [Foundation 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.1)
* [Material 1.5.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.1)
* [Material3 1.1.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.1)

# 1.5.1 (September 2023)

## Common

### Features
- [Support kotlin 1.9.10](https://github.com/JetBrains/compose-multiplatform/pull/3616)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.5.3](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.3)
- [Runtime 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.0)
- [UI 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.0)
- [Foundation 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.0)
- [Material 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.0)
- [Material3 1.1.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.1)


# 1.5.0 (August 2023)
> This is a combined changelog from the prerelease versions:
> - [1.5.0-beta01](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.0-beta01)
> - [1.5.0-beta02](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.0-beta02)

## Common

### Features
- [`Dialog` API is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/632)
- [`Popup` API is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/611)
- [`WindowInsets` API is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/586)
- [A warning if `compose.kotlinCompilerPlugin` is set to `androidx.compose.compiler.compiler`](https://github.com/JetBrains/compose-multiplatform/pull/3313)
- [`androidx.compose.material.DropdownMenu` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/738)
- [`androidx.compose.material3.DropdownMenu` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/737)
- [`androidx.compose.material.AlertDialog` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/708)
- [`androidx.compose.material3.AlertDialog` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/710)
- [Add `PopupProperties.clippingEnabled` setting](https://github.com/JetBrains/compose-multiplatform-core/pull/740)
- material3. Support [`DatePicker`](https://github.com/JetBrains/compose-multiplatform-core/pull/717), [`DatePickerDialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/745) and `TimePicker`

### Fixes
- [`Popup`. Fix overriding `pressOwner` on multitouch](https://github.com/JetBrains/compose-multiplatform-core/pull/704)
- [Fix sending multiple touches in tests](https://github.com/JetBrains/compose-multiplatform-core/pull/688)
- [Fix multi-owner input processing](https://github.com/JetBrains/compose-multiplatform-core/pull/634)
- [Fix paragraph word boundary unicode handling](https://github.com/JetBrains/compose-multiplatform-core/pull/541)
- [Optimize the `Canvas` transformation functions](https://github.com/JetBrains/skiko/pull/724)
- [Fix click outside of common `Dialog` behaviour](https://github.com/JetBrains/compose-multiplatform-core/pull/707)

### API Changes
- [Change the default layout behavior of `AlertDialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/708)

## iOS

### Features
- [iOS native scroll and feel](https://github.com/JetBrains/compose-multiplatform-core/pull/609)
- [Simplify resource management](https://github.com/JetBrains/compose-multiplatform/pull/3340) (works without CocoaPods now)
- [`TextField` keyboardOptions, capitalization](https://github.com/JetBrains/compose-multiplatform/issues/2735)
- [`TextField`, keyboard behavior when have fullscreen size TextField](https://github.com/JetBrains/compose-multiplatform/issues/2752)
- [`TextField`, different behavior strategies on appearing the keyboard](https://github.com/JetBrains/compose-multiplatform/issues/3128)
- [Insets on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/577)
- [Support `Density.textSize` (Dynamic Type)](https://github.com/JetBrains/compose-multiplatform/issues/2567)
- [Change default fonts](https://github.com/JetBrains/compose-multiplatform-core/pull/552) (San Francisco is the default font)
- [UIKit public `LocalUIViewController`](https://github.com/JetBrains/compose-multiplatform-core/pull/501)
- [Support singleLine and `KeyboardAction`](https://github.com/JetBrains/compose-multiplatform-core/pull/699)

### Fixes
- [Fix dynamic framework support](https://github.com/JetBrains/skiko/pull/763)
- [Fix `TextField` context menu](https://github.com/JetBrains/compose-multiplatform/issues/3276)
- [Fix complex blending on iOS](https://github.com/JetBrains/skiko/pull/728)
- [`ViewConfiguration.touchSlop` value is quite low on iOS](https://github.com/JetBrains/compose-multiplatform/issues/3397)
- [Fix `topLeftOffset` calculation on iOS in Split View](https://github.com/JetBrains/compose-multiplatform-core/pull/678)
- [`Modifier.draggable` `onDragStopped` not called](https://github.com/JetBrains/compose-multiplatform/issues/3310)
- [`UIKitView`. Fix lifetime discrepancy within the composition](https://github.com/JetBrains/compose-multiplatform-core/pull/576/files)
- [Support 120hz screens](https://github.com/JetBrains/compose-multiplatform-ios-android-template/pull/17)
- [Fix incorrect Skiko render target on iOS Metal](https://github.com/JetBrains/compose-multiplatform-core/pull/554)
- [Properly detect content based text direction on native](https://github.com/JetBrains/compose-multiplatform-core/pull/514)
- Implemented all low-level functions to avoid random crashes
  - [Implement `TreeSet`](https://github.com/JetBrains/compose-multiplatform/issues/2878)
  - [Implement `AnnotatedString.transform`](https://github.com/JetBrains/compose-multiplatform-core/pull/523)
  - [Implement `WeakHashMap`](https://github.com/JetBrains/compose-multiplatform/issues/2877)
  - [Implement `typefacesCache`](https://github.com/JetBrains/compose-multiplatform/issues/2873)
  - [Implement `ExpireAfterAccessCache`](https://github.com/JetBrains/compose-multiplatform/issues/2871)
  - [Implement `NativeStringDelegate`](https://github.com/JetBrains/compose-multiplatform/issues/2876)
- [Fix memory leak in `ComposeUIViewController`](https://github.com/JetBrains/compose-multiplatform/issues/3201)
- [Manage Kotlin native cache kind automatically based on Kotlin version](https://github.com/JetBrains/compose-multiplatform/pull/3477) (`kotlin.native.cacheKind=none` is no longer needed)
- [Limit max `Dialog` and `Popup` size by safe area on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/732)
- [`TextField`, Korean characters are not normally entered](https://github.com/JetBrains/compose-multiplatform/issues/3101)
- [`ColorMatrix` value range for 5th column was incorrect on Skiko backed platforms](https://github.com/JetBrains/compose-multiplatform/issues/3461)
- [`isSystemDarkTheme` now automatically react to the system theme changes](https://github.com/JetBrains/compose-multiplatform-core/pull/715)

### API Changes
- Resource management was reimplemented. Follow [the guide in the PR](https://github.com/JetBrains/compose-multiplatform/pull/3340) to support new feautures
- [`CADisableMinimumFrameDurationOnPhone` added to the template's `Info.plist` to support 120Hz](https://github.com/JetBrains/compose-multiplatform-ios-android-template/pull/17#issue-1714201779)
- [`ComposeUIViewController`. Dispose composition on `viewDidDisappear`](https://github.com/JetBrains/compose-multiplatform-core/pull/747)

## Desktop

### Features
- [Swing interop. Experimental off-screen rendering on graphics](https://github.com/JetBrains/compose-multiplatform-core/pull/601) (hardware accelerated only on macOs at the moment)
- [Swing interop. `ComposePanel` that can be disposed manually](https://github.com/JetBrains/compose-multiplatform-core/pull/620)
- [Add semantic properties to `DialogWindow`, `Popup` and `Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/698)
- [Use `Segoe UI` as sans serif font on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/557)
- [Add ProGuard optimize flag](https://github.com/JetBrains/compose-multiplatform/pull/3408)

### Fixes
- [Fix loosing frames on macOs](https://github.com/JetBrains/skiko/pull/753)
- [Scrolling `LazyColumn` via mouse wheel stops working](https://github.com/JetBrains/compose-multiplatform/issues/3366)
- [`Slider` can be moved using keyboard, but cannot be submitted](https://github.com/JetBrains/compose-multiplatform/issues/2798)
- [Make one pixel beyond the scrollbar thumb react correctly to clicks](https://github.com/JetBrains/compose-multiplatform-core/pull/505)
- [`VerticalScrollbar` doesn't work properly when `LazyColumn` exists `StickyHeader`](https://github.com/JetBrains/compose-multiplatform/issues/2940)
- [All `Popup` overloads call dismiss on `Esc` key by default](https://github.com/JetBrains/compose-multiplatform-core/pull/712)
- [Fix Could not infer Java runtime version for Java home directory](https://github.com/JetBrains/compose-multiplatform/issues/3133)
- [Usage of deprecated `forEachGesture` in `Scrollbar.desktop.kt`](https://github.com/JetBrains/compose-multiplatform/issues/3045)
- Test framework fixes
  - [Implement idling resources for tests](https://github.com/JetBrains/compose-multiplatform-core/pull/599)
  - [Implement `SemanticsNodeInteraction.captureToImage()`](https://github.com/JetBrains/compose-multiplatform-core/pull/589)
  - [Wait until compose is idle before returning from `DesktopTestOwner.getRoots()`](https://github.com/JetBrains/compose-multiplatform-core/pull/588)
  - [When the test main clock is advanced, call `ComposeScene.render` with the current test time](https://github.com/JetBrains/compose-multiplatform-core/pull/584)
  - [Add timestamps to batched test input events, and advance the test clock accordingly when sending them](https://github.com/JetBrains/compose-multiplatform-core/pull/578)
  - [Tests support semanatic nodes inside `Window` and `DialogWindow`](https://github.com/JetBrains/compose-multiplatform-core/pull/697)

### API Changes
- [Rename desktop's `Dialog` to `DialogWindow`](https://github.com/JetBrains/compose-multiplatform-core/pull/661)

## Web

### Features
- [Let `ComposeWindow` accept a custom canvas id (html canvas element id)](https://github.com/JetBrains/compose-multiplatform-core/pull/626)
- [Support `Scrollbar`](https://github.com/JetBrains/compose-multiplatform-core/pull/571)
- [Make `CanvasBasedWindow` apply default styles, set title](https://github.com/JetBrains/compose-multiplatform-core/pull/722)

### Fixes
- [Workaround `ComposeWindow` multiple event listeners on resize](https://github.com/JetBrains/compose-multiplatform-core/pull/692)
- [Fix `Modifier.pointerHoverIcon` for browser apps](https://github.com/JetBrains/compose-multiplatform-core/pull/629)
- [Fux `RoundedCornerShape` with zero corners](https://github.com/JetBrains/compose-multiplatform/issues/3013)
- [Fix `ScrollConfig` in web target](https://github.com/JetBrains/compose-multiplatform-core/pull/628)
- Implemented low-level functions to avoid random crashes:
  - [Implement cache key in `PlatformFont`](https://github.com/JetBrains/compose-multiplatform-core/pull/671)
  - [implement platform detection to apply correct `KeyMapping` and `FontFamiliesMapping`](https://github.com/JetBrains/compose-multiplatform-core/pull/637)
  - [Implement `PlatformLocale` and `StringDelegate` for k/wasm](https://github.com/JetBrains/compose-multiplatform-core/pull/625)
  - [Add missing `getAndIncrement` in `AtomicLong` implementation for web](https://github.com/JetBrains/compose-multiplatform-core/pull/570)

## HTML library

### Features
- [Make HTML `TestUtils` wait functions cancellable](https://github.com/JetBrains/compose-multiplatform/pull/3320)

## Gradle Plugin

### Features
- [Add `runtimeSaveable` to Dependencies in compose gradle plugin](https://github.com/JetBrains/compose-multiplatform/pull/3449)

### API Changes
- [Raise error when Homebrew JDK is used for packaging](https://github.com/JetBrains/compose-multiplatform/pull/3451/files)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.0)
- [Runtime 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.0)
- [UI 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.0)
- [Foundation 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.0)
- [Material 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.0)
- [Material3 1.1.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.1)


# 1.5.0-beta02 (August 2023)

## Common

### Features
- [`androidx.compose.material.DropdownMenu` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/738)
- [`androidx.compose.material3.DropdownMenu` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/737)
- [`androidx.compose.material.AlertDialog` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/708)
- [`androidx.compose.material3.AlertDialog` is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/710)
- [Add `PopupProperties.clippingEnabled` setting](https://github.com/JetBrains/compose-multiplatform-core/pull/740)
- material3. Support [`DatePicker`](https://github.com/JetBrains/compose-multiplatform-core/pull/717), [`DatePickerDialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/745) and `TimePicker`

### API Changes
- [Change the default layout behavior of `AlertDialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/708)

## iOS

### Features
- [Support singleLine and `KeyboardAction`](https://github.com/JetBrains/compose-multiplatform-core/pull/699)

### Fixes
- [Fix memory leak in `ComposeUIViewController`](https://github.com/JetBrains/compose-multiplatform/issues/3201)
- [Manage Kotlin native cache kind automatically based on Kotlin version](https://github.com/JetBrains/compose-multiplatform/pull/3477) (`kotlin.native.cacheKind=none` is no longer needed)
- [Limit max `Dialog` and `Popup` size by safe area on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/732)
- [`TextField`, Korean characters are not normally entered](https://github.com/JetBrains/compose-multiplatform/issues/3101)
- [`ColorMatrix` value range for 5th column was incorrect on Skiko backed platforms](https://github.com/JetBrains/compose-multiplatform/issues/3461)
- [`isSystemDarkTheme` now automatically react to the system theme changes](https://github.com/JetBrains/compose-multiplatform-core/pull/715)

### API Changes
- [`ComposeUIViewController`. Dispose composition on `viewDidDisappear`](https://github.com/JetBrains/compose-multiplatform-core/pull/747)

## Web

### Features
- [Make `CanvasBasedWindow` apply default styles, set title](https://github.com/JetBrains/compose-multiplatform-core/pull/722)

## Gradle Plugin

### Features
- [Add `runtimeSaveable` to Dependencies in compose gradle plugin](https://github.com/JetBrains/compose-multiplatform/pull/3449)

### API Changes
- [Raise error when Homebrew JDK is used for packaging](https://github.com/JetBrains/compose-multiplatform/pull/3451/files)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.0)
- [Runtime 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.0)
- [UI 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.0)
- [Foundation 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.0)
- [Material 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.0)
- [Material3 1.1.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.1)

# 1.5.0-beta01 (July 2023)

## Common

### Features
- [`Dialog` API is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/632)
- [`Popup` API is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/611)
- [`WindowInsets` API is available to use from common source set](https://github.com/JetBrains/compose-multiplatform-core/pull/586)
- [A warning if `compose.kotlinCompilerPlugin` is set to `androidx.compose.compiler.compiler`](https://github.com/JetBrains/compose-multiplatform/pull/3313)

### Fixes
- [`Popup`. Fix overriding `pressOwner` on multitouch](https://github.com/JetBrains/compose-multiplatform-core/pull/704)
- [Fix sending multiple touches in tests](https://github.com/JetBrains/compose-multiplatform-core/pull/688)
- [Fix multi-owner input processing](https://github.com/JetBrains/compose-multiplatform-core/pull/634)
- [Fix paragraph word boundary unicode handling](https://github.com/JetBrains/compose-multiplatform-core/pull/541)
- [Optimize the `Canvas` transformation functions](https://github.com/JetBrains/skiko/pull/724)
- [Fix click outside of common `Dialog` behaviour](https://github.com/JetBrains/compose-multiplatform-core/pull/707)

## iOS

### Features
- [iOS native scroll and feel](https://github.com/JetBrains/compose-multiplatform-core/pull/609)
- [Simplify resource management](https://github.com/JetBrains/compose-multiplatform/pull/3340) (works without CocoaPods now)
- [`TextField` keyboardOptions, capitalization](https://github.com/JetBrains/compose-multiplatform/issues/2735)
- [`TextField`, keyboard behavior when have fullscreen size TextField](https://github.com/JetBrains/compose-multiplatform/issues/2752)
- [`TextField`, different behavior strategies on appearing the keyboard](https://github.com/JetBrains/compose-multiplatform/issues/3128)
- [Insets on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/577)
- [Support `Density.textSize` (Dynamic Type)](https://github.com/JetBrains/compose-multiplatform/issues/2567)
- [Change default fonts](https://github.com/JetBrains/compose-multiplatform-core/pull/552) (San Francisco is the default font)
- [UIKit public `LocalUIViewController`](https://github.com/JetBrains/compose-multiplatform-core/pull/501)

### Fixes
- [Fix dynamic framework support](https://github.com/JetBrains/skiko/pull/763)
- [Fix `TextField` context menu](https://github.com/JetBrains/compose-multiplatform/issues/3276)
- [Fix complex blending on iOS](https://github.com/JetBrains/skiko/pull/728)
- [`ViewConfiguration.touchSlop` value is quite low on iOS](https://github.com/JetBrains/compose-multiplatform/issues/3397)
- [Fix `topLeftOffset` calculation on iOS in Split View](https://github.com/JetBrains/compose-multiplatform-core/pull/678)
- [`Modifier.draggable` `onDragStopped` not called](https://github.com/JetBrains/compose-multiplatform/issues/3310)
- [`UIKitView`. Fix lifetime discrepancy within the composition](https://github.com/JetBrains/compose-multiplatform-core/pull/576/files)
- [Support 120hz screens](https://github.com/JetBrains/compose-multiplatform-ios-android-template/pull/17)
- [Fix incorrect Skiko render target on iOS Metal](https://github.com/JetBrains/compose-multiplatform-core/pull/554)
- [Properly detect content based text direction on native](https://github.com/JetBrains/compose-multiplatform-core/pull/514)
- Implemented all low-level functions to avoid random crashes
  - [Implement `TreeSet`](https://github.com/JetBrains/compose-multiplatform/issues/2878)
  - [Implement `AnnotatedString.transform`](https://github.com/JetBrains/compose-multiplatform-core/pull/523)
  - [Implement `WeakHashMap`](https://github.com/JetBrains/compose-multiplatform/issues/2877)
  - [Implement `typefacesCache`](https://github.com/JetBrains/compose-multiplatform/issues/2873)
  - [Implement `ExpireAfterAccessCache`](https://github.com/JetBrains/compose-multiplatform/issues/2871)
  - [Implement `NativeStringDelegate`](https://github.com/JetBrains/compose-multiplatform/issues/2876)

### API Changes
 - Resource management was reimplemented. Follow [the guide in the PR](https://github.com/JetBrains/compose-multiplatform/pull/3340) to support new features
- [`CADisableMinimumFrameDurationOnPhone` added to the template's `Info.plist` to support 120Hz](https://github.com/JetBrains/compose-multiplatform-ios-android-template/pull/17#issue-1714201779)

## Desktop

### Features
- [Swing interop. Experimental off-screen rendering on graphics](https://github.com/JetBrains/compose-multiplatform-core/pull/601) (hardware accelerated only on macOs at the moment)
- [Swing interop. `ComposePanel` that can be disposed manually](https://github.com/JetBrains/compose-multiplatform-core/pull/620)
- [Add semantic properties to `DialogWindow`, `Popup` and `Dialog`](https://github.com/JetBrains/compose-multiplatform-core/pull/698)
- [Use `Segoe UI` as sans serif font on Windows](https://github.com/JetBrains/compose-multiplatform-core/pull/557)
- [Add ProGuard optimize flag](https://github.com/JetBrains/compose-multiplatform/pull/3408)

### Fixes
- [Fix loosing frames on macOs](https://github.com/JetBrains/skiko/pull/753)
- [Scrolling `LazyColumn` via mouse wheel stops working](https://github.com/JetBrains/compose-multiplatform/issues/3366)
- [`Slider` can be moved using keyboard, but cannot be submitted](https://github.com/JetBrains/compose-multiplatform/issues/2798)
- [Make one pixel beyond the scrollbar thumb react correctly to clicks](https://github.com/JetBrains/compose-multiplatform-core/pull/505)
- [`VerticalScrollbar` doesn't work properly when `LazyColumn` exists `StickyHeader`](https://github.com/JetBrains/compose-multiplatform/issues/2940)
- [All `Popup` overloads call dismiss on `Esc` key by default](https://github.com/JetBrains/compose-multiplatform-core/pull/712)
- [Fix Could not infer Java runtime version for Java home directory](https://github.com/JetBrains/compose-multiplatform/issues/3133)
- [Usage of deprecated `forEachGesture` in `Scrollbar.desktop.kt`](https://github.com/JetBrains/compose-multiplatform/issues/3045)
- Test framework fixes
  - [Implement idling resources for tests](https://github.com/JetBrains/compose-multiplatform-core/pull/599)
  - [Implement `SemanticsNodeInteraction.captureToImage()`](https://github.com/JetBrains/compose-multiplatform-core/pull/589)
  - [Wait until compose is idle before returning from `DesktopTestOwner.getRoots()`](https://github.com/JetBrains/compose-multiplatform-core/pull/588)
  - [When the test main clock is advanced, call `ComposeScene.render` with the current test time](https://github.com/JetBrains/compose-multiplatform-core/pull/584)
  - [Add timestamps to batched test input events, and advance the test clock accordingly when sending them](https://github.com/JetBrains/compose-multiplatform-core/pull/578)
  - [Tests support semanatic nodes inside `Window` and `DialogWindow`](https://github.com/JetBrains/compose-multiplatform-core/pull/697)

### API Changes
- [Rename desktop's `Dialog` to `DialogWindow`](https://github.com/JetBrains/compose-multiplatform-core/pull/661)

## Web

### Features
- [Let `ComposeWindow` accept a custom canvas id (html canvas element id)](https://github.com/JetBrains/compose-multiplatform-core/pull/626)
- [Support `Scrollbar`](https://github.com/JetBrains/compose-multiplatform-core/pull/571)

### Fixes
- [Workaround `ComposeWindow` multiple event listeners on resize](https://github.com/JetBrains/compose-multiplatform-core/pull/692)
- [Fix `Modifier.pointerHoverIcon` for browser apps](https://github.com/JetBrains/compose-multiplatform-core/pull/629)
- [Fux `RoundedCornerShape` with zero corners](https://github.com/JetBrains/compose-multiplatform/issues/3013)
- [Fix `ScrollConfig` in web target](https://github.com/JetBrains/compose-multiplatform-core/pull/628)
- Implemented low-level functions to avoid random crashes:
  - [Implement cache key in `PlatformFont`](https://github.com/JetBrains/compose-multiplatform-core/pull/671)
  - [implement platform detection to apply correct `KeyMapping` and `FontFamiliesMapping`](https://github.com/JetBrains/compose-multiplatform-core/pull/637)
  - [Implement `PlatformLocale` and `StringDelegate` for k/wasm](https://github.com/JetBrains/compose-multiplatform-core/pull/625)
  - [Add missing `getAndIncrement` in `AtomicLong` implementation for web](https://github.com/JetBrains/compose-multiplatform-core/pull/570)

## HTML library

### Features
- [Make HTML `TestUtils` wait functions cancellable](https://github.com/JetBrains/compose-multiplatform/pull/3320)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.5.0)
- [Runtime 1.5.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.5.0-beta03)
- [UI 1.5.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.5.0-beta03)
- [Foundation 1.5.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.5.0-beta03)
- [Material 1.5.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-material#1.5.0-beta03)
- [Material3 1.1.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.1.1)

# 1.4.3 (July 2023)

## Common

### Features
- Support Kotlin 1.8.21, 1.8.22, 1.9.0

## iOS

### Fixes
- [Using Indication as a parameter in extension function doesn't compile on iOS](https://github.com/JetBrains/compose-multiplatform/issues/3086)
- [Compile error when using delegated property with @Composable getters in objects](https://github.com/JetBrains/compose-multiplatform/issues/3216)
- [Using a public property extensions inside a class or object with a @Composable backing delegate fails to link for iOS targets](https://github.com/JetBrains/compose-multiplatform/issues/3084)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.4.4](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.4.4)
- [Runtime 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.4.3)
- [UI 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.4.3)
- [Foundation 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.4.3)
- [Material 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-material#1.4.3)
- [Material3 1.0.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.1)

# 1.4.1 (June 2023)

## Common

### Fixes
- [Fix perspective transform usage](https://github.com/JetBrains/compose-multiplatform-core/pull/598)
- [After each ComposeScene.render phase, send apply notifications and perform the corresponding changes](https://github.com/JetBrains/compose-multiplatform-core/pull/563)
- [Fix awaitDragStartOnSlop to detect slop-passing on both axes](https://github.com/JetBrains/compose-multiplatform-core/pull/534)

## Desktop

### Fixes
- [Fix `DesktopDropdownMenuPositionProvider` to align with the correct horizontal side of the window](https://github.com/JetBrains/compose-multiplatform-core/pull/555)
- [Propagate LocalLayoutDirection into PopupLayout](https://github.com/JetBrains/compose-multiplatform-core/pull/562)
- [Fix missing scrolling events](https://github.com/JetBrains/compose-multiplatform-core/pull/527)
- [Make popups accessible on desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/439)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.4.4](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.4.4)
- [Runtime 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.4.3)
- [UI 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.4.3)
- [Foundation 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.4.3)
- [Material 1.4.3](https://developer.android.com/jetpack/androidx/releases/compose-material#1.4.3)
- [Material3 1.0.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.1)

# 1.4.0 (April 2023)

## Common

- [Added support for Kotlin `1.8.20`](https://github.com/JetBrains/compose-multiplatform/pull/3000)

### Features

- [Support perspective projection and `cameraDistance` parameter in `graphicsLayer`](https://github.com/JetBrains/compose-multiplatform-core/pull/422)
- [Support `brush` parameter in `TextStyle` for complex coloring](https://github.com/JetBrains/compose-multiplatform/issues/2814)
- [Support `drawStyle` parameter in `TextStyle` for drawing outlined text](https://github.com/JetBrains/compose-multiplatform-core/pull/470)
- [Support `blendMode` parameter when drawing text on Canvas](https://github.com/JetBrains/compose-multiplatform-core/pull/470)
- [Support compositing strategy on `graphicsLayer`](https://github.com/JetBrains/compose-multiplatform-core/pull/486)
- [Add `minLines` parameter to `BasicTextField`](https://github.com/JetBrains/compose-multiplatform-core/pull/469)
- [Support `painterResource` function in common source set](https://github.com/JetBrains/compose-multiplatform/pull/2793)

### Fixes

- [Actually remember provider in `rememberComponentRectPositionProvider`](https://github.com/JetBrains/compose-multiplatform-core/pull/444)
- [Fix merging multiple selection areas inside `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform/issues/2899)
- [Fix crash on selecting text if layout result is invalid](https://github.com/JetBrains/compose-multiplatform-core/pull/482)
- [Fix error on call an `expect`/`actual` function with default arguments and a Composable argument](https://github.com/JetBrains/compose-multiplatform/issues/2806)

## iOS

[iOS support is in Alpha now](../d44114d/README.md#ios)

## Desktop

### Features

- [Support external drag and drop](https://github.com/JetBrains/compose-multiplatform-core/pull/391)
- [Animate scroll initiated by mouse wheel input](https://github.com/JetBrains/compose-multiplatform-core/pull/438)
- [Remove AWT dependency in `rememberCursorPositionProvider`](https://github.com/JetBrains/compose-multiplatform-core/pull/432)
- Major scrollbar overhaul
  - [Scrollbar now works correctly in lazy lists when items vary in size](https://github.com/JetBrains/compose-multiplatform/issues/2679)
  - [Scrollbar now works correctly with lazy list `contentPadding`](https://github.com/JetBrains/compose-multiplatform/issues/2604)
  - [Scrollbar now works correctly in lazy list with spacing between items](https://github.com/JetBrains/compose-multiplatform-core/pull/380)
  - [Scrollbar now works correctly if the size of the scrollbar doesn't match the size of the widget it scrolls](https://github.com/JetBrains/compose-multiplatform-core/pull/368)
  - [Pressing the scrollbar track now works correctly](https://github.com/JetBrains/compose-multiplatform-core/pull/409)
  - [Added scrollbar support for lazy grids and text fields](https://github.com/JetBrains/compose-multiplatform/issues/1575)

### Fixes

- [Fix using a custom shape for an `AlertDialog`](https://github.com/JetBrains/compose-multiplatform/issues/1269)
- [Fix `AlertDialog` width to match the maximum of its content](https://github.com/JetBrains/compose-multiplatform/issues/2836)
- [Change `ContextMenuArea` to emit a single element, rather than two elements](https://github.com/JetBrains/compose-multiplatform/issues/2729)
- [Use state hoisting instead of global mouse position to determine where context menus, dropdown menus and tooltips should be displayed](https://github.com/JetBrains/compose-multiplatform-core/pull/437)
- [Limit error dialog width in case of long error message](https://github.com/JetBrains/compose-multiplatform-core/pull/441)
- [Fix window flashing background with unspecified size](https://github.com/JetBrains/compose-multiplatform-core/pull/442)
- [Fix crash `SplitPane` with a modifier](https://github.com/JetBrains/compose-multiplatform/issues/2214)
- [Fix applying `ComposePanel` visibility](https://github.com/JetBrains/compose-multiplatform/issues/2656)

## Web

- [`Compose for Web` is renamed to `Compose HTML Library`](https://github.com/JetBrains/compose-multiplatform/pull/2997)
- [The new experimental `Compose for Web` based on Kotlin/Wasm is available](../d44114d/README.md#web)

## Tools

- [Packaging native distributions now requires at least JDK 17](https://github.com/JetBrains/compose-multiplatform/blob/master/tutorials/Native_distributions_and_local_execution/README.md#customizing-jdk-version)

### Features

- [Provide relative path to jpackage --main-jar](https://github.com/JetBrains/compose-multiplatform/issues/1578)
- [Provide some entitlements on macOS by default](https://github.com/JetBrains/compose-multiplatform/pull/2974)


### Fixes

- [Unzip wix to build directory](https://github.com/JetBrains/compose-multiplatform/issues/2804)

## API Changes

- [Remove deprecated `SkiaRootForTest.processPointerInput`](https://github.com/JetBrains/compose-multiplatform-core/pull/456)
- [Remove deprecated `PointerEvent.awtEvent`, `KeyEvent.awtEvent`](https://github.com/JetBrains/compose-multiplatform-core/pull/456)
- [Make accidentally exposed to public `ParagraphLayouter` class `internal` as designed](https://github.com/JetBrains/compose-multiplatform-core/pull/426)
- [`androidx.compose.foundation.v2.ScrollbarAdapter` is introduced in order to allow scrollbars of a size different from the scrolled widget](https://github.com/JetBrains/compose-multiplatform-core/pull/368)

## Dependencies

This version of Compose Multiplatform is based on the next Jetpack Compose libraries:

- [Compiler 1.4.4](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.4.4)
- [Runtime 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.4.0)
- [UI 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.4.0)
- [Foundation 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.4.0)
- [Material 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.4.0)
- [Material3 1.0.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.1)

___

# 1.3.1 (March 2023)

## Common

- Added support for Kotlin 1.8.10

### Fixes
- [Layout in movableContent](https://github.com/JetBrains/compose-multiplatform-core/pull/413)
- [Unzip wix to build directory](https://github.com/JetBrains/compose-multiplatform/pull/2838)

## Desktop

### Fixes
- [Korean text input](https://github.com/JetBrains/compose-multiplatform-core/pull/406)
- [Size UndecoratedWindowResizer to the size of the window](https://github.com/JetBrains/compose-multiplatform-core/pull/388)
- [Fix sizing a window with unspecified size to its content's size](https://github.com/JetBrains/compose-multiplatform-core/pull/401)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.4.2](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.4.2)
- [Runtime 1.3.3](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.3.3)
- [UI 1.3.3](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.3.3)
- [Foundation 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.3.1)
- [Material 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.3.1)
- [Material3 1.0.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.1)

___

# 1.3.0 (January 2023)

## Common

### Fixes
- [Fix configuration cache issues](https://github.com/JetBrains/compose-jb/pull/2607)
- [Use global directory to download wix toolset](https://github.com/JetBrains/compose-jb/pull/2639)
- [IDEA plugin - Redo: set until-build to undefined value](https://github.com/JetBrains/compose-jb/pull/2658)

## Desktop

### Features
- [Desktop AlertDialog scrim color](https://github.com/JetBrains/androidx/pull/358)
- [Material 3 DropdownMenu Skiko support](https://github.com/JetBrains/androidx/pull/347)

### Fixes
- [Fix San Francisco font as a default (macOS)](https://github.com/JetBrains/androidx/pull/343)
- [Make ScrollbarAdapter for LazyList take contentPadding into account](https://github.com/JetBrains/androidx/pull/365)
- [Fix AlertDialog dismiss on click to Dialog content](https://github.com/JetBrains/androidx/pull/359)
- [Fix shaky scrolling of LazyColumn when the items are of varying size](https://github.com/JetBrains/androidx/pull/362)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.4.0)
- [Runtime 1.3.3](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.3.3)
- [UI 1.3.3](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.3.3)
- [Foundation 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.3.1)
- [Material 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.3.1)
- [Material3 1.0.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.1)

# 1.2.2 (December 2022)

## Desktop

## Features
- [Improve DSL for setting a custom Compose Plugin](https://github.com/JetBrains/compose-jb/pull/2527)
- [Add default ProGuard rules for Kotlin](https://github.com/JetBrains/compose-jb/commit/e6b2f0b8d0dab37529717942859ddc019ceaeecb)

### Fixes
- Fix [Performance degradation after some time if there are animations on the screen](https://github.com/JetBrains/compose-jb/issues/2455)
- Fix [SwingPanel throws NPE on focus request if nothing focusable](https://github.com/JetBrains/compose-jb/issues/2512)
- Fix [jvmTarget value specified using jvmToolchain is ignored and overwritten with "1.8"](https://github.com/JetBrains/compose-jb/issues/2511)
- Fix [Trying to delete a word via Ctrl+Backspace in an empty TextField causes an exception](https://github.com/JetBrains/compose-jb/issues/2466)

## Web
### Fixes
- Fix [Compose Web 1.2.1 regression: unexpected recompositions](https://github.com/JetBrains/compose-jb/issues/2539)
- Fix [Wrong instance remembered when several remember calls in one scope](https://github.com/JetBrains/compose-jb/issues/2535)


## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.3.2)
- [Runtime 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.2.1)
- [UI 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.2.1)
- [Foundation 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.2.1)
- [Material 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.2.1)
- [Material3 1.0.0-alpha14](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.0-alpha14)

# 1.2.1 (November 2022)
## Common

### Fixes
- [Fix `kotlinCompilerPlugin` property](https://github.com/JetBrains/compose-jb/pull/2396)
- Fix [runDistributable fails in v1.2.0-beta01](https://github.com/JetBrains/compose-jb/issues/2329)
- Fix [Packaging fails on JDK-19](https://github.com/JetBrains/compose-jb/issues/2328)

## Desktop

### Fixes
- Fix [`duplicate definition of resource file` when running packageRelease](https://github.com/JetBrains/compose-jb/issues/2393)
- [Fix support of GPU's which don't support DirectX 12 (Windows)](https://github.com/JetBrains/skiko/pull/603)

## Web

### Fixes
- [Support Kotlin 1.7.20](https://github.com/JetBrains/compose-jb/issues/2349)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.3.2)
- [Runtime 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.2.1)
- [UI 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.2.1)
- [Foundation 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.2.1)
- [Material 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.2.1)
- [Material3 1.0.0-alpha14](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.0-alpha14)

# 1.2.0 (October 2022)
## Common

### Features
- [Allow to use Compose on multiple Kotlin versions](https://github.com/JetBrains/compose-jb/pull/2366)
- [`import org.jetbrains.compose.compose` is no longer needed in build.gradle.kts](https://github.com/JetBrains/compose-jb/pull/2215)
- [Allow to use a custom Compose Compiler](https://github.com/JetBrains/compose-jb/pull/2347)

## Desktop

### Features
- [Support Kotlin 1.7.20](https://github.com/JetBrains/compose-jb/pull/2357)
- [ProGuard integration for packaging](https://github.com/JetBrains/compose-jb/pull/2313)
- [New experimental event API (onClick, onDrag, WindowInfo.keyboardModifiers)](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Mouse_Events#new-experimental-onclick-handlers-only-for-desktop-jvm-platform)
- Focus
    - [Make clickable, mouseClickable, toggleable request focus onClick](https://github.com/JetBrains/androidx/pull/257)
    - [Toggle a toggleable component on Space key](https://github.com/JetBrains/androidx/pull/262)
    - [Make Slider change the value with onKeyEvent for: all arrows, PgDn, PdUp, Home, End buttons](https://github.com/JetBrains/androidx/pull/254)
    - [Navigate drop down menu items using up and down arrows](https://github.com/JetBrains/androidx/pull/259)
- [Ability to override text context menu globally](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Context_Menu#custom-text-context-menu)
- [Context menu implementation for integrating into Swing applications](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Context_Menu#swing-interoperability)
- [Animated image component](https://github.com/JetBrains/compose-jb/pull/2015)
- [Show a new window/dialog on the same display](https://github.com/JetBrains/androidx/pull/312)
- [Change default Font on MacOs to San Francisco](https://github.com/JetBrains/androidx/pull/296)
- Support [performKeyInput](https://github.com/JetBrains/androidx/pull/278), [performMouseInput, performTextInput](https://github.com/JetBrains/androidx/pull/260) in tests.
- Focus switches seamlessly between Swing and Compose components using [SwingPanel](https://github.com/JetBrains/androidx/pull/229) or [ComposePanel](https://github.com/JetBrains/androidx/pull/228)
- [Documentation for how to package apps using Conveyor](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#available-tools)

### Fixes
- [Fix IDEA plugin compatibility](https://github.com/JetBrains/compose-jb/pull/2318)
- Fixes for Right-to-Left languages support
    - [LayoutDirection is detected from the system settings](https://github.com/JetBrains/androidx/pull/264)
    - [Fix RTL selection in a multiline text](https://github.com/JetBrains/androidx/pull/285)
    - [Fix cursor visual position at BiDi transition](https://github.com/JetBrains/androidx/pull/286)
    - [Fix the cursor position after the '\n' character in RTL](https://github.com/JetBrains/androidx/pull/268)
    - [In placeAutoMirrored for RTL, calculate the placement position using the placeable width respecting the constraints](https://github.com/JetBrains/androidx/pull/267)
    - [Placing the root content properly for RTL layout](https://github.com/JetBrains/androidx/pull/265)
- TextField
    - [Fix the cursor position when clicking at a position after a line-break](https://github.com/JetBrains/androidx/pull/284)
    - [Fix selection with End, Home](https://github.com/JetBrains/androidx/pull/279)
    - [Fix the cursor height on a new empty line](https://github.com/JetBrains/androidx/pull/277)
- [Fix DesktopMenu and DesktopAlertDialog to invoke dismiss callback once](https://github.com/JetBrains/androidx/pull/256)
- [Fix a large icon in macOs menu](https://github.com/JetBrains/androidx/pull/248)
- [Fix hover in LazyColumn](https://github.com/JetBrains/androidx/pull/249)
- [Fix pointerHoverIcon: update icon when it's changed conditionally](https://github.com/JetBrains/androidx/pull/231)
- [Fix focusable parameter for Window and Dialog](https://github.com/JetBrains/androidx/pull/225)
- [Fix 2 SplitPanel minor bugs](https://github.com/JetBrains/compose-jb/pull/2175)
- [Fix `java.lang.IllegalStateException: cannot open system clipboard` crash](https://github.com/JetBrains/skiko/pull/586)
- [Fix `System.setProperty("skiko.renderApi", "SOFTWARE"` on macOS](https://github.com/JetBrains/skiko/pull/599)

### API changes
- [Deprecate experimental pointerMoveFilter in favor of onPointerEvent](https://github.com/JetBrains/androidx/pull/247)
- [Deprecate experimental Modifier.mouseClickable in favor of Modifier.onClick](https://github.com/JetBrains/androidx/pull/243)

## Web

### Features
- Support Kotlin 1.7.10
- [Add support for transition in CSS api](https://github.com/JetBrains/compose-jb/pull/2228)
- [Add missing `background-blend-mode` property](https://github.com/JetBrains/compose-jb/pull/2128)
- [SVG: Publish custom attr functions](https://github.com/JetBrains/compose-jb/pull/2127)
- [Add list overload for `classes`](https://github.com/JetBrains/compose-jb/pull/2094)
- [Add support of dl, dt and dd](https://github.com/JetBrains/compose-jb/pull/1922)

### Fixes
- [Add pom information and license to Web artifacts](https://github.com/JetBrains/compose-jb/pull/2195)

### API changes
- [Deprecate 3 overloads of StyleScope.borderWidth with wrong parameter names](https://github.com/JetBrains/compose-jb/pull/2297)
- [Remove deprecated compose.web.web-widgets from the source code](https://github.com/JetBrains/compose-jb/pull/2294)
- [Remove unnecessary parameter for `required` attribute](https://github.com/JetBrains/compose-jb/pull/1988)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.3.2)
- [Runtime 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.2.1)
- [UI 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.2.1)
- [Foundation 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.2.1)
- [Material 1.2.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.2.1)
- [Material3 1.0.0-alpha14](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.0-alpha14)

# 1.1.1 (Mar 2022)
## Desktop

### Fixes
- [Support textIndent](https://github.com/JetBrains/compose-jb/issues/1733)
- [Fix cursor placement inside ligature](https://github.com/JetBrains/compose-jb/issues/1891)

### API changes
- [Compose doesn't depend on kotlinx-coroutines-swing](https://github.com/JetBrains/compose-jb/issues/1943)

If you use `Dispatchers.Swing` or `Dispatchers.Main` in your code, add this dependency into `build.gradle.kts`:
```
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
}
```

Also, usage of `Dispatchers.Swing` or `Dispatchers.Main` inside internals of Compose is implementation details, and can be changed in the future. If you need to avoid race conditions with Compose UI, you can obtain appropriate coroutine scope via `rememberCoroutineScope`:
```
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.application

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun main() = application {
    val scope = rememberCoroutineScope()
    val someApplicationObject = remember(scope) { SomeApplicationObject(scope) }
    
    DisposableEffect(Unit) {
        SomeGlobalObject.init(scope)
        onDispose {  }
    }
}
```

# 1.1.0 (Feb 2022)
## Desktop

### Features
- [Implement experimental accessibility support for Windows](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Accessibility)
- [Implement accessibility focus tracking by Tab](https://github.com/JetBrains/compose-jb/issues/1772)
- All bugfixes/features between  Jetpack Compose 1.1.0-beta02 and 1.1.0 (see the release notes for each module [here](https://developer.android.com/jetpack/androidx/releases/compose))

### Fixes
- Fixes for TextField ([1](https://github.com/JetBrains/compose-jb/issues/1834), [2](https://github.com/JetBrains/compose-jb/issues/1615), [3](https://github.com/JetBrains/compose-jb/issues/1781), [4](https://github.com/JetBrains/compose-jb/issues/1670))
- [Fix exception when we initialize a window with visible = false, undecorated = true](https://github.com/JetBrains/compose-jb/issues/1652)
- [Fix crash in ImageComposeScene](https://github.com/JetBrains/compose-jb/issues/1392)
- [Fixes for situations, when hover state doesn't disappear during scrolling](https://github.com/JetBrains/compose-jb/issues/1324#issuecomment-981148420)
- Fixes for Slider/Scrollbar dragging ([1](https://github.com/JetBrains/compose-jb/issues/643), [2](https://github.com/JetBrains/compose-jb/issues/691))
- [Fixed a case where [event.modifiersEx] does not provide info about the pressed mouse button (AWT)](https://github.com/JetBrains/androidx/pull/181)
- Fix [TextField crashes after selecting the text and then deactivating the text field](https://github.com/JetBrains/compose-jb/issues/1474)
- [Fix consuming events by mouse clickable](https://github.com/JetBrains/androidx/pull/178)
- [Hide top-level dialog from the taskbar](https://github.com/JetBrains/androidx/pull/177)

### API changes
- [The first frame of the window draws offscreen now](https://github.com/JetBrains/compose-jb/issues/1794). If your application has too long start, measure your first frame, and move the heavy logic to background or to the next frames. You can measure the first frame with this snippet:
```
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.singleWindowApplication

private var time by mutableStateOf(System.nanoTime())
private var frame by mutableStateOf(0)

fun main() = singleWindowApplication {
    if (frame == 0) {
        frame++
    } else if (frame == 1) {
        val duration = ((System.nanoTime() - time) / 1E6).toLong()
        println("First frame millis: $duration")
    }
}
```
- [`PointerEvent.awtEvent`, `KeyEvent.awtEvent` are deprecated](https://github.com/JetBrains/androidx/pull/198), use `PointerEvent.awtEventOrNull`, `KeyEvent.awtEventOrNull` instead. The event can be null, if it isn't sent by AWT (for example, Compose can send synthetic Move events on relayout)

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.1.0)
- [Runtime 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.1.0)
- [UI 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.1.0)
- [Foundation 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.1.0)
- [Material 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.1.0)
- [Material3 1.0.0-alpha05](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.0-alpha05)

# 1.0.1 (Dec 2021)
This is basically 1.0.0 that works with Kotlin 1.6.10

# 1.0.0 (Dec 2021)
## Desktop
### Features
- [Experimental support for material3](https://github.com/JetBrains/compose-jb/issues/1335)
- [All clickable components are focusable and react to Enter key](https://android-review.googlesource.com/c/platform/frameworks/support/+/1858797)
- [All clickable/draggable components no longer react to right clicks by default](https://github.com/JetBrains/compose-jb/issues/832)
- [Show the default error dialog when an error occurs](https://github.com/JetBrains/compose-jb/issues/663)
- [ContextMenu, AlertDialog, DropdownMenu close on Esc key by default](https://github.com/JetBrains/compose-jb/issues/1379)
- [Application by default calls exitProcess after its Composable is disposed](https://github.com/JetBrains/androidx/pull/69)

### API changes
- [Introduced experimental onPointerEvent (will replace mouseScrollFilter/pointerMoveFilter in the future)](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Mouse_Events#mouse-scroll-listeners)
- [Introduced PointerEventType.Scroll, which can be used in common code](https://android-review.googlesource.com/c/platform/frameworks/support/+/1864680)
- [MouseEvent.mouseEvent and KeyEvent.nativeKeyEvent are replaced by MouseEvent.awtEvent and KeyEvent.awtEvent](https://github.com/JetBrains/androidx/pull/87/files)
- [Experimental overload for loading resources painterResource(String, ResourceLoader)](https://github.com/JetBrains/androidx/blob/e2a3108b92d4c54b5780f9eeceb1712845961cd7/compose/ui/ui/src/desktopMain/kotlin/androidx/compose/ui/res/PainterResources.desktop.kt#L64)

## Web
### API changes
- compose.web.widgets is deprecated
- All CSSSelectors can't be instantiated via constructor now (they're private). [Please use functions provided in SelectorsScope instead](https://github.com/JetBrains/compose-jb/blob/master/web/core/src/jsMain/kotlin/org/jetbrains/compose/web/css/StyleSheetBuilder.kt#L37)
- [Some functions were made internal (either not related to compose-web or not intended for usage in apps):  buildCSS , StylePropertyList.nativeEquals , variableValue ,  buildCSSStyleRule , buildKeyframes , jsObject , CSSKeyframesRule.appendRule](https://github.com/JetBrains/compose-jb/pull/1509/files)
- CSSMediaRule: functions feature  and combine  were made extensions functions on GenericStyleSheetBuilder. This makes them consistent with the rest of functions which create CSSMediaQuery.MediaFeature  instances

## Dependencies
This version of Compose Multiplatform is based on the next Jetpack Compose libraries:
- [Compiler 1.1.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.1.0-beta02)
- [Runtime 1.1.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.1.0-beta02)
- [UI 1.1.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.1.0-beta02)
- [Foundation 1.1.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.1.0-beta02)
- [Material 1.1.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-material#1.1.0-beta02)
- [Material3 1.0.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.0.0-alpha03)

# 1.0.0-beta (Oct 2021)
## Common
- no Android artifacts are published anymore. Google-published artifacts are referenced instead. This approach eliminates compatibility issues.

## Desktop

### Features
- Accessibility support on MacOS
- Smart rendering fallback logic (no crashes on bad hardware/drivers anymore)
- Performance improvement of software rendering (up to 100%)
- Transparent window support
- `clickable` and `toggleable` components have a hoverable indication

### API changes
- DesktopMaterialTheme is deprecated, use MaterialTheme instead
- WindowSize is deprecated, use DpSize instead
- Modifier.pointerMoveFilter marked as Experimental, stable alternatives are Modifier.hoverable or Modifier.pointerInput
- Modifier.mouseScrollFilter marked as Experimental, this API will likely change in the future

### API breaking changes
- Old Window API (AppWindow, AppManager) was removed
- Modifier.pointerIcon is replaced with Modifier.pointerHoverIcon
- Tray can be used only inside ApplicationScope now
- Tray(hint=) replaced with Tray(tooltip=)

## Web

### Features
- SVG support

### API changes
- Controlled Inputs were added
- New API for testing - test-utils was added 

# 1.0.0-alpha (Aug 2021)
## Common
- Desktop, Web, and Android artifacts publish at the same time with the same version

## Desktop

### Features
- [Context menu support in selectable text](https://android-review.googlesource.com/c/platform/frameworks/support/+/1742314)
- [Cursor change behavior in text and pointer icon API](https://android-review.googlesource.com/c/platform/frameworks/support/+/1736714/12/compose/desktop/desktop/samples/src/jvmMain/kotlin/androidx/compose/desktop/examples/example1/Main.jvm.kt#357)
- [Mouse Clickable modifier](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Mouse_Events#mouse-rightmiddle-clicks-and-keyboard-modifiers)
- Tab navigation between text fields by default
- Resource packing to native distribution
- Support @Preview annotation in desktopMain sourceSet's (when the Compose MPP plugin is installed in IDEA)
- [New features for Composable menu (icons, shortcuts, mnemonics, radiob buttons, checkboxes](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Tray_Notifications_MenuBar_new#menubar)
- [Adaptive window size](https://github.com/JetBrains/compose-jb/blob/master/tutorials/Window_API_new/README.md#adaptive-window-size)
- Support Linux on ARM64
- [Support hidpi on some Linux distros](https://github.com/JetBrains/compose-jb/issues/188#issuecomment-891614869)
- Support resizing of undecorated resizable windows (`Window(undecorated=true, resizable=true, ...)`)

### API changes
- new Window API is no longer experimental
- old Window API is deprecated
- classes from `android.compose.desktop.*` moved to `androidx.compose.ui.awt.*` (ComposeWindow, ComposePanel, etc)
- `svgResource`/`vectorXmlResource`/`imageResource` replaced by painterResource

### API breaking changes
- Window level keyboard API for the old Window API removed
- Window(icon: BufferedImage) replaced by Window(icon: Painter)
- ContextMenu renamed to CursorDropdownMenu

## Web

### API changes
- [classes behave cumulatively](https://github.com/JetBrains/compose-jb/pull/690)
- [removed content builder for empty elements](https://github.com/JetBrains/compose-jb/issues/744)
- [Introduce CSS arithmetic operations](https://github.com/JetBrains/compose-jb/pull/761)
- [Improved the types of Inputs and input events](https://github.com/JetBrains/compose-jb/pull/799)
- [CSS Animations](https://github.com/JetBrains/compose-jb/pull/810)
- [All event types expose native properties](https://github.com/JetBrains/compose-jb/pull/887)
- [Added a complete list of HTML color aliases](https://github.com/JetBrains/compose-jb/issues/890)
- [Introduce support for CSS Grid API](https://github.com/JetBrains/compose-jb/issues/895)
- [Deprecate Color.RGB, Color.HSL etc. functions in favor of top-level rgb, hsl an so on](https://github.com/JetBrains/compose-jb/issues/902)
- [negate CSSNumeric value directly](https://github.com/JetBrains/compose-jb/issues/921)

### API breaking changes
- [boolean like attributes don't have any parameters anymore](https://github.com/JetBrains/compose-jb/pull/780)
- [removed input type specific event listeners](https://github.com/JetBrains/compose-jb/pull/861)
- [replaced maxWidth/minWidth media queries with prefixed names](https://github.com/JetBrains/compose-jb/issues/886)
- [Remove CSSVariables context and introduce specialized methods for adding String- and Number-valued CSS variables](https://github.com/JetBrains/compose-jb/issues/894)
- [inline style builder was moved into AttributeBuilder scope](https://github.com/JetBrains/compose-jb/pull/699)


# M4 (Jun 2021)
  - New experimental [Composable Window API](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Window_API_new)
  - [Tooltips](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Desktop_Components#tooltips)
  - Use [Metal renderer for macOS by default](https://github.com/JetBrains/skiko/pull/70)
  - [Expose a swing mouse event in Modifier.pointerInput](https://github.com/JetBrains/compose-jb/issues/129#issuecomment-784149646)
  - Improved [keyboard support in TextField](https://android-review.googlesource.com/c/platform/frameworks/support/+/1578803)
  - Avoid forcing discrete GPU on multi-GPU MacOS machines in [Skiko](https://github.com/JetBrains/skiko/pull/83) and [native distributions](https://github.com/JetBrains/compose-jb/issues/545)
  - [Make DropdownMenu focusable by default](https://github.com/JetBrains/compose-jb/issues/375)
  - [Scrollbar. get rid of itemCount and averageItemSize from rememberScrollbarAdapte](https://github.com/JetBrains/compose-jb/issues/181)
  - [Support scrollbars for LazyColumn with reverseLayout = true](https://github.com/JetBrains/compose-jb/issues/209)
  - Fix [memory leak](https://github.com/JetBrains/compose-jb/issues/538)
  - Fix [Scroll NaN rounding bug, desktop version](https://github.com/JetBrains/compose-jb/issues/304)
  - Fix [Dragging prevents pointer move events](https://github.com/JetBrains/compose-jb/issues/134)
  - Fix [Dragging window to another display makes Icon show up incorrectly](https://github.com/JetBrains/compose-jb/issues/677)
  - Fix ["Padding must be non-negative" after resizing window with Slider and Box](https://github.com/JetBrains/compose-jb/issues/367)
  - Breaking change [old Dialog/Menubar/Tray are moved to androidx.compose.ui.window.v1](https://android-review.googlesource.com/c/platform/frameworks/support/+/1685905)

# M3 (Feb 2021)
   - Improve [TextField](https://github.com/JetBrains/compose-jb/issues/277)
   - Support [SVG](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Image_And_Icons_Manipulations#loading-svg-images)
   - Support [vsync](https://github.com/JetBrains/skiko/pull/44),  [sync composition with rendering frames](https://android-review.googlesource.com/c/platform/frameworks/support/+/1534675)
   - Support [DirectX on Windows by default](https://github.com/JetBrains/skiko/pull/63)
   - Support [software rendering fallback](https://github.com/JetBrains/skiko/pull/56)
   - Implement [signing and notarization for macOS](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Signing_and_notarization_on_macOS)
   - Improve Swing interoperability support [Swing component in Compose hierarchy](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Swing_Integration#adding-a-swing-component-to-cfd-composition-using-swingpanel)
   - Support using [Compose in IntelliJ plugins](https://github.com/JetBrains/compose-jb/tree/master/examples/intelliJPlugin)
   - Skiko native binaries are now signed on macOS (x64 and arm)
   - Fix [Ambients are not transferred across pop ups](https://github.com/JetBrains/compose-jb/issues/135)
   - Fix [Laggy UI on Linux](https://github.com/JetBrains/compose-jb/issues/23)
   - Fix [Using AndroidX Compose specific dependencies for Android target](https://github.com/JetBrains/compose-jb/issues/272)

# M2 (Dec 2020)
   - Swing interoperability support (Compose in Swing frame)
   - Support [XML vector images](https://developer.android.com/guide/topics/graphics/vector-drawable-resources)
   - [Support for Gradle 6.6 and 6.7](https://github.com/JetBrains/compose-jb/issues/66)
   - [Support macOS 10.13 and 10.14](https://github.com/JetBrains/compose-jb/issues/76)
   - Support Apple Silicon (arm64) natively
   - [Support letter spacing in text](https://github.com/JetBrains/compose-jb/issues/82)
   - [Implemented desktop "ActualDialog" and "ActualPopup"](https://github.com/JetBrains/compose-jb/issues/19)
   - [Fix input method bar positioning](https://github.com/JetBrains/compose-jb/issues/67)
   - [Fix text alignment](https://github.com/JetBrains/compose-jb/issues/92)
   - [Fix dropdown/popup positioning](https://github.com/JetBrains/compose-jb/issues/139)
   - [Fix using with Compose in same app as Java FX](https://github.com/JetBrains/compose-jb/issues/17)
   - [Added screenshots of example apps](https://github.com/JetBrains/compose-jb/issues/90)

# M1 (Nov 2020)
   - Initial release
