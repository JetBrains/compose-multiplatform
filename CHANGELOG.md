# 1.9.3 (November 2025)

_Changes since 1.9.2_

## Fixes

### iOS

- Fix crash on iOS older than 17 when accessibility is enabled [#2541](https://github.com/JetBrains/compose-multiplatform-core/pull/2541)

### Gradle Plugin

- Support AGP 9.0.0 [#5391](https://github.com/JetBrains/compose-multiplatform/pull/5391)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.3`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.4)
  - [UI 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.4)
  - [Foundation 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.4)
  - [Material 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.4)
  - [Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0`. Based on [Jetpack Compose Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0`. Based on [Jetpack Compose Material3 Adaptive 1.2.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.6`. Based on [Jetpack Lifecycle 2.9.4](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.4)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.1`. Based on [Jetpack Navigation 2.9.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.4)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate*:1.3.6`. Based on [Jetpack Savedstate 1.3.3](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.3)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.10.0-beta01 (November 2025)

_Changes since 1.10.0-alpha03_

## Highlights

### Desktop

- Compose Hot Reload Gradle plugin is bundled with Compose Gradle plugin (no need to configure it separately) [#5444](https://github.com/JetBrains/compose-multiplatform/pull/5444)

## Known Issues

### Multiple Platforms

- Due to the bundled Hot Reload Gradle plugin, Compose Multiplatform requires Kotlin version 2.1.20 or higher [#5444](https://github.com/JetBrains/compose-multiplatform/pull/5444)

## Migration Notes

### Multiple Platforms

- Remove experimental annotation from `usePlatformInsets`, `useSoftwareKeyboardInset` and `scrimColor` in `DialogProperties` [#2495](https://github.com/JetBrains/compose-multiplatform-core/pull/2495)
- Remove experimental annotation from `usePlatformDefaultWidth`, `usePlatformInsets` in `PopupProperties` [#2495](https://github.com/JetBrains/compose-multiplatform-core/pull/2495)
- Deprecation level of `Popup` overloads without `PopupProperties` parameter changed from `WARNING` to `ERROR` [#2495](https://github.com/JetBrains/compose-multiplatform-core/pull/2495)
- Dependency aliases in Gradle plugin (e.g. `compose.ui`) are now deprecated. Use provided direct artifact coordinates and add them to the version catalog [#5462](https://github.com/JetBrains/compose-multiplatform/pull/5462)

## Features

### iOS

- Add support for self-sizing of UIKit interop views in Compose [#2485](https://github.com/JetBrains/compose-multiplatform-core/pull/2485)
- Add experimental `UIKitInteropProperties.placedAsOverlay` flag, which allows to place interop views above the Compose canvas. This allows interop views with a transparent background or shader effect to be used [#2501](https://github.com/JetBrains/compose-multiplatform-core/pull/2501)

### Web

- Esc button calls back navigation event on the web now [#2499](https://github.com/JetBrains/compose-multiplatform-core/pull/2499)
- Add support of `important` keyword for CSS properties [#5439](https://github.com/JetBrains/compose-multiplatform/pull/5439)

### Navigation

- Published `org.jetbrains.compose.material3.adaptive:adaptive-navigation3` library [#2516](https://github.com/JetBrains/compose-multiplatform-core/pull/2516)
- Add a new configuration option in `ComposeUIViewController` to set end-edge gestures behavior [#2519](https://github.com/JetBrains/compose-multiplatform-core/pull/2519)

## Fixes

### Multiple Platforms

- Align `roundToPx()` behavior between platforms: `NaN` value produces `0` instead of `IllegalArgumentException` on non-JVM platforms now [#2526](https://github.com/JetBrains/compose-multiplatform-core/pull/2526)
- Added previously missing `ExperimentalComposeUiApi` annotation to platform-specific `DragAndDropTransferData`, `DragAndDropEvent` and `ClipEntry` constructors on Desktop and Web. Working with these APIs already requires this annotation due to types of parameters, so it wasn't supposed to be marked as "stable" yet. You can follow [CMP-7624](https://youtrack.jetbrains.com/issue/CMP-7624) to track the updates about finalizing the shape of these APIs [#2538](https://github.com/JetBrains/compose-multiplatform-core/pull/2538)

### iOS

- Fix `onKeyEvent` handling when `Full Keyboard Access` is enabled [#2494](https://github.com/JetBrains/compose-multiplatform-core/pull/2494)
- Fix incorrect tap responses in `Dialog` when `LocalDensity` is modified [#2527](https://github.com/JetBrains/compose-multiplatform-core/pull/2527)
- _(prerelease fix)_ Fix crash when interacting with scrollables with overscroll [#2532](https://github.com/JetBrains/compose-multiplatform-core/pull/2532)

### Desktop

- Fix `SwingPanel` blocking mouse wheel scroll events from going to its parent [#2486](https://github.com/JetBrains/compose-multiplatform-core/pull/2486)

### Web

- Correct drag-and-drop behaviour on mobile devices [#2510](https://github.com/JetBrains/compose-multiplatform-core/pull/2510)
- Fix incorrect interpretation of `ontouchend` events [#2490](https://github.com/JetBrains/compose-multiplatform-core/pull/2490)
- Unblock autocomplete and the other keyboard features on mobile iOS [#2529](https://github.com/JetBrains/compose-multiplatform-core/pull/2529)
- Correct behaviour when a virtual keyboard suggestion was accepted while the cursor was in the middle of the word [#2530](https://github.com/JetBrains/compose-multiplatform-core/pull/2530)

### Resources

- Use Web Cache API for all resources to avoid repeated and redundant HTTP requests [#5379](https://github.com/JetBrains/compose-multiplatform/pull/5379)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.10.0-beta01`. Based on Jetpack Compose libraries:
  - [Runtime 1.10.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.10.0-beta01)
  - [UI 1.10.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.10.0-beta01)
  - [Foundation 1.10.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.10.0-beta01)
  - [Material 1.10.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-material#1.10.0-beta01)
  - [Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.10.0-alpha04`. Based on [Jetpack Compose Material3 1.5.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0-alpha07)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.3.0-alpha01`. Based on [Jetpack Compose Material3 Adaptive 1.3.0-alpha02](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.3.0-alpha02)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.10.0-alpha04`. Based on [Jetpack Lifecycle 2.10.0-beta01](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.10.0-beta01)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.1`. Based on [Jetpack Navigation 2.9.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.4)
- Navigation 3 libraries `org.jetbrains.androidx.navigation:navigation3-*:1.0.0-alpha04`. Based on [Jetpack Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3#1.0.0-beta01)
- Navigation Event library `org.jetbrains.androidx.navigationevent:navigationevent-compose:1.0.0-beta01`. Based on [Jetpack Navigation Event 1.0.0-beta01](https://developer.android.com/jetpack/androidx/releases/navigationevent#1.0.0-beta01)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate*:1.4.0-beta01`. Based on [Jetpack Savedstate 1.4.0-rc01](https://developer.android.com/jetpack/androidx/releases/savedstate#1.4.0-rc01)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.5.0-rc01`. Based on [Jetpack WindowManager 1.5.0](https://developer.android.com/jetpack/androidx/releases/window#1.5.0)

---

# 1.9.2 (October 2025)

_Changes since 1.9.1_

## Fixes

### Multiple Platforms

- Prevent possible symbol duplicates in `savedstate-compose` due to redirects to Google's `runtime-saveable` that supports all KMP platforms [#2503](https://github.com/JetBrains/compose-multiplatform-core/pull/2503)

### Web

- Removed an outline on a `Canvas` element when it's focused [#2450](https://github.com/JetBrains/compose-multiplatform-core/pull/2450)
- Fix focus with Tab behaviour in Text Fields [#2452](https://github.com/JetBrains/compose-multiplatform-core/pull/2452)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.2`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.4)
  - [UI 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.4)
  - [Foundation 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.4)
  - [Material 1.9.4](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.4)
  - [Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0`. Based on [Jetpack Compose Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0`. Based on [Jetpack Compose Material3 Adaptive 1.2.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.5`. Based on [Jetpack Lifecycle 2.9.4](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.4)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.1`. Based on [Jetpack Navigation 2.9.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.4)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate*:1.3.5`. Based on [Jetpack Savedstate 1.3.3](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.3)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.10.0-alpha03 (October 2025)

_Changes since 1.10.0-alpha02_

## Migration Notes

### Multiple Platforms

- _(prerelease fix)_ Disabled (by default) the new text context menus until they are fully supported. You can enable them by setting `ComposeFoundationFlags.isNewContextMenuEnabled = true` [#2466](https://github.com/JetBrains/compose-multiplatform-core/pull/2466)
- Deprecate `org.jetbrains.compose.ui.tooling.preview.Preview` annotation in favor of `androidx.compose.ui.tooling.preview.Preview` to reduce variety of Preview annotations [#5453](https://github.com/JetBrains/compose-multiplatform/pull/5453)

### iOS

- Remove experimental annotation from `LocalUIView` [#2478](https://github.com/JetBrains/compose-multiplatform-core/pull/2478)

### Desktop

- Deprecate `androidx.compose.desktop.ui.tooling.preview.Preview` annotation in favor of `androidx.compose.ui.tooling.preview.Preview` to reduce variety of Preview annotations [#2474](https://github.com/JetBrains/compose-multiplatform-core/pull/2474)

## Features

### Lifecycle

- Publish multiplatform `lifecycle-viewmodel-navigation3` library [#2476](https://github.com/JetBrains/compose-multiplatform-core/pull/2476)

## Fixes

### Multiple Platforms

- _(prerelease fix)_ Provide `LocalWindowInfo.current.containerDpSize` value [#2493](https://github.com/JetBrains/compose-multiplatform-core/pull/2493)

### iOS

- Fix crash when `parallelRendering` is enabled [#2448](https://github.com/JetBrains/compose-multiplatform-core/pull/2448)
- Fix text context menu interaction on `Popup`s and `Dialog`s [#2434](https://github.com/JetBrains/compose-multiplatform-core/pull/2434)
- Fix crash when dragging two Scrollable components with two fingers [#2456](https://github.com/JetBrains/compose-multiplatform-core/pull/2456)
- Fix the crash that occurs when the magnifier appears in text fields that are zero size [#2460](https://github.com/JetBrains/compose-multiplatform-core/pull/2460)
- Fix Lifecycle status updates for multi-window applications using `UIScene` notifications [#2472](https://github.com/JetBrains/compose-multiplatform-core/pull/2472)
- Support updating of the `WindowInfo.isWindowFocused` property when the window's state changes [#2482](https://github.com/JetBrains/compose-multiplatform-core/pull/2482)
- Fix the text removal issue when inputting text using dictation [#2488](https://github.com/JetBrains/compose-multiplatform-core/pull/2488)

### Desktop

- ComposePanel now re-dispatches unconsumed mouse wheel events, allowing scrollable components beneath to be scrolled. To disable this, set the system property `"compose.swing.redispatchMouseWheelEvents"` to `"false"` [#2425](https://github.com/JetBrains/compose-multiplatform-core/pull/2425)
- Fix background flashing when showing a window/dialog [#2471](https://github.com/JetBrains/compose-multiplatform-core/pull/2471)

### Web

- Removed an outline on a `Canvas` element when it's focused [#2450](https://github.com/JetBrains/compose-multiplatform-core/pull/2450)
- Fix focus with Tab behaviour in Text Fields [#2452](https://github.com/JetBrains/compose-multiplatform-core/pull/2452)
- Mobile. Composite input. When a syllable block is created, a new block is added instead of replacing the old one [#2454](https://github.com/JetBrains/compose-multiplatform-core/pull/2454)

### Resources

- Fixed an issue where resources were not copied when packaging the macOS native target, causing the application to crash when it attempted to read those resources [#5431](https://github.com/JetBrains/compose-multiplatform/pull/5431)
- Fixed resources generated code to be reproducible between different machines [#5446](https://github.com/JetBrains/compose-multiplatform/pull/5446)
- Fix a crash when calling `getString` and the Locale has no region specified [#5447](https://github.com/JetBrains/compose-multiplatform/pull/5447)
- Use the non-empty font as the default when awaiting a asynchronous request completion on the web [#5456](https://github.com/JetBrains/compose-multiplatform/pull/5456)

### Navigation

- Ignore back gestures in navigation in case of open dialog for non-android targets [#2439](https://github.com/JetBrains/compose-multiplatform-core/pull/2439)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.10.0-alpha03`. Based on Jetpack Compose libraries:
  - [Runtime 1.10.0-alpha05](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.10.0-alpha05)
  - [UI 1.10.0-alpha05](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.10.0-alpha05)
  - [Foundation 1.10.0-alpha05](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.10.0-alpha05)
  - [Material 1.10.0-alpha05](https://developer.android.com/jetpack/androidx/releases/compose-material#1.10.0-alpha05)
  - [Material3 1.5.0-alpha06](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0-alpha06)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.10.0-alpha03`. Based on [Jetpack Compose Material3 1.5.0-alpha06](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0-alpha06)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-beta01`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-rc01)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.10.0-alpha03`. Based on [Jetpack Lifecycle 2.10.0-alpha05](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.10.0-alpha05)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.1`. Based on [Jetpack Navigation 2.9.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.4)
- Navigation 3 libraries `org.jetbrains.androidx.navigation:navigation3-*:1.0.0-alpha03`. Based on [Jetpack Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3#1.0.0-alpha11)
- Navigation Event library `org.jetbrains.androidx.navigationevent:navigationevent-compose:1.0.0-alpha02`. Based on [Jetpack Navigation Event 1.0.0-beta01](https://developer.android.com/jetpack/androidx/releases/navigationevent#1.0.0-beta01)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.4.0-alpha03`. Based on [Jetpack Savedstate 1.4.0-beta01](https://developer.android.com/jetpack/androidx/releases/savedstate#1.4.0-beta01)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.5.0-beta01`. Based on [Jetpack WindowManager 1.5.0](https://developer.android.com/jetpack/androidx/releases/window#1.5.0)

---

# 1.9.1 (October 2025)

_Changes since 1.9.0_

## Migration Notes

### Multiple Platforms

- `compose.material3` alias from Gradle plugin now points to `org.jetbrains.compose.material3:material3:1.9.0` (based on Jetpack Compose Material3 `1.4.0`), it was `org.jetbrains.compose.material3:material3:1.8.2` (based on Jetpack Compose Material3 `1.3.2`) in Compose Multiplatform `1.9.0`. If you want to stay on the same minor version, replace the alias by an explicit dependency: [#5441](https://github.com/JetBrains/compose-multiplatform/pull/5441)

  ```
  // was
  implementation(compose.material3)
  
  // replace by
  implementation("org.jetbrains.compose.material3:material3:1.8.2")
  ```

## Fixes

### iOS

- Add ability to reach internal accessibility elements inside accessibility nodes [#2416](https://github.com/JetBrains/compose-multiplatform-core/pull/2416)
- Fix input session restarts triggered by `PlatformImeOptionsImpl` not being `@Immutable` [#2413](https://github.com/JetBrains/compose-multiplatform-core/pull/2413)
- Fix crash when removing characters after string replacement [#2361](https://github.com/JetBrains/compose-multiplatform-core/pull/2361)
- Fix context menu appearance for text fields with transformation [#2377](https://github.com/JetBrains/compose-multiplatform-core/pull/2377)

### Desktop

- `ComposePanel` can now re-dispatch unconsumed mouse wheel events, allowing scrollable components beneath to be scrolled. To enable this behavior, set the system property `"compose.swing.redispatchMouseWheelEvents"` to `"true"` [#2438](https://github.com/JetBrains/compose-multiplatform-core/pull/2438)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.1`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.3](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.3)
  - [UI 1.9.3](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.3)
  - [Foundation 1.9.3](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.3)
  - [Material 1.9.3](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.3)
  - [Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0`. Based on [Jetpack Compose Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha05`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-alpha10](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha10)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.5`. Based on [Jetpack Lifecycle 2.9.4](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.4)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.1`. Based on [Jetpack Navigation 2.9.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.4)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.5`. Based on [Jetpack Savedstate 1.3.3](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.3)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.10.0-alpha02 (October 2025)

_Changes since 1.10.0-alpha01_

## Highlights

### Multiple Platforms

- `androidx.compose.ui.tooling.preview.Preview` annotation is now available from `commonMain` source set [#2424](https://github.com/JetBrains/compose-multiplatform-core/pull/2424)

### Navigation

- Provide required `NavigationEventDispatcherOwner` for a correct Navigation3 support [#2382](https://github.com/JetBrains/compose-multiplatform-core/pull/2382)

## Features

### Multiple Platforms

- Provide `LocalLifecycleOwner` inside `runComposeUiTest` by default [#2400](https://github.com/JetBrains/compose-multiplatform-core/pull/2400)

### iOS

- Add API to configure `UITextInputTraits.writingToolsBehavior` with `PlatformImeOptions` [#2435](https://github.com/JetBrains/compose-multiplatform-core/pull/2435)

### Desktop

- Experimental `-Dskiko.gpu.resourceCacheLimit` VM property is introduced to limit GPU resource cache used for one window (default is 256M) [#2422](https://github.com/JetBrains/compose-multiplatform-core/pull/2422)

### Resources

- Add `setResourceReaderAndroidContext` to configure Android context in cases when a provider initialization is not available [#5434](https://github.com/JetBrains/compose-multiplatform/pull/5434)

### Navigation

- Basic support of the navigation3 library [#2436](https://github.com/JetBrains/compose-multiplatform-core/pull/2436)

## Fixes

### Multiple Platforms

- _(prerelease fix)_ Fix "Cinterop fails with an error when Compilation works fine" [#2386](https://github.com/JetBrains/compose-multiplatform-core/pull/2386)
- _(prerelease fix)_ Fix "warning: KLIB resolver: Could not find" [#2386](https://github.com/JetBrains/compose-multiplatform-core/pull/2386)

### iOS

- Clear `TextField` focus when another view becomes first responder [#2337](https://github.com/JetBrains/compose-multiplatform-core/pull/2337)
- Fix context menu appearance for text fields with transformation [#2377](https://github.com/JetBrains/compose-multiplatform-core/pull/2377)
- Fix input session restarts triggered by `PlatformImeOptionsImpl` not being `@Immutable` [#2413](https://github.com/JetBrains/compose-multiplatform-core/pull/2413)
- Add ability to reach internal accessibility elements inside accessibility nodes [#2416](https://github.com/JetBrains/compose-multiplatform-core/pull/2416)

### Desktop

- Fixed `ComposePanel` not initially focusing the first focusable node, when running in JetBrains Runtime JVM [#2384](https://github.com/JetBrains/compose-multiplatform-core/pull/2384)
- Fixed background flashing when a window or dialog are closed [#2403](https://github.com/JetBrains/compose-multiplatform-core/pull/2403)
- Fix "ComposePanel doesn't use hardware acceleration with `RenderSettings.SwingGraphics` on Linux" [#2422](https://github.com/JetBrains/compose-multiplatform-core/pull/2422)

### Resources

- Fix resource access in a Robolectric test environment [#5433](https://github.com/JetBrains/compose-multiplatform/pull/5433)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.10.0-alpha02`. Based on Jetpack Compose libraries:
  - [Runtime 1.10.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.10.0-alpha04)
  - [UI 1.10.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.10.0-alpha04)
  - [Foundation 1.10.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.10.0-alpha04)
  - [Material 1.10.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-material#1.10.0-alpha04)
  - [Material3 1.5.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0-alpha04)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.10.0-alpha02`. Based on [Jetpack Compose Material3 1.5.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0-alpha04)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha07`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-beta03)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.10.0-alpha02`. Based on [Jetpack Lifecycle 2.10.0-alpha04](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.10.0-alpha04)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0`. Based on [Jetpack Navigation 2.9.1](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.1)
- Navigation Event library `org.jetbrains.androidx.navigationevent:navigationevent-compose:1.0.0-alpha01`. Based on [Jetpack Navigation Event 1.0.0-alpha09](https://developer.android.com/jetpack/androidx/releases/navigationevent#1.0.0-alpha09)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.4.0-alpha02`. Based on [Jetpack Savedstate 1.4.0-alpha03](https://developer.android.com/jetpack/androidx/releases/savedstate#1.4.0-alpha03)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.5.0-alpha02`. Based on [Jetpack WindowManager 1.5.0](https://developer.android.com/jetpack/androidx/releases/window#1.5.0)

---

# 1.9.0 (September 2025)

_Changes since 1.8.2_

See also [What's new](https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose-190.html).

## Highlights

### Web

- Added a `WebElementView` Composable function for embedding the absolutely positioned HTML content in Compose for web target. It's supported only when using `ComposeViewport` entry point and it's not supported with `CanvasBasedWindow`, which is deprecated now [#2145](https://github.com/JetBrains/compose-multiplatform-core/pull/2145)
- Text context menu is supported on web platforms for both modes: mobile and desktop [#2207](https://github.com/JetBrains/compose-multiplatform-core/pull/2207)
- Introduce the basic support of accessibility in the web target [#2188](https://github.com/JetBrains/compose-multiplatform-core/pull/2188)

## Migration Notes

### Multiple Platforms

- Material3 versioning is decoupled for the Compose Multiplatform 1.9.* release due the upstream Jetpack Compose Material3 1.4 has not been released as stable yet [#5360](https://github.com/JetBrains/compose-multiplatform/pull/5360)
- `compose.material3` now points to the latest stable Material3 version, 1.8.2. If the latest Material3 features are needed, please include it this way: [#5360](https://github.com/JetBrains/compose-multiplatform/pull/5360)
  ```
  implementation("org.jetbrains.compose.material3:material3:1.9.0-beta06")
  ```
- [If you use `org.jetbrains.compose.material3:material3:1.9.0-beta06`] `kotlinx-datetime` is updated to `0.7.1`. If you also use it in your projects, please update it to this version to ensure compatibility [#2276](https://github.com/JetBrains/compose-multiplatform-core/pull/2276)

### iOS

- Remove experimental attribute from `UIKitInteropInteractionMode` [#2215](https://github.com/JetBrains/compose-multiplatform-core/pull/2215)

### Desktop

- `Window` and `DialogWindow` overloads that create a `ComposeWindow`/`ComposeDialog` have been renamed to `SwingWindow`/`SwingDialog` and the old versions have been deprecated [#2141](https://github.com/JetBrains/compose-multiplatform-core/pull/2141)
- When the window is iconified, converting to/from screen coordinates (with e.g.`LayoutCoordinates.positionOnScreen()`) will return `Offset.Unspecified` [#2163](https://github.com/JetBrains/compose-multiplatform-core/pull/2163)
- Deprecated experimental `Modifier.mouseClickable` is removed. See https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-desktop-mouse-events.html for alternatives [#2194](https://github.com/JetBrains/compose-multiplatform-core/pull/2194)
- Kotlin 2.1 is required for all platforms including JVM (it was required only for native and web before) [#2276](https://github.com/JetBrains/compose-multiplatform-core/pull/2276)

### Web

- `fun ComposeViewport` with `viewportContainerId: String?` parameter now can be called from a web-common `fun main` (k/js and k/wasm) [#2226](https://github.com/JetBrains/compose-multiplatform-core/pull/2226)
- `CanvasBasedWindow` is deprecated, use `ComposeViewport` instead. Unlike `CanvasBasedWindow`, which expect as an input param the id to the `HTMLCanvasElement` that will be used for rendering, `ComposeViewport` one passes `parentContainer` (and corresponding HTML Canvas element will be created automatically). By default such container is `document.body` [#2280](https://github.com/JetBrains/compose-multiplatform-core/pull/2280)
- Setting `org.jetbrains.compose.experimental.jscanvas.enabled=true` is not required anymore when having a kotlin/js target [#5340](https://github.com/JetBrains/compose-multiplatform/pull/5340)

### Gradle Plugin

- The Compose Gradle plugin requires the Kotlin Gradle plugin version 2.+ now. Old `org.jetbrains.compose.compiler` is not supported anymore and the API to configure it was removed [#5283](https://github.com/JetBrains/compose-multiplatform/pull/5283)

## Features

### Multiple Platforms

- Adopted a change in `ComposeUiTest` API. The  `block` in `runComposeUiTest` is `suspend` now. It allows to call `awaitIdle` and other suspend functions. It ensures a correct execution of a test on all platforms. See the web specifics in `kotlinx.coroutines.test.runTest` [documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html) [#2066](https://github.com/JetBrains/compose-multiplatform-core/pull/2066)
- Support customizable shadows [#2183](https://github.com/JetBrains/compose-multiplatform-core/pull/2183)
- Extended the `@Preview` (`org.jetbrains.compose.ui.tooling.preview`) annotation with the following parameters: name, group, widthDp, heightDp, locale, showBackground, backgroundColor. IDE (IJ or AS) will pick up these parameters in the same way it works for `androidx` Preview annotations [#5339](https://github.com/JetBrains/compose-multiplatform/pull/5339)

### iOS

- Add support for native IME configuration with `PlatformImeOptions` [#2108](https://github.com/JetBrains/compose-multiplatform-core/pull/2108)
- Add support for `keepScreenOn` modifier [#2180](https://github.com/JetBrains/compose-multiplatform-core/pull/2180)
- Support new context menu API with default menu [#2214](https://github.com/JetBrains/compose-multiplatform-core/pull/2214)
- Add support for frame rate voting [#2205](https://github.com/JetBrains/compose-multiplatform-core/pull/2205)
- Support scroll commands with Voice Control [#2234](https://github.com/JetBrains/compose-multiplatform-core/pull/2234)
- Implement accessibility scroll to focused interop views [#2228](https://github.com/JetBrains/compose-multiplatform-core/pull/2228)

### Desktop

- Add accessibility role for `Switch`, reporting it as a checkbox [#2136](https://github.com/JetBrains/compose-multiplatform-core/pull/2136)
- Added `SwingFrame` and `SwingDialog` composables that allow configuring the window/dialog before it is shown [#2139](https://github.com/JetBrains/compose-multiplatform-core/pull/2139)
- Full `AnnotatedString` is available as a data flavor in `ClipEntry`, instead of only its text [#2092](https://github.com/JetBrains/compose-multiplatform-core/pull/2092)
- Add `RenderSettings.SwingGraphics` option for `ComposePanel.renderSettings` argument [#2071](https://github.com/JetBrains/compose-multiplatform-core/pull/2071)
- Basic support for new context menu API [#2196](https://github.com/JetBrains/compose-multiplatform-core/pull/2196)
- Added experimental support for save and restore compose state. `ComposePanel`, `ComposeWindow` and `ComposeDialog` now has `savedState` constructor parameter to restore previous state and `saveState` function to save the current state for later use [#2225](https://github.com/JetBrains/compose-multiplatform-core/pull/2225)

### Web

- Support the new context menu API in web targets in the desktop mode [#2224](https://github.com/JetBrains/compose-multiplatform-core/pull/2224)
- Support of the new context menu toolbar in web mobile targets [#2251](https://github.com/JetBrains/compose-multiplatform-core/pull/2251)
- [js] there's no need to manually add skiko.js to the html page any more [#2264](https://github.com/JetBrains/compose-multiplatform-core/pull/2264)
- Introduce `composeCompatibilityBrowserDistribution`  task. This task combines two prod distributions - for js and for wasm in such way so that if modern required features are not supported by the consumer browser, application switch to js mode [#5375](https://github.com/JetBrains/compose-multiplatform/pull/5375)

### Resources

- Added `JvmResourceReader` API and made `LocalResourceReader` public to allow providing a custom classloader for desktop target [#5334](https://github.com/JetBrains/compose-multiplatform/pull/5334)

### Navigation

- A new API was added to bind the browser navigation state with the `NavController` - `suspend fun NavController.bindToBrowserNavigation`. And the existing function `suspend fun Window.bindToNavigation` is deprecated now [#2189](https://github.com/JetBrains/compose-multiplatform-core/pull/2189)

## Fixes

### Multiple Platforms

- Fix extra draw invalidations during scrolling (1.8 regression) [#2212](https://github.com/JetBrains/compose-multiplatform-core/pull/2212)
- Fix text ellipsis if there's not enough vertical space to fit all lines [#2246](https://github.com/JetBrains/compose-multiplatform-core/pull/2246)
- Fix "IrLinkageError: Function can not be called: No function found for symbol" [#2293](https://github.com/JetBrains/compose-multiplatform-core/pull/2293)
- `ExperimentalMaterial3ExpressiveApi` annotation removed from no-longer-experimental API [#2298](https://github.com/JetBrains/compose-multiplatform-core/pull/2298)

### iOS

- Fix issue where keyboard would appear after second tap when text input session was intercepted [#2049](https://github.com/JetBrains/compose-multiplatform-core/pull/2049)
- Change the accessibility selection to the element that has most recently been focused on [#2217](https://github.com/JetBrains/compose-multiplatform-core/pull/2217)
- Fix Full Keyboard Access on iOS 17 and lower [#2216](https://github.com/JetBrains/compose-multiplatform-core/pull/2216)
- Fixes the appearance of the keyboard when a pop-up or dialog on the background is dismissed [#2240](https://github.com/JetBrains/compose-multiplatform-core/pull/2240)
- Do not flatten accessibility tree inside accessibility elements [#2243](https://github.com/JetBrains/compose-multiplatform-core/pull/2243)

### Desktop

- [macOS] Fix the background flashing when closing a window/dialog and an animation is running [#2058](https://github.com/JetBrains/compose-multiplatform-core/pull/2058)
- [macOS; JBR] Fixed the current composition in a text field being duplicated into another text field when switching focus to it [#2026](https://github.com/JetBrains/compose-multiplatform-core/pull/2026)
- [macOS] Fixed strange glyph being displayed in a text field if window becomes unfocused, then focused again while there's an active composition in the text field (after pressing e.g. backspace) [#2026](https://github.com/JetBrains/compose-multiplatform-core/pull/2026)
- [macOS] Fix showing the input method toolbar before any text field becomes focused (on JBR only; other runtimes continue to be buggy) [#2047](https://github.com/JetBrains/compose-multiplatform-core/pull/2047)
- Improved performance for `ComposePanel` with `System.setProperty("compose.swing.render.on.graphics", "true")` [#2097](https://github.com/JetBrains/compose-multiplatform-core/pull/2097)
- Fix the positioning of the IME popup being too far away from the text, on screens with density greater than 1 [#2118](https://github.com/JetBrains/compose-multiplatform-core/pull/2118)
- Fixed the position of the IME popup, which was below the previous, rather than the current, cursor position [#2122](https://github.com/JetBrains/compose-multiplatform-core/pull/2122)
- [Accessibility, macOS] Fixed VoiceOver to announce a Slider's value on every change [#2152](https://github.com/JetBrains/compose-multiplatform-core/pull/2152)
- Return `Offset.Unspecified` instead of throwing an exception in `LayoutCoordinates.localToScreen` and `LayoutCoordinates.screenToLocal` [#2160](https://github.com/JetBrains/compose-multiplatform-core/pull/2160)
- When the window moves or becomes iconified/de-iconified, all instances of `Modifier.onGloballyPositioned` will be called [#2163](https://github.com/JetBrains/compose-multiplatform-core/pull/2163)
- Implemented a context menu for `BasicTextField(TextFieldState)` [#2168](https://github.com/JetBrains/compose-multiplatform-core/pull/2168)
- `SemanticsProperties.Text` and `SemanticsProperties.ContentDescription` values will now be correctly concatenated when `Modifier.semantics(mergeDescendants = true)` is used [#2202](https://github.com/JetBrains/compose-multiplatform-core/pull/2202)
- Fixed the case when the IME's first event to a text field is to commit a composition [#2210](https://github.com/JetBrains/compose-multiplatform-core/pull/2210)
- Elements marked with Modifier.semantics { hideFromAccessibility() } should now be correctly hidden from a11y [#2204](https://github.com/JetBrains/compose-multiplatform-core/pull/2204)
- [macOS] Fix composite (e.g. Chinese) input after pressing backspace [#2250](https://github.com/JetBrains/compose-multiplatform-core/pull/2250)
- [TextField] Fixed duplication of the composed characters when moving the caret by clicking during a composition [#2255](https://github.com/JetBrains/compose-multiplatform-core/pull/2255)
- Correctly remove `SwingPanel` children of `ComposePanel`, when the compose panel is itself removed from the hierarchy [#2277](https://github.com/JetBrains/compose-multiplatform-core/pull/2277)
- Fix `runRelease` task when navigation and `obfuscate.set(true)` are used [#5384](https://github.com/JetBrains/compose-multiplatform/pull/5384)

### Web

- Fixed an unexpected back gesture after a horizontal scroll [#2186](https://github.com/JetBrains/compose-multiplatform-core/pull/2186)
- Mitigate a typing delay regression in Safari browsers [#2195](https://github.com/JetBrains/compose-multiplatform-core/pull/2195)
- Web Lifecycle triggers `START`/`STOP` events on the `visibilitychange` callback now [#2219](https://github.com/JetBrains/compose-multiplatform-core/pull/2219)
- Lifecycle fix on iOS Safari: now touch events trigger `ON_RESUME` because Safari ignores interactions and doesn't request the focus [#2219](https://github.com/JetBrains/compose-multiplatform-core/pull/2219)
- Fix software keyboard behaviour for Compose Text Fields in iOS Safari [#2260](https://github.com/JetBrains/compose-multiplatform-core/pull/2260)
- Fixed the bugs with composite text input [#2256](https://github.com/JetBrains/compose-multiplatform-core/pull/2256)
- Fixed the issue with software keyboard when it was shown repeatedly in Chrome mobile [#2279](https://github.com/JetBrains/compose-multiplatform-core/pull/2279)
- Fix the issue where deleting a word in the middle of a sentence also affects the word next to it [#2372](https://github.com/JetBrains/compose-multiplatform-core/pull/2372)

### Gradle Plugin

- Don't fail gradle sync if `TargetFormat.AppImage` is specified in `targetFormats` on macOS [#5332](https://github.com/JetBrains/compose-multiplatform/pull/5332)
- Fix codesigning on macOS when the developer id contains non-ASCII characters. Note that this requires JDK 21 or later [#5358](https://github.com/JetBrains/compose-multiplatform/pull/5358)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.0`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.0)
  - [UI 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.0)
  - [Foundation 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.0)
  - [Material 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.0)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0-beta06`. Based on [Jetpack Compose Material3 1.4.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta03)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha06`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-alpha11](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha11)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.4`. Based on [Jetpack Lifecycle 2.9.2](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.2)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0`. Based on [Jetpack Navigation 2.9.1](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.1)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.4`. Based on [Jetpack Savedstate 1.3.1](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.1)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.9.0-rc02 (September 2025)

_Changes since 1.9.0-rc01_

## Fixes

### Multiple Platforms

- _(prerelease fix)_ Fix "Cinterop fails with an error when Compilation works fine" [#2386](https://github.com/JetBrains/compose-multiplatform-core/pull/2386)
- _(prerelease fix)_ Fix "warning: KLIB resolver: Could not find" [#2386](https://github.com/JetBrains/compose-multiplatform-core/pull/2386)

### Web

- Fix the issue where deleting a word in the middle of a sentence also affects the word next to it [#2372](https://github.com/JetBrains/compose-multiplatform-core/pull/2372)

### Gradle Plugin

- _(prerelease fix)_ Fix `composeCompatibilityBrowserDistribution` task lazy configuration [#5398](https://github.com/JetBrains/compose-multiplatform/pull/5398)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.0-rc02`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.0)
  - [UI 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.0)
  - [Foundation 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.0)
  - [Material 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.0)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0-beta05`. Based on [Jetpack Compose Material3 1.4.0-beta03](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta03)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha06`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-alpha11](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha11)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.4-rc01`. Based on [Jetpack Lifecycle 2.9.2](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.2)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-rc02`. Based on [Jetpack Navigation 2.9.1](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.1)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.4-rc01`. Based on [Jetpack Savedstate 1.3.1](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.1)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0-rc02`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.10.0-alpha01 (September 2025)

_Changes since 1.9.0-rc01_

## Highlights

### Multiple Platforms

- `widgets-gallery` sample has been removed in favor of the [interactive API reference](https://kotlinlang.org/api/compose-multiplatform/stories/material3/) [#5365](https://github.com/JetBrains/compose-multiplatform/pull/5365)

## Breaking Changes

### iOS

- Align `@Composable` attribute in `WindowInsets.Companion.captionBar` to other platforms [#2258](https://github.com/JetBrains/compose-multiplatform-core/pull/2258)

## Migration Notes

### Multiple Platforms

- The pre‑1.7 workaround in the common `PopupProperties`/`DialogProperties` constructors (annotated `Deprecated(HIDDEN)` since 1.7) has been removed. This change may formally affect binary compatibility. Although we are not aware of concrete cases, if your project depends on third‑party libraries that reference this constructor, please update those dependencies to versions compatible with this release [#2303](https://github.com/JetBrains/compose-multiplatform-core/pull/2303)
- Kotlin 2.2 is required for native and web platforms [#2357](https://github.com/JetBrains/compose-multiplatform-core/pull/2357)

### Desktop

- Removed IntelliJ plugin sample, please refer to [Jewel](https://github.com/JetBrains/intellij-community/tree/master/platform/jewel) documentation instead [#5368](https://github.com/JetBrains/compose-multiplatform/pull/5368)

## Features

### Multiple Platforms

- Update skia to m138 (see [release notes](https://skia.googlesource.com/skia/+/refs/heads/chrome/m138/RELEASE_NOTES.md)) [#2304](https://github.com/JetBrains/compose-multiplatform-core/pull/2304)
- The new context menu is enabled by default on all platforms now. It still might be disabled via `ComposeFoundationFlags.isNewContextMenuEnabled` flag in case of issues [#2341](https://github.com/JetBrains/compose-multiplatform-core/pull/2341)
- Provide public API for `@ResourceContentHash` annotation generation [#5402](https://github.com/JetBrains/compose-multiplatform/pull/5402)

### iOS

- Add support for `WindowInsetsRulers` [#2258](https://github.com/JetBrains/compose-multiplatform-core/pull/2258)
- Support automatic scrolling for Full Keyboard Access [#2222](https://github.com/JetBrains/compose-multiplatform-core/pull/2222)
- Support custom menu items for text context menu [#2324](https://github.com/JetBrains/compose-multiplatform-core/pull/2324)
- Add API to configure `UIResponder.inputView` with `PlatformImeOptions` [#2350](https://github.com/JetBrains/compose-multiplatform-core/pull/2350)
- Add API to configure `UIResponder.inputAccessoryView` with `PlatformImeOptions` [#2350](https://github.com/JetBrains/compose-multiplatform-core/pull/2350)

### Desktop

- Added `modalityType` parameter to `DialogWindow()` [#2300](https://github.com/JetBrains/compose-multiplatform-core/pull/2300)
- The Compose entry points on the desktop (`ComposeWindow`, `ComposePanel` and `ImageComposeScene`) now expose `val semanticsOwners: Collection<SemanticsOwner>` [#2358](https://github.com/JetBrains/compose-multiplatform-core/pull/2358)

## Fixes

### Multiple Platforms

- Fix application of `baselineShift` parameter in text layout [#2304](https://github.com/JetBrains/compose-multiplatform-core/pull/2304)
- Fix setting `lineHeight` to `0` in text layout [#2304](https://github.com/JetBrains/compose-multiplatform-core/pull/2304)

### iOS

- Fix incorrect behavior of `WindowInsets.displayCutout` in different interface orientations [#2301](https://github.com/JetBrains/compose-multiplatform-core/pull/2301)
- Fix incorrect behavior of `WindowInsets.displayCutout` on iPad [#2301](https://github.com/JetBrains/compose-multiplatform-core/pull/2301)
- Fix the ability to use UIKitViewController inside `Popup`s and `Dialog`s [#2270](https://github.com/JetBrains/compose-multiplatform-core/pull/2270)
- Fix Accessibility announcing the old state of component [#2327](https://github.com/JetBrains/compose-multiplatform-core/pull/2327)
- Align the semantics of TextFields with iOS text inputs [#2331](https://github.com/JetBrains/compose-multiplatform-core/pull/2331)
- Fix crash when removing characters after string replacement [#2361](https://github.com/JetBrains/compose-multiplatform-core/pull/2361)

### Desktop

- Fix non-focusable popup with `compose.layers.type=WINDOW` stealing focus [#2285](https://github.com/JetBrains/compose-multiplatform-core/pull/2285)
- Change `ComposePanel.getPreferredSize` to return 0x0 instead of `null` [#2283](https://github.com/JetBrains/compose-multiplatform-core/pull/2283)
- Request initial focus for focusable popups when used from `ComposePanel` in some cases [#2289](https://github.com/JetBrains/compose-multiplatform-core/pull/2289)
- In experimental `compose.layers.type` modes, fix `Popup`/`Dialog` container size calculation that prevents mouse interactions on base compose scene [#2304](https://github.com/JetBrains/compose-multiplatform-core/pull/2304)
- Fixed the sizing of unfocusable layers when `compose.layers.type=COMPONENT` is used [#2305](https://github.com/JetBrains/compose-multiplatform-core/pull/2305)
- `SwingPanel` no longer requires to be manually sized to a fixed value; it will size according to its content's min/pref/max sizes [#2310](https://github.com/JetBrains/compose-multiplatform-core/pull/2310)
- Made disabled new context menu items actually disabled, including the right semantics [#2347](https://github.com/JetBrains/compose-multiplatform-core/pull/2347)

### Web

- Fix the issue where deleting a word in the middle of a sentence also affects the word next to it [#2372](https://github.com/JetBrains/compose-multiplatform-core/pull/2372)

### Gradle Plugin

- Support `AGP 9.0.0` [#5391](https://github.com/JetBrains/compose-multiplatform/pull/5391)
- _(prerelease fix)_ Fix composeCompatibilityBrowserDistribution task lazy configuration [#5398](https://github.com/JetBrains/compose-multiplatform/pull/5398)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.10.0-alpha01`. Based on Jetpack Compose libraries:
  - [Runtime 1.10.0-alpha02](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.10.0-alpha02)
  - [UI 1.10.0-alpha02](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.10.0-alpha02)
  - [Foundation 1.10.0-alpha02](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.10.0-alpha02)
  - [Material 1.10.0-alpha02](https://developer.android.com/jetpack/androidx/releases/compose-material#1.10.0-alpha02)
  - [Material3 1.5.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0-alpha03)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.10.0-alpha01`. Based on [Jetpack Compose Material3 1.5.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0-alpha03)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha06`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-alpha11](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha11)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.10.0-alpha01`. Based on [Jetpack Lifecycle 2.10.0-alpha03](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.10.0-alpha03)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-rc01`. Based on [Jetpack Navigation 2.9.1](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.1)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.4.0-alpha01`. Based on [Jetpack Savedstate 1.4.0-alpha03](https://developer.android.com/jetpack/androidx/releases/savedstate#1.4.0-alpha03)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.5.0-alpha01`. Based on [Jetpack WindowManager 1.5.0-beta02](https://developer.android.com/jetpack/androidx/releases/window#1.5.0-beta02)

---

# 1.9.0-rc01 (August 2025)

_Changes since 1.9.0-beta03_

## Fixes

### Multiple Platforms

- _(prerelease fix)_ Fix incorrect light source position for dynamic shadows in some cases [#2328](https://github.com/JetBrains/compose-multiplatform-core/pull/2328)

### Desktop

- _(prerelease fix)_ Fixed focus indication being shown in touch input mode [#2342](https://github.com/JetBrains/compose-multiplatform-core/pull/2342)
- Fix `runRelease` task when navigation and `obfuscate.set(true)` are used [#5384](https://github.com/JetBrains/compose-multiplatform/pull/5384)

### Lifecycle

- Fix dependency to Compose in `lifecycle-viewmodel-compose` module: `2.9.2` incorrectly refer Compose Multiplatform `1.9.0-beta03`. Now it reverted back to `1.8.2` [#2318](https://github.com/JetBrains/compose-multiplatform-core/pull/2318)

### SavedState

- Fix dependency to Compose in `savedstate-compose` module: `1.3.2` incorrectly refer Compose Multiplatform `1.9.0-beta03`. Now it reverted back to `1.8.2` [#2321](https://github.com/JetBrains/compose-multiplatform-core/pull/2321)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.0-rc01`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.0)
  - [UI 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.0)
  - [Foundation 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.0)
  - [Material 1.9.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.0)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0-beta04`. Based on [Jetpack Compose Material3 1.4.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta02)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha05`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-alpha10](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha10)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.3`. Based on [Jetpack Lifecycle 2.9.2](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.2)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-rc01`. Based on [Jetpack Navigation 2.9.1](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.1)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.3`. Based on [Jetpack Savedstate 1.3.1](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.1)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0-rc01`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.9.0-beta03 (August 2025)

_Changes since 1.9.0-beta01_

## Breaking Changes

### Multiple Platforms

- _(prerelease fix)_ All public APIs tagged with `ExperimentalMaterial3ExpressiveApi` or `ExperimentalMaterial3ComponentOverrideApi` have been removed, see [Google Jetpack changelog](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta01). Please use the previous Material3 alpha version explicitly to continue enjoying these features: [#2278](https://github.com/JetBrains/compose-multiplatform-core/pull/2278)

  ```
  implementation("org.jetbrains.compose.material3:material3:1.9.0-alpha04")
  ```

## Migration Notes

### Multiple Platforms

- `kotlinx-datetime` is updated to `0.7.1`. If you also use it in your projects, please update it to this version to ensure compatibility [#2276](https://github.com/JetBrains/compose-multiplatform-core/pull/2276)

### Desktop

- Kotlin 2.1 is required for all platforms including JVM (it was required only for native and web before) [#2276](https://github.com/JetBrains/compose-multiplatform-core/pull/2276)

### Web

- `CanvasBasedWindow` is deprecated, use `ComposeViewport` instead. Unlike `CanvasBasedWindow`, which expect as an input param the id to the `HTMLCanvasElement` that will be used for rendering, `ComposeViewport` one passes `parentContainer` (and corresponding HTML Canvas element will be created automatically). By default such container is `document.body` [#2280](https://github.com/JetBrains/compose-multiplatform-core/pull/2280)

## Features

### Web

- Support of the new context menu toolbar in web mobile targets [#2251](https://github.com/JetBrains/compose-multiplatform-core/pull/2251)
- [js] there's no need to manually add skiko.js to the html page any more [#2264](https://github.com/JetBrains/compose-multiplatform-core/pull/2264)
- Introduce `composeCompatibilityBrowserDistribution`  task. This task combines two prod distributions - for js and for wasm in such way so that if modern required features are not supported by the consumer browser, application switch to js mode [#5375](https://github.com/JetBrains/compose-multiplatform/pull/5375)

## Fixes

### Multiple Platforms

- Fix text ellipsis if there's not enough vertical space to fit all lines [#2246](https://github.com/JetBrains/compose-multiplatform-core/pull/2246)
- Fix "IrLinkageError: Function can not be called: No function found for symbol" [#2293](https://github.com/JetBrains/compose-multiplatform-core/pull/2293)
- `ExperimentalMaterial3ExpressiveApi` annotation removed from no-longer-experimental API [#2298](https://github.com/JetBrains/compose-multiplatform-core/pull/2298)

### iOS

- Do not flatten accessibility tree inside accessibility elements [#2243](https://github.com/JetBrains/compose-multiplatform-core/pull/2243)

### Desktop

- [macOS] Fix composite (e.g. Chinese) input after pressing backspace [#2250](https://github.com/JetBrains/compose-multiplatform-core/pull/2250)
- [TextField] Fixed duplication of the composed characters when moving the caret by clicking during a composition [#2255](https://github.com/JetBrains/compose-multiplatform-core/pull/2255)
- _(prerelease fix)_ Close the context menu when a menu item is clicked (old context menu API) [#2259](https://github.com/JetBrains/compose-multiplatform-core/pull/2259)
- Correctly remove `SwingPanel` children of `ComposePanel`, when the compose panel is itself removed from the hierarchy [#2277](https://github.com/JetBrains/compose-multiplatform-core/pull/2277)
- _(prerelease fix)_ Fix `DialogWindow` causing a taskbar icon to be displayed in some cases where it shouldn't [#2291](https://github.com/JetBrains/compose-multiplatform-core/pull/2291)

### Web

- Fix software keyboard behaviour for Compose Text Fields in iOS Safari [#2260](https://github.com/JetBrains/compose-multiplatform-core/pull/2260)
- Fixed the bugs with composite text input [#2256](https://github.com/JetBrains/compose-multiplatform-core/pull/2256)
- _(prerelease fix)_ Hide disabled context menu items in the web text toolbar menu [#2268](https://github.com/JetBrains/compose-multiplatform-core/pull/2268)
- _(prerelease fix)_ Show the "paste" item regardless of the the clipboard content state if the Clipboard  API is supported [#2267](https://github.com/JetBrains/compose-multiplatform-core/pull/2267)
- _(prerelease fix)_ The context menu will not show the Clipboard-related items when the Clipboard API are not supported by a browser [#2266](https://github.com/JetBrains/compose-multiplatform-core/pull/2266)
- Fixed the issue with software keyboard when it was shown repeatedly in Chrome mobile [#2279](https://github.com/JetBrains/compose-multiplatform-core/pull/2279)
- _(prerelease fix)_ The context menu had only "Select All" item when targeting k/js [#2296](https://github.com/JetBrains/compose-multiplatform-core/pull/2296)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.0-beta03`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.0-rc01)
  - [UI 1.9.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.0-rc01)
  - [Foundation 1.9.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.0-rc01)
  - [Material 1.9.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.0-rc01)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0-beta03`. Based on [Jetpack Compose Material3 1.4.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta01)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha05`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-alpha10](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha10)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.2`. Based on [Jetpack Lifecycle 2.9.2](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.2)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-beta05`. Based on [Jetpack Navigation 2.9.1](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.1)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.2`. Based on [Jetpack Savedstate 1.3.1](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.1)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0-beta01`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.9.0-beta01 (July 2025)

_Changes since 1.9.0-alpha03_

## Highlights

### Web

- Text context menu is supported on web platforms for both modes: mobile and desktop [#2207](https://github.com/JetBrains/compose-multiplatform-core/pull/2207)
- Introduce the basic support of accessibility in the web target [#2188](https://github.com/JetBrains/compose-multiplatform-core/pull/2188)

## Migration Notes

### Multiple Platforms

- Material3 versioning is decoupled for the Compose Multiplatform 1.9.* release due the upstream Jetpack Compose Material3 1.4 has not been released as stable yet [#5360](https://github.com/JetBrains/compose-multiplatform/pull/5360)
- `compose.material3` now points to the latest stable Material3 version, 1.8.2. If the latest Material3 features are needed, please include it this way: [#5360](https://github.com/JetBrains/compose-multiplatform/pull/5360)

  ```
  implementation("org.jetbrains.compose.material3:material3:1.9.0-alpha04")
  ```

### iOS

- Remove experimental attribute from `UIKitInteropInteractionMode` [#2215](https://github.com/JetBrains/compose-multiplatform-core/pull/2215)

### Web

- `fun ComposeViewport` with `viewportContainerId: String?` parameter now can be called from a web-common `fun main` (k/js and k/wasm) [#2226](https://github.com/JetBrains/compose-multiplatform-core/pull/2226)

## Features

### Multiple Platforms

- Extended the `@Preview` (`org.jetbrains.compose.ui.tooling.preview`) annotation with the following parameters: name, group, widthDp, heightDp, locale, showBackground, backgroundColor. IDE (IJ or AS) will pick up these parameters in the same way it works for `androidx` Preview annotations [#5339](https://github.com/JetBrains/compose-multiplatform/pull/5339)

### iOS

- Support new context menu API with default menu [#2214](https://github.com/JetBrains/compose-multiplatform-core/pull/2214)
- Add support for frame rate voting [#2205](https://github.com/JetBrains/compose-multiplatform-core/pull/2205)
- Support scroll commands with Voice Control [#2234](https://github.com/JetBrains/compose-multiplatform-core/pull/2234)
- Implement accessibility scroll to focused interop views [#2228](https://github.com/JetBrains/compose-multiplatform-core/pull/2228)

### Desktop

- Basic support for new context menu API [#2196](https://github.com/JetBrains/compose-multiplatform-core/pull/2196)
- Added experimental support for save and restore compose state. `ComposePanel`, `ComposeWindow` and `ComposeDialog` now has `savedState` constructor parameter to restore previous state and `saveState` function to save the current state for later use [#2225](https://github.com/JetBrains/compose-multiplatform-core/pull/2225)

### Web

- Support the new context menu API in web targets in the desktop mode [#2224](https://github.com/JetBrains/compose-multiplatform-core/pull/2224)

### Resources

- Added `JvmResourceReader` API and made `LocalResourceReader` public to allow providing a custom classloader for desktop target [#5334](https://github.com/JetBrains/compose-multiplatform/pull/5334)

### Navigation

- A new API was added to bind the browser navigation state with the `NavController` - `suspend fun NavController.bindToBrowserNavigation`. And the existing function `suspend fun Window.bindToNavigation` is deprecated now [#2189](https://github.com/JetBrains/compose-multiplatform-core/pull/2189)

## Fixes

### Multiple Platforms

- Fix extra draw invalidations during scrolling (1.8 regression) [#2212](https://github.com/JetBrains/compose-multiplatform-core/pull/2212)
- _(prerelease fix)_ Fix trigger of `Modifier.onFirstVisible` modifier (added in Jetpack Compose `1.9.0-alpha03`) [#2233](https://github.com/JetBrains/compose-multiplatform-core/pull/2233)

### iOS

- Change the accessibility selection to the element that has most recently been focused on [#2217](https://github.com/JetBrains/compose-multiplatform-core/pull/2217)
- Fix Full Keyboard Access on iOS 17 and lower [#2216](https://github.com/JetBrains/compose-multiplatform-core/pull/2216)
- _(prerelease fix)_ Fixed `Undefined symbols for architecture arm64: _kfun:androidx.compose.material3.adaptive.WindowAdaptiveInfo` [#2236](https://github.com/JetBrains/compose-multiplatform-core/pull/2236)
- Fixes the appearance of the keyboard when a pop-up or dialog on the background is dismissed [#2240](https://github.com/JetBrains/compose-multiplatform-core/pull/2240)

### Desktop

- Fixed the case when the IME's first event to a text field is to commit a composition [#2210](https://github.com/JetBrains/compose-multiplatform-core/pull/2210)
- Elements marked with Modifier.semantics { hideFromAccessibility() } should now be correctly hidden from a11y [#2204](https://github.com/JetBrains/compose-multiplatform-core/pull/2204)
- _(prerelease fix)_ Fix focus switching for ComposePanel embedded in Swing UI [#2232](https://github.com/JetBrains/compose-multiplatform-core/pull/2232)
- _(prerelease fix)_ Fix Maven project doesn't work with 1.9.0-alpha03 [#2248](https://github.com/JetBrains/compose-multiplatform-core/pull/2248)

### Web

- Web Lifecycle triggers `START`/`STOP` events on the `visibilitychange` callback now [#2219](https://github.com/JetBrains/compose-multiplatform-core/pull/2219)
- Lifecycle fix on iOS Safari: now touch events trigger `ON_RESUME` because Safari ignores interactions and doesn't request the focus [#2219](https://github.com/JetBrains/compose-multiplatform-core/pull/2219)

### Gradle Plugin

- Fix codesigning on macOS when the developer id contains non-ASCII characters. Note that this requires JDK 21 or later [#5358](https://github.com/JetBrains/compose-multiplatform/pull/5358)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.0-beta01`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.0-beta02)
  - [UI 1.9.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.0-beta02)
  - [Foundation 1.9.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.0-beta02)
  - [Material 1.9.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.0-beta02)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Compose Material3 libraries `org.jetbrains.compose.material3:material3*:1.9.0-alpha04`. Based on [Jetpack Compose Material3 1.4.0-alpha17](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha17)
- Compose Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha04`. Based on [Jetpack Compose Material3 Adaptive 1.2.0-alpha08](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha08)
- Graphics-Shapes library `org.jetbrains.androidx.graphics:graphics-shapes:1.0.0-alpha09`. Based on [Jetpack Graphics-Shapes 1.0.1](https://developer.android.com/jetpack/androidx/releases/graphics#graphics-shapes-1.0.1)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.1`. Based on [Jetpack Lifecycle 2.9.1](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.1)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-beta04`. Based on [Jetpack Navigation 2.9.1](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.1)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.1`. Based on [Jetpack Savedstate 1.3.0](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.0)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0-alpha09`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.9.0-alpha03 (July 2025)

_Changes since 1.9.0-alpha02_

## Highlights

### Web

- Added a `WebElementView` Composable function for embedding the absolutely positioned HTML content in Compose for web target. It's supported only when using `ComposeViewport` entry point and it's not supported with `CanvasBasedWindow`, which is deprecated now [#2145](https://github.com/JetBrains/compose-multiplatform-core/pull/2145)

## Migration Notes

### Desktop

- `Window` and `DialogWindow` overloads that create a `ComposeWindow`/`ComposeDialog` have been renamed to `SwingWindow`/`SwingDialog` and the old versions have been deprecated [#2141](https://github.com/JetBrains/compose-multiplatform-core/pull/2141)
- When the window is iconified, converting to/from screen coordinates (with e.g.`LayoutCoordinates.positionOnScreen()`) will return `Offset.Unspecified` [#2163](https://github.com/JetBrains/compose-multiplatform-core/pull/2163)
- Deprecated experimental `Modifier.mouseClickable` is removed. See https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-desktop-mouse-events.html for alternatives [#2194](https://github.com/JetBrains/compose-multiplatform-core/pull/2194)

### Web

- Setting `org.jetbrains.compose.experimental.jscanvas.enabled=true` is not required anymore when having a kotlin/js target [#5340](https://github.com/JetBrains/compose-multiplatform/pull/5340)

## Features

### Multiple Platforms

- Support customizable shadows [#2183](https://github.com/JetBrains/compose-multiplatform-core/pull/2183)

### iOS

- Add support for `keepScreenOn` modifier [#2180](https://github.com/JetBrains/compose-multiplatform-core/pull/2180)

### Desktop

- Added `SwingFrame` and `SwingDialog` composables that allow configuring the window/dialog before it is shown [#2139](https://github.com/JetBrains/compose-multiplatform-core/pull/2139)
- Full `AnnotatedString` is available as a data flavor in `ClipEntry`, instead of only its text [#2092](https://github.com/JetBrains/compose-multiplatform-core/pull/2092)
- Add `RenderSettings.SwingGraphics` option for `ComposePanel.renderSettings` argument [#2071](https://github.com/JetBrains/compose-multiplatform-core/pull/2071)

## Fixes

### Multiple Platforms

- Fixed caret placement near glyphs if glyphs are compound symbols and part of them are non-spacing marks [#2147](https://github.com/JetBrains/compose-multiplatform-core/pull/2147)
- _(prerelease fix)_ Fix applying `colorFilter` and `blendMode` from `GraphicsLayerScope` to `GraphicsLayer` [#2184](https://github.com/JetBrains/compose-multiplatform-core/pull/2184)

### Desktop

- [Accessibility, macOS] Fixed VoiceOver to announce a Slider's value on every change [#2152](https://github.com/JetBrains/compose-multiplatform-core/pull/2152)
- Return `Offset.Unspecified` instead of throwing an exception in `LayoutCoordinates.localToScreen` and `LayoutCoordinates.screenToLocal` [#2160](https://github.com/JetBrains/compose-multiplatform-core/pull/2160)
- When the window moves or becomes iconified/de-iconified, all instances of `Modifier.onGloballyPositioned` will be called [#2163](https://github.com/JetBrains/compose-multiplatform-core/pull/2163)
- Implemented a context menu for `BasicTextField(TextFieldState)` [#2168](https://github.com/JetBrains/compose-multiplatform-core/pull/2168)
- _(prerelease fix)_ `Could not find org.jetbrains.androidx.window:window-core-desktop:1.4.0-alpha07` when using `material-adaptive` or `material3-adaptive-navigation-suite` [#2179](https://github.com/JetBrains/compose-multiplatform-core/pull/2179)
- `SemanticsProperties.Text` and `SemanticsProperties.ContentDescription` values will now be correctly concatenated when `Modifier.semantics(mergeDescendants = true)` is used [#2202](https://github.com/JetBrains/compose-multiplatform-core/pull/2202)

### Web

- Fixed an unexpected back gesture after a horizontal scroll [#2186](https://github.com/JetBrains/compose-multiplatform-core/pull/2186)
- Mitigate a typing delay regression in Safari browsers [#2195](https://github.com/JetBrains/compose-multiplatform-core/pull/2195)

### Gradle Plugin

- Don't fail gradle sync if TargetFormat.AppImage is specified in `targetFormats` on macOS [#5332](https://github.com/JetBrains/compose-multiplatform/pull/5332)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.0-alpha03`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.0-beta01)
  - [UI 1.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.0-beta01)
  - [Foundation 1.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.0-beta01)
  - [Material 1.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.0-beta01)
  - [Material3 1.4.0-alpha16](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha16)

- Graphics-Shapes library `org.jetbrains.androidx.graphics:graphics-shapes:1.0.0-alpha09`. Based on [Jetpack Graphics-Shapes 1.0.1](https://developer.android.com/jetpack/androidx/releases/graphics#graphics-shapes-#1.0.1)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.1`. Based on [Jetpack Lifecycle 2.9.1](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.1)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha03`. Based on [Jetpack Material3 Adaptive 1.2.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha07)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-beta03`. Based on [Jetpack Navigation 2.9.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.1`. Based on [Jetpack Savedstate 1.3.0](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.0)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0-alpha08`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.8.2 (June 2025)

_Changes since 1.8.1_

## Fixes

### Multiple Platforms

- Fixed `TextField(TextFieldValue)` when used with a visual transformation with a non-identity offset mapping (potentially even crashing) [#2130](https://github.com/JetBrains/compose-multiplatform-core/pull/2130)
- Fixed a memory leak and performance degradation when `ComposeUiFlags.isRectTrackingEnabled` set to `true` (by default) [#2123](https://github.com/JetBrains/compose-multiplatform-core/pull/2123)
- Fixed caret placement near glyphs if glyphs are compound symbols and part of them are non-spacing marks [#2155](https://github.com/JetBrains/compose-multiplatform-core/pull/2155)
- Support Preview parameters for Previews in common source sets in IJ and AS. Note: IDEs also need to implement support on their end. Please check the respective IDE release notes to confirm this is supported [#5323](https://github.com/JetBrains/compose-multiplatform/pull/5323)
  
  Example usage:
  
  ```
  import androidx.compose.runtime.Composable
  import org.jetbrains.compose.ui.tooling.preview.Preview
  import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
  import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
  
  class MyPreviewParameterProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf("Hello, Compose!", "Hello, World!")
  }
  
  /**
   * This function will generate two preview images with different texts.
   */
  @Preview
  @Composable
  fun MyPreview(@PreviewParameter(MyPreviewParameterProvider::class) text: String) {
    Text(text)
  }
  ```
  

### Desktop

- Fix the positioning of the IME popup being too far away from the text, on screens with density greater than `1.0` [#2158](https://github.com/JetBrains/compose-multiplatform-core/pull/2158)

### Navigation

- Fix the browser navigation integration problem due encoded routes [#2143](https://github.com/JetBrains/compose-multiplatform-core/pull/2143)
- Fix a crash on iOS when a `NavHost` is located in a scrollable container [#2146](https://github.com/JetBrains/compose-multiplatform-core/pull/2146)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.2`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.2](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.2)
  - [UI 1.8.2](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.2)
  - [Foundation 1.8.2](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.2)
  - [Material 1.8.2](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.2)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.1`. Based on [Jetpack Lifecycle 2.9.1](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.1)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.2`. Based on [Jetpack Material3 Adaptive 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-beta03`. Based on [Jetpack Navigation 2.9.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.1`. Based on [Jetpack Savedstate 1.3.0](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.0)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0-alpha07`. Based on [Jetpack WindowManager 1.4.0-alpha04](https://developer.android.com/jetpack/androidx/releases/window#1.4.0-alpha04)

---

# 1.9.0-alpha02 (June 2025)

_Changes since 1.8.1_

## Highlights

### Multiple Platforms

- `material3` library now includes new experimental `MaterialExpressiveTheme` [#2127](https://github.com/JetBrains/compose-multiplatform-core/pull/2127)

## Migration Notes

### Gradle Plugin

- The Compose Gradle plugin requires the Kotlin Gradle plugin version 2.+ now. Old `org.jetbrains.compose.compiler` is not supported anymore and the API to configure it was removed [#5283](https://github.com/JetBrains/compose-multiplatform/pull/5283)

## Features

### Multiple Platforms

- Adopted a change in `ComposeUiTest` API. The  `block` in `runComposeUiTest` is `suspend` now. It allows to call `awaitIdle` and other suspend functions. It ensures a correct execution of a test on all platforms. See the web specifics in `kotlinx.coroutines.test.runTest` [documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html) [#2066](https://github.com/JetBrains/compose-multiplatform-core/pull/2066)

### iOS

- Add support for native IME configuration with `PlatformImeOptions` [#2108](https://github.com/JetBrains/compose-multiplatform-core/pull/2108)

### Desktop

- Add accessibility role for `Switch`, reporting it as a checkbox [#2136](https://github.com/JetBrains/compose-multiplatform-core/pull/2136)

## Fixes

### Multiple Platforms

- Fixed `TextField(TextFieldValue)` when used with a visual transformation with a non-identity offset mapping (potentially even crashing) [#2117](https://github.com/JetBrains/compose-multiplatform-core/pull/2117)
- Fixed a memory leak and performance degradation when `ComposeUiFlags.isRectTrackingEnabled` set to `true` (default) [#2112](https://github.com/JetBrains/compose-multiplatform-core/pull/2112)
- Support Preview parameters for Previews in common source sets in IJ and AS. Note: IDEs also need to implement support on their end. Please check the respective IDE release notes to confirm this is supported [#5319](https://github.com/JetBrains/compose-multiplatform/pull/5319)
  Example usage:
  ```
  import androidx.compose.runtime.Composable
  import org.jetbrains.compose.ui.tooling.preview.Preview
  import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
  import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
  class MyPreviewParameterProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf("Hello, Compose!", "Hello, World!")
  }
  /**
   * This function will generate two preview images with different texts
   */
  @Preview
  @Composable
  fun MyPreview(@PreviewParameter(MyPreviewParameterProvider::class) text: String) {
    Text(text)
  }
  ```

### iOS

- Fix issue where keyboard would appear after second tap when text input session was intercepted [#2049](https://github.com/JetBrains/compose-multiplatform-core/pull/2049)

### Desktop

- [macOS] Fix the background flashing when closing a window/dialog and an animation is running [#2058](https://github.com/JetBrains/compose-multiplatform-core/pull/2058)
- [macOS; JBR] Fixed the current composition in a text field being duplicated into another text field when switching focus to it [#2026](https://github.com/JetBrains/compose-multiplatform-core/pull/2026)
- [macOS] Fixed strange glyph being displayed in a text field if window becomes unfocused, then focused again while there's an active composition in the text field (after pressing e.g. backspace) [#2026](https://github.com/JetBrains/compose-multiplatform-core/pull/2026)
- [macOS] Fix showing the input method toolbar before any text field becomes focused (on JBR only; other runtimes continue to be buggy) [#2047](https://github.com/JetBrains/compose-multiplatform-core/pull/2047)
- Improved performance for `ComposePanel` with `System.setProperty("compose.swing.render.on.graphics", "true")` [#2097](https://github.com/JetBrains/compose-multiplatform-core/pull/2097)
- Fix the positioning of the IME popup being too far away from the text, on screens with density greater than 1 [#2118](https://github.com/JetBrains/compose-multiplatform-core/pull/2118)
- Fixed the position of the IME popup, which was below the previous, rather than the current, cursor position [#2122](https://github.com/JetBrains/compose-multiplatform-core/pull/2122)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.9.0-alpha02`. Based on Jetpack Compose libraries:
  - [Runtime 1.9.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.9.0-alpha03)
  - [UI 1.9.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.9.0-alpha03)
  - [Foundation 1.9.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.9.0-alpha03)
  - [Material 1.9.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-material#1.9.0-alpha03)
  - [Material3 1.4.0-alpha15](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha15)

- Graphics-Shapes library `org.jetbrains.androidx.graphics:graphics-shapes:1.0.0-alpha08`. Based on [Jetpack Graphics-Shapes 1.0.1](https://developer.android.com/jetpack/androidx/releases/graphics#graphics-shapes-#1.0.1)
- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0`. Based on [Jetpack Lifecycle 2.9.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.2.0-alpha02`. Based on [Jetpack Material3 Adaptive 1.2.0-alpha06](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.2.0-alpha06)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-beta02`. Based on [Jetpack Navigation 2.9.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0)
- Savedstate library `org.jetbrains.androidx.savedstate:savedstate:1.3.0`. Based on [Jetpack Savedstate 1.3.0](https://developer.android.com/jetpack/androidx/releases/savedstate#1.3.0)
- WindowManager Core library `org.jetbrains.androidx.window:window-core:1.4.0-alpha07`. Based on [Jetpack WindowManager 1.4.0](https://developer.android.com/jetpack/androidx/releases/window#1.4.0)

---

# 1.8.1 (May 2025)

_Changes since 1.8.0_

## Features

### Resources

- Now a Compose library with resources may be built and used as XCFramework (it requires Kotlin Gradle plugin 2.2 or higher) [#5294](https://github.com/JetBrains/compose-multiplatform/pull/5294)
- Gradle Plugin DSL to change the generated `Res` class name [#5296](https://github.com/JetBrains/compose-multiplatform/pull/5296)

## Fixes

### Multiple Platforms

- Fix incorrect pointer position calculation with rotation around unspecified pivot [#2082](https://github.com/JetBrains/compose-multiplatform-core/pull/2082)

### iOS

- Fix dialogs after modal view controller presentation [#2085](https://github.com/JetBrains/compose-multiplatform-core/pull/2085)
- Fix issue where `androidx.compose.material3.ModalBottomSheet` closes after any tap [#2086](https://github.com/JetBrains/compose-multiplatform-core/pull/2086)
- Fix context menu appearance after triple-tap [#2087](https://github.com/JetBrains/compose-multiplatform-core/pull/2087)
- Fix a memory leak in `ComposeUIViewController` when text input starts [#2088](https://github.com/JetBrains/compose-multiplatform-core/pull/2088)
- Use the cross-fade animation effect when rotating the screen with interop views [#2101](https://github.com/JetBrains/compose-multiplatform-core/pull/2101)
- Show an error message when `UIKitViewController` inside `Popup` or `Dialog` [#2102](https://github.com/JetBrains/compose-multiplatform-core/pull/2102)
- Fix an issue where the keyboard would appear after the second tap when the text input session was intercepted [#2103](https://github.com/JetBrains/compose-multiplatform-core/pull/2103)

### Desktop

- [Linux] Fix `svgPainter` doesn't show any images [#2096](https://github.com/JetBrains/compose-multiplatform-core/pull/2096)
- Fix deadlock between `BroadcastFrameClock.lock` and `Recomposer.stateLock` [#2098](https://github.com/JetBrains/compose-multiplatform-core/pull/2098)
- Fix "Serializer for class is not found" using `androidx.navigation` and running `./gradlew runRelease` [#5314](https://github.com/JetBrains/compose-multiplatform/pull/5314)
- `kotlinx.serialization` ProGuard rules are bundled in the Compose Gradle plugin [#5314](https://github.com/JetBrains/compose-multiplatform/pull/5314)

### Web

- Fixed the positioning and the dimensions of the backing text input (HTML element). The bug used to lead to unexpected scrolls on the page due to the browser trying to bring the HTML element into a view [#2081](https://github.com/JetBrains/compose-multiplatform-core/pull/2081)

### Resources

- Fix IDE highlighting/resolution when a generated file with resource accessors is too big [#5298](https://github.com/JetBrains/compose-multiplatform/pull/5298)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.1`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.1)
  - [UI 1.8.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.1)
  - [Foundation 1.8.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.1)
  - [Material 1.8.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.1)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0`. Based on [Jetpack Lifecycle 2.9.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-beta02`. Based on [Jetpack Navigation 2.9.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.1`. Based on [Jetpack Material3 Adaptive 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0)

---

# 1.8.0 (May 2025)

_Changes since 1.7.3_

## Highlights

### Multiple Platforms

- Compose Multiplatform codebase is fully migrated to K2. Please note that native and web klibs can be consumed only with Kotlin 2.1.0 or newer. Also, due to underlying changes in the compiler plugin, it's better to recompile libraries against the new version. Please let us know if you find any compatibility issues during this migration [#1778](https://github.com/JetBrains/compose-multiplatform-core/pull/1778)
- Implement multiplatform `BackHandler` and `PredictiveBackHandler`. And use them in material3 widgets and androidx-navigation library [#1771](https://github.com/JetBrains/compose-multiplatform-core/pull/1771)

### iOS

- Remove experimental flag from `fun enableTraceOSLog()` [#1652](https://github.com/JetBrains/compose-multiplatform-core/pull/1652)

### Web

- Improves text input support in Safari on  mobile and desktop [#1941](https://github.com/JetBrains/compose-multiplatform-core/pull/1941)
- Correct certain text input scenarios on Web targets [#1941](https://github.com/JetBrains/compose-multiplatform-core/pull/1941)

### Resources

- Add `FontVariation.Settings` support to the resources library [#5183](https://github.com/JetBrains/compose-multiplatform/pull/5183)

## Breaking Changes

### Tests

- `runOnIdle` will now execute `action` on the UI thread aligning the behavior with Android [#1601](https://github.com/JetBrains/compose-multiplatform-core/pull/1601)
- `runOnIdle` will no longer call `waitForIdle` after executing the action aligning the behavior with Android [#1601](https://github.com/JetBrains/compose-multiplatform-core/pull/1601)
- Advancing `mainClock` such that it doesn't reach the next frame, will no longer cause a recomposition [#1618](https://github.com/JetBrains/compose-multiplatform-core/pull/1618)
- [IdlingResource](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/IdlingResource) interface was moved from commonMain to android and desktop source sets. The related experimental methods of [ComposeUiTest](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/ComposeUiTest) were moved too. They are not available for Web and iOS anymore. Consider using [waitUntil function](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/ComposeUiTest#waitUntil(kotlin.String,kotlin.Long,kotlin.Function0)) as an alternative. Note: it's a breaking change only for Web and iOS, but not for Desktop and Android [#1822](https://github.com/JetBrains/compose-multiplatform-core/pull/1822)

### Multiple Platforms

- Multiplatform lifecycle was migrated from a internal `core-bundle` module to the androidx SavedState. Libraries that use `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate`  or `org.jetbrains.androidx.savedstate:savedstate` should migrate to the latest version [#1850](https://github.com/JetBrains/compose-multiplatform-core/pull/1850)
- A custom implementation for deprecated `LocalTextInputService` is no longer supported [#1974](https://github.com/JetBrains/compose-multiplatform-core/pull/1974)

### iOS

- Update `AccessibilitySyncOptions` and remove `AccessibilityDebugLogger` from public API [#1604](https://github.com/JetBrains/compose-multiplatform-core/pull/1604)
- Remove obsolete Canvas Layers mode on iOS [#1680](https://github.com/JetBrains/compose-multiplatform-core/pull/1680)
- Add Composable annotation to the `WindowInsets.Companion.waterfall` getter to match the expected API [#1919](https://github.com/JetBrains/compose-multiplatform-core/pull/1919)

### Desktop

- Deprecated/experimental `Modifier.onExternalDrag` has been removed - common `Modifier.dragAndDropTarget` API should be used instead [#1606](https://github.com/JetBrains/compose-multiplatform-core/pull/1606)

## Migration Notes

### Multiple Platforms

- [Google Maven](https://maven.google.com/) now contains some artifacts for all Kotlin targets including Wasm and JS. Compose Multiplatform now depends on those artifacts and user projects might need to add `google()` repo to `repositories {...}` block if it is not there yet [#1819](https://github.com/JetBrains/compose-multiplatform-core/pull/1819)
- material/material3 libraries no longer add a dependency to `material-icons-core` so if your project relied on that, you will have to explicitly add that dependency in your `build.gradle[.kts]` files: [#2030](https://github.com/JetBrains/compose-multiplatform-core/pull/2030)
  ```
  implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
  ```

### iOS

- `LocalUIViewController` moved to the `androidx.compose.ui.uikit` module [#1608](https://github.com/JetBrains/compose-multiplatform-core/pull/1608)
- `ComposeUIViewControllerDelegate` marked as deprecated. Use the parent view controller to override the methods of the `UIViewController` class [#1651](https://github.com/JetBrains/compose-multiplatform-core/pull/1651)
- Experimental classes `CupertinoScrollDecayAnimationSpec` and `CupertinoOverscrollEffect` are removed from public API [#1806](https://github.com/JetBrains/compose-multiplatform-core/pull/1806)

### Gradle Plugin

- The Compose Gradle Plugin requires Kotlin Gradle Plugin 2.+ version now. Old `org.jetbrains.compose.compiler` is not supported anymore and the API to configure it was removed [#5293](https://github.com/JetBrains/compose-multiplatform/pull/5293)

## Features

### Multiple Platforms

- Support configurable vertical text centering via `LineHeightStyle.Alignment` [#1569](https://github.com/JetBrains/compose-multiplatform-core/pull/1569)
- Support Variable Fonts In All Platforms [#1623](https://github.com/JetBrains/compose-multiplatform-core/pull/1623)
- Update skia to m132 [#1823](https://github.com/JetBrains/compose-multiplatform-core/pull/1823)
- Adopt a new `Clipboard` interface with suspend functions, which work correctly on all targets including Web. The `ClipboardManager` was deprecated because it was not possible to correctly implement it for Web [#1796](https://github.com/JetBrains/compose-multiplatform-core/pull/1796)

### iOS

- Support state announcements for scrollable lists in VoiceOver [#1644](https://github.com/JetBrains/compose-multiplatform-core/pull/1644)
- Support for accessibility gestures for left-to-right languages [#1663](https://github.com/JetBrains/compose-multiplatform-core/pull/1663)
- (Experimental) `ComposeUIViewControllerConfiguration.useSeparateRenderThreadWhenPossible` flag that allows offloading GPU commands encoding to the separate thread and improving performance [#1694](https://github.com/JetBrains/compose-multiplatform-core/pull/1694)
- Initial Drag&Drop support [#1690](https://github.com/JetBrains/compose-multiplatform-core/pull/1690)
- Align Compose components semantics with UIKit views accessibility [#1719](https://github.com/JetBrains/compose-multiplatform-core/pull/1719)
- Accessibility navigation uses safe area to calculate when focused rect is out of bounds [#1745](https://github.com/JetBrains/compose-multiplatform-core/pull/1745)
- Support VoiceControl on iOS [#1780](https://github.com/JetBrains/compose-multiplatform-core/pull/1780)
- `AccessibilitySyncOptions` removed. The accessibility tree is built on demand [#1780](https://github.com/JetBrains/compose-multiplatform-core/pull/1780)
- Calculate the order and location of semantic elements in the same way as it's done on Android [#1809](https://github.com/JetBrains/compose-multiplatform-core/pull/1809)
- Support `UIAccessibilityContainerTypeSemanticGroup` for traversal groups [#1809](https://github.com/JetBrains/compose-multiplatform-core/pull/1809)
- Compose works correctly with nested `UIScrollView`s, as well as within `UIScrollView`s [#1818](https://github.com/JetBrains/compose-multiplatform-core/pull/1818)
- Added the ability to close modal Compose view controllers (with non-scrollable content on them) with a swipe gesture [#1818](https://github.com/JetBrains/compose-multiplatform-core/pull/1818)
- Support new haptic feedback types [#1831](https://github.com/JetBrains/compose-multiplatform-core/pull/1831)
- Support for focusable nodes when Full Keyboard Access is enabled on iOS [#1825](https://github.com/JetBrains/compose-multiplatform-core/pull/1825)
- Floating cursor support for `BasicTextField(TextFieldState)` [#1598](https://github.com/JetBrains/compose-multiplatform-core/pull/1598)
- Add support for Bold Text accessibility setting [#1846](https://github.com/JetBrains/compose-multiplatform-core/pull/1846)
- Bhojpuri language support for VoiceOver [#1838](https://github.com/JetBrains/compose-multiplatform-core/pull/1838)
- Add support for Reduce Motion accessibility setting [#1847](https://github.com/JetBrains/compose-multiplatform-core/pull/1847)
- Default `androidx.navigation` transition animation on iOS is as close as possible to the iOS back gesture [#1861](https://github.com/JetBrains/compose-multiplatform-core/pull/1861)
- Support accessibility text input [#1875](https://github.com/JetBrains/compose-multiplatform-core/pull/1875)
- Support text input for UI Tests [#1875](https://github.com/JetBrains/compose-multiplatform-core/pull/1875)
- Accessibility: added ability to traverse nodes within a scrollable container [#1837](https://github.com/JetBrains/compose-multiplatform-core/pull/1837)
- Add ability to recreate Composable after `ComposeUIViewController` leaves view controller hierarchy [#1877](https://github.com/JetBrains/compose-multiplatform-core/pull/1877)
- Support trackpad pointer input [#1893](https://github.com/JetBrains/compose-multiplatform-core/pull/1893)
- Save Composable state when view controller leaves view controller hierarchy [#1904](https://github.com/JetBrains/compose-multiplatform-core/pull/1904)
- Support text input when Full Keyboard Access is enabled [#1917](https://github.com/JetBrains/compose-multiplatform-core/pull/1917)
- Added native behavior for tap, long tap and multiple tap to `BasicTextField(TextFieldState)` [#1923](https://github.com/JetBrains/compose-multiplatform-core/pull/1923)
- Magnifier support for `BasicTextField(TextFieldState)` [#1926](https://github.com/JetBrains/compose-multiplatform-core/pull/1926)

### Desktop

- Improved performance on Windows by replacing the native code compiler to Clang (in 1.8.0 comparing to 1.7.3) [#1863](https://github.com/JetBrains/compose-multiplatform-core/pull/1863)
  - Software rendering (used on some VMs) FPS is **6.397x higher** on average
  - Reducing the size of a packaged application. Example https://kmp.jetbrains.com:
     - the installer size is changed from 44.9 Mb to 44.1 Mb
     - the installed size is changed from 107 Mb to 103 Mb
- The default ProGuard version is set to 7.7.0 [#5279](https://github.com/JetBrains/compose-multiplatform/pull/5279)
   - If there are any new errors in the release build, update [the ProGuard rules](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-native-distribution.html#minification-and-obfuscation)
   - A usual workaround is to add `-keep class` for the associated with error class in "Location:"
   - If the error contains `androidx.` package, it might a Compose bug, please report in https://youtrack.jetbrains.com/issues/CMP. The `-keep class` workaround should also work in this case

### Web

- Adds experimental `PointerIcon.fromKeyword` function to change the browser cursor [#1704](https://github.com/JetBrains/compose-multiplatform-core/pull/1704)

### Resources

- Add new API to preload and cache font and image resources on web targets: `preloadFont`, `preloadImageBitmap`, `preloadImageVector` [#5159](https://github.com/JetBrains/compose-multiplatform/pull/5159)

### Gradle Plugin

- Support compose resources in `androidLibrary` target [#5157](https://github.com/JetBrains/compose-multiplatform/pull/5157)

### Navigation

- Basic support a navigation by deep links [#1610](https://github.com/JetBrains/compose-multiplatform-core/pull/1610)
- Commonize `navController.navigate(Uri)` method [#1617](https://github.com/JetBrains/compose-multiplatform-core/pull/1617)
- Implemented non-android `navController.handleDeepLink(NavDeepLinkRequest)` method [#1617](https://github.com/JetBrains/compose-multiplatform-core/pull/1617)
- New API to configure browser navigation buttons and the address line [#1621](https://github.com/JetBrains/compose-multiplatform-core/pull/1621)
- Navigation via a browser address field [#1640](https://github.com/JetBrains/compose-multiplatform-core/pull/1640)

## Fixes

### Tests

- Clear the canvas before rendering each frame in tests, to avoid drawing different frames on top of each other, resulting in incorrect images being returned by `captureToImage` [#1820](https://github.com/JetBrains/compose-multiplatform-core/pull/1820)

### Multiple Platforms

- Fix changing `FontRenderingSettings` is not reflected until composition restarts [#1595](https://github.com/JetBrains/compose-multiplatform-core/pull/1595)
- The overhead for running an empty test has been significantly reduced [#1615](https://github.com/JetBrains/compose-multiplatform-core/pull/1615)
- Fix missing implementation on non-Android platforms for `ModalDrawerSheet` overload with `DrawerState` argument [#1763](https://github.com/JetBrains/compose-multiplatform-core/pull/1763)
- Fix light position for during shadow rendering to match [the Material specification](https://m2.material.io/design/environment/light-shadows.html#light) and Android behaviour [#1754](https://github.com/JetBrains/compose-multiplatform-core/pull/1754)
- Fix light source position for shadow rendering to match [the Material specification](https://m2.material.io/design/environment/light-shadows.html#light) and Android behavior [#1784](https://github.com/JetBrains/compose-multiplatform-core/pull/1784)
- Fix inconsistency between Android and Desktop in `placeWithLayer` scale application: offset is not scaled now (Android behavior) [#1784](https://github.com/JetBrains/compose-multiplatform-core/pull/1784)
- Fixed the output of `TextField(TextFieldState)` (aka `BasicTextField2`) transformations incorrectly leeching into the untransformed text itself, when input method is used (Chinese and other languages with multi-keystroke character input) [#1833](https://github.com/JetBrains/compose-multiplatform-core/pull/1833)
- Fix possible `IllegalArgumentException: Object already closed` during `GraphicsLayer.record` [#1839](https://github.com/JetBrains/compose-multiplatform-core/pull/1839)
- Compose runtime module now exposes its dependency on Kotlin Coroutines API (changed from `implementation()` to `api()`) [#1883](https://github.com/JetBrains/compose-multiplatform-core/pull/1883)
- Correctly set `ScrollState.viewportSize` for (Basic)`TextField(TextFieldState)` [#1896](https://github.com/JetBrains/compose-multiplatform-core/pull/1896)
- Changes pressing backspace in a textfield to delete diacritic marks, if any, rather than the entire character [#1869](https://github.com/JetBrains/compose-multiplatform-core/pull/1869)
- Fixed `IllegalArgumentException: maxWidth(value) must be >= than minWidth(0)` crashes when a `TextField` with `trailingIcon` is too narrow [#1936](https://github.com/JetBrains/compose-multiplatform-core/pull/1936)
- Fixed crash when dragging selection in `TextField(TextFieldState)` while also editing the text, such as by pressing Backspace [#1969](https://github.com/JetBrains/compose-multiplatform-core/pull/1969)
- Fix `InterceptPlatformTextInput` for the legacy TextField [#1974](https://github.com/JetBrains/compose-multiplatform-core/pull/1974)

### iOS

- Deprecate `defaultUIKitMain()` [#1585](https://github.com/JetBrains/compose-multiplatform-core/pull/1585)
- Fixed visibility of `androidx.compose.material3.internal.formatWithSkeleton` that was accidently marked as public [#1609](https://github.com/JetBrains/compose-multiplatform-core/pull/1609)
- Fix a bug where the accessibility tree did not reload when VoiceOver was enabled [#1656](https://github.com/JetBrains/compose-multiplatform-core/pull/1656)
- Fix Display Cutout Padding when rotating the device [#1645](https://github.com/JetBrains/compose-multiplatform-core/pull/1645)
- Fixes an interruption while typing characters on a Chinese keyboard [#1692](https://github.com/JetBrains/compose-multiplatform-core/pull/1692)
- Accessibility reading of content when obscured by layers such as pop-ups and dialogs [#1698](https://github.com/JetBrains/compose-multiplatform-core/pull/1698)
- Enables Cupertino Overscroll by default for scrollable components [#1753](https://github.com/JetBrains/compose-multiplatform-core/pull/1753)
- Experimental method `optOutOfCupertinoOverscroll()` removed [#1753](https://github.com/JetBrains/compose-multiplatform-core/pull/1753)
- Fix iOS Accessibility element tree construction within merged nodes [#1750](https://github.com/JetBrains/compose-multiplatform-core/pull/1750)
- Performance issues when iOS screen reader is active fixed [#1780](https://github.com/JetBrains/compose-multiplatform-core/pull/1780)
- Fixed issues where the interactive pop gesture would stop working [#1818](https://github.com/JetBrains/compose-multiplatform-core/pull/1818)
- Fixes an issue where it's not possible to close the `UIMenu` that appears over the Compose content [#1818](https://github.com/JetBrains/compose-multiplatform-core/pull/1818)
- Fix touches tracking for multitouch gestures [#1827](https://github.com/JetBrains/compose-multiplatform-core/pull/1827)
- Fixed a bug where touches could be handled by back gesture and composable content at the same time [#1879](https://github.com/JetBrains/compose-multiplatform-core/pull/1879)
- Removed permissions alert when pasting text into a `TextField` [#1894](https://github.com/JetBrains/compose-multiplatform-core/pull/1894)
- Fix crash when removing popup during scene size change animation [#1878](https://github.com/JetBrains/compose-multiplatform-core/pull/1878)
- Fix accessibility elements rects when `ComposeUIViewController` is shifted [#1899](https://github.com/JetBrains/compose-multiplatform-core/pull/1899)
- Fixed an issue where it wasn't possible to open a popup using pointer input devices [#1906](https://github.com/JetBrains/compose-multiplatform-core/pull/1906)
- Fix popup safe drawing padding when `usePlatformInsets = true` [#1920](https://github.com/JetBrains/compose-multiplatform-core/pull/1920)
- Fix an issue where Compose would retain the old state when its view was reappeared [#1921](https://github.com/JetBrains/compose-multiplatform-core/pull/1921)
- Fix Text Menu opening after Select All action tap [#1930](https://github.com/JetBrains/compose-multiplatform-core/pull/1930)
- Fix freeze where scrolling was cancelled but the overscroll effect was not completed [#1928](https://github.com/JetBrains/compose-multiplatform-core/pull/1928)
- Fix overscroll animation freeze when pull-to-refresh is triggered [#1932](https://github.com/JetBrains/compose-multiplatform-core/pull/1932)
- Fix issue where root compose canvas does not resize without animation [#1934](https://github.com/JetBrains/compose-multiplatform-core/pull/1934)
- Fix issue where dialog layer may not be shown [#1934](https://github.com/JetBrains/compose-multiplatform-core/pull/1934)
- Remove focus on accessibility nodes with clearing semantics [#1933](https://github.com/JetBrains/compose-multiplatform-core/pull/1933)
- Fix adding extra `MetalView` when Compose controller re-enters view hierarchy [#1938](https://github.com/JetBrains/compose-multiplatform-core/pull/1938)
- Fix the first layer appearance freeze [#1948](https://github.com/JetBrains/compose-multiplatform-core/pull/1948)
- Fixed text editing behavior (typing / deleting) in `BasicTextField(TextFieldState)` with applied `OutputTransformation` [#1953](https://github.com/JetBrains/compose-multiplatform-core/pull/1953)
- Fixed incorrect undo behavior for text deletion in `BasicTextField(TextFieldState)` [#1956](https://github.com/JetBrains/compose-multiplatform-core/pull/1956)
- Fix composite input in `BasicTextField(TextFieldState)` [#1984](https://github.com/JetBrains/compose-multiplatform-core/pull/1984)
- Fixes an issue where the accessibility engine could leave a scrollable list without reading it to the end [#1986](https://github.com/JetBrains/compose-multiplatform-core/pull/1986)
- Fix Accessibility navigation through traversal groups in Container mode [#1987](https://github.com/JetBrains/compose-multiplatform-core/pull/1987)
- Fix focus for items within dialogs when full keyboard access is enabled [#1990](https://github.com/JetBrains/compose-multiplatform-core/pull/1990)
- Fix gesture handling for third party interop views [#1993](https://github.com/JetBrains/compose-multiplatform-core/pull/1993)
- Fix the keyboard reappearing after it was dismissed via `LocalSoftwareKeyboardController` in `BasicTextField(TextFieldState)` [#1974](https://github.com/JetBrains/compose-multiplatform-core/pull/1974)
- Fix overscroll touches assertion when back handler is involved [#2017](https://github.com/JetBrains/compose-multiplatform-core/pull/2017)
- Fixed incorrect selection and navigation by arrow keys from a hardware keyboard in `BasicTextField(TextFieldState)` [#2018](https://github.com/JetBrains/compose-multiplatform-core/pull/2018)
- Fix back gesture after modal popup appearance [#2019](https://github.com/JetBrains/compose-multiplatform-core/pull/2019)
- Fixed the behavior of a context menu in the text fields inside modal screens [#2028](https://github.com/JetBrains/compose-multiplatform-core/pull/2028)

### Desktop

- Fix for excessive garbage generation from redrawing on Swing [#1657](https://github.com/JetBrains/compose-multiplatform-core/pull/1657)
- Fix drag-and-drop when the list of supported actions doesn't include `Move` [#1683](https://github.com/JetBrains/compose-multiplatform-core/pull/1683)
- Fix accessibility focus when using `compose.swing.render.on.graphics=true` [#1688](https://github.com/JetBrains/compose-multiplatform-core/pull/1688)
- Fix "Context menu on desktop shows incorrect items after the second showing" [#1693](https://github.com/JetBrains/compose-multiplatform-core/pull/1693)
- Fixed rare crash when using a scrollbar for lazy grid with animated enter/exit items [#1707](https://github.com/JetBrains/compose-multiplatform-core/pull/1707)
- Fix possible exception during `Dialog`/`Popup` creation in case of `compose.layers.type=COMPONENT` and pointing `windowContainer` to `ComposePanel` itself [#1747](https://github.com/JetBrains/compose-multiplatform-core/pull/1747)
- Fix interop initial z-order placement on Windows with `compose.interop.blending` flag (1.7.0 regression) [#1774](https://github.com/JetBrains/compose-multiplatform-core/pull/1774)
- Fixed the background of transparent windows/dialogs on Windows becoming opaque as the window is moved [#1772](https://github.com/JetBrains/compose-multiplatform-core/pull/1772)
- Elements marked with `Modifier.semantics { hideFromAccessibility() }` should now be correctly hidden from a11y [#1798](https://github.com/JetBrains/compose-multiplatform-core/pull/1798)
- Fixed the positioning of the IME candidate popup for `TextField(TextFieldState)` (aka `BasicTextField2`) [#1794](https://github.com/JetBrains/compose-multiplatform-core/pull/1794)
- Fixed input of diacritics via long-press on macOS in `TextField(TextFieldState)` (aka `BasicTextField2`) [#1810](https://github.com/JetBrains/compose-multiplatform-core/pull/1810)
- Radio buttons and any other elements using `Modifier.selectable` with `Role.RadioButton` will have their state reported to accessibility via `AccessibleValue.getCurrentAccessibleValue()` and as `AccessibleState.CHECKED` in `getAccessibleStateSet()` [#1797](https://github.com/JetBrains/compose-multiplatform-core/pull/1797)
- [Windows] Fixed ordering of `SwingPanel`s when using `compose.interop.blending=true` [#1901](https://github.com/JetBrains/compose-multiplatform-core/pull/1901)
- [macOS] Fix, for some cases, the background flashing when closing a window/dialog. This can still happen if an animation is running when the window or dialog is closing [#1911](https://github.com/JetBrains/compose-multiplatform-core/pull/1911)
- Fix `onRenderApiChanged` in `ComposeWindow` and `ComposeDialog` not working when the renderer changes due to fallback, rather than explicit change [#1911](https://github.com/JetBrains/compose-multiplatform-core/pull/1911)
- [Swing Interop] Fixed `compose.interop.blending=true` completely breaking Swing interop on Windows when Direct3D is unsupported. Note that interop blending is still supported on Windows only if Direct3D is available [#1913](https://github.com/JetBrains/compose-multiplatform-core/pull/1913)
- Fixed only the first character being temporarily shown in a `SecureTextField` [#1853](https://github.com/JetBrains/compose-multiplatform-core/pull/1853)
- Changes in `TextFieldState` are now correctly reported to the transformations when inputting composite characters (e.g. ㅀ), instead of the whole text being replaced on each new character [#1853](https://github.com/JetBrains/compose-multiplatform-core/pull/1853)
- Fixed a potential deadlock when calling `Snapshot.sendApplyNotifications` from a thread other than the event dispatching thread [#1955](https://github.com/JetBrains/compose-multiplatform-core/pull/1955)
- Fix missing window shadows on macOS in case of usage `compose.interop.blending` flag [#1908](https://github.com/JetBrains/compose-multiplatform-core/pull/1908)
- Fix `Execution failed for task ':composeApp:proguardReleaseJars'` when `material3` is included in the project [#5261](https://github.com/JetBrains/compose-multiplatform/pull/5261)

### Web

- The `BasicTextField` handles browser copy/cut/paste events correctly now. Previously, they were ignored [#1795](https://github.com/JetBrains/compose-multiplatform-core/pull/1795)
- Mobile browsers: the virtual keyboard is shown when the `TextField` is clicked/receives focus [#1865](https://github.com/JetBrains/compose-multiplatform-core/pull/1865)
- Fix text selection with mouse in `TextField` [#1876](https://github.com/JetBrains/compose-multiplatform-core/pull/1876)
- Fix horizontal scroll when using touchpad and pressing Shift [#1909](https://github.com/JetBrains/compose-multiplatform-core/pull/1909)
- Prevent app from crashing when running in an insecure context where Web Clipboard API is unavailable [#1931](https://github.com/JetBrains/compose-multiplatform-core/pull/1931)
- Replace tab characters with spaces in `Text` and `TextField`s, to avoid them being drawn as tofu [#1943](https://github.com/JetBrains/compose-multiplatform-core/pull/1943)
- Fix touch events processing. Now `Modifier.detectTransformGestures` will allow to handle zoom and rotation gestures [#1942](https://github.com/JetBrains/compose-multiplatform-core/pull/1942)

### Resources

- Read `android:autoMirrored="true"` property and pass it to ImageVector builder [#5140](https://github.com/JetBrains/compose-multiplatform/pull/5140)
- Fix string resource's regex for placeholders to correctly match multi-digit placeholders [#5187](https://github.com/JetBrains/compose-multiplatform/pull/5187)

### Navigation

- Fixed `No destination with ID 0 is on the NavController's back stack` crash on iOS [#1596](https://github.com/JetBrains/compose-multiplatform-core/pull/1596)
- Fix incorrect navigation up on the root screen for non-android targets [#1736](https://github.com/JetBrains/compose-multiplatform-core/pull/1736)
- Fixed browser navigation integration when route paths contain special symbols [#1738](https://github.com/JetBrains/compose-multiplatform-core/pull/1738)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0)
  - [UI 1.8.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0)
  - [Foundation 1.8.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0)
  - [Material 1.8.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-beta01`. Based on [Jetpack Lifecycle 2.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-beta01)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-beta01`. Based on [Jetpack Navigation 2.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0-beta01)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0`. Based on [Jetpack Material3 Adaptive 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0)

---

# 1.8.0-rc01 (April 2025)

_Changes since 1.8.0-beta02_

## Highlights

### Web

-  Improves text input support in Safari on mobile and desktop [#1941](https://github.com/JetBrains/compose-multiplatform-core/pull/1941)
-  Correct certain text input scenarios on Web targets [#1941](https://github.com/JetBrains/compose-multiplatform-core/pull/1941)
New `WebTextInputService` seeks to achieve the following goals:
1. Add support to Safari (which happens to have its own flow of input events that are inconsistent with other browsers)
2. Correct behavior for composition and accent dialogue scenarios
3. Introduce some changes that we will use for improving accessibility

## Breaking Changes

### Multiple Platforms

- A custom implementation for deprecated `LocalTextInputService` is no longer supported [#1974](https://github.com/JetBrains/compose-multiplatform-core/pull/1974)

## Migration Notes

### Multiple Platforms

- material/material3 libraries no longer add a dependency to `material-icons-core` so if your project relied on that, you will have to explicitly add that dependency in your `build.gradle[.kts]` files: [#2025](https://github.com/JetBrains/compose-multiplatform-core/pull/2025), [#2030](https://github.com/JetBrains/compose-multiplatform-core/pull/2030)
```
implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
```

### Gradle Plugin

- The Compose Gradle Plugin requires Kotlin Gradle Plugin 2.+ version now. Old `org.jetbrains.compose.compiler` is not supported anymore and the API to configure it was removed [#5293](https://github.com/JetBrains/compose-multiplatform/pull/5293)

### Lifecycle

- _(prerelease fix)_ Remove deprecated `AbstractSavedStateViewModelFactory` from common code [#1976](https://github.com/JetBrains/compose-multiplatform-core/pull/1976)

## Features

### Desktop

- The default ProGuard version is set to 7.7.0 [#5279](https://github.com/JetBrains/compose-multiplatform/pull/5279)
   - If there are any new errors in the release build, update [the ProGuard rules](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-native-distribution.html#minification-and-obfuscation)
   - A usual workaround is to add `-keep class` for the associated with error class in "Location:"
   - If the error contains `androidx.` package, it might a Compose bug, please report in https://youtrack.jetbrains.com/issues/CMP. The `-keep class` workaround should also work in this case
- _(prerelease fix)_ `./gradlew runRelease` doesn't crash with `java.lang.VerifyError` [#5279](https://github.com/JetBrains/compose-multiplatform/pull/5279)

## Fixes

### Multiple Platforms

- _(prerelease fix)_ `org.jetbrains.compose.material3:material3-adaptive-navigation-suite`, `org.jetbrains.compose.material3:material3-window-size-class` are reverted to the 1.3.1 state. They will be promoted to 1.4 in Compose Multiplatform 1.9 with material3 [#1995](https://github.com/JetBrains/compose-multiplatform-core/pull/1995)
- Fix `InterceptPlatformTextInput` for the legacy TextField [#1974](https://github.com/JetBrains/compose-multiplatform-core/pull/1974)

### iOS

- _(prerelease fix)_ Fix Text Menu placement for `TextField(TextFieldState)` [#1972](https://github.com/JetBrains/compose-multiplatform-core/pull/1972)
- _(prerelease fix)_ Fix Full Keyboard Access focus when Compose is located inside SwiftUI view [#1975](https://github.com/JetBrains/compose-multiplatform-core/pull/1975)
- _(prerelease fix)_ Fix an issue where traversal group nodes with test tag are missing in the accessibility tree [#1977](https://github.com/JetBrains/compose-multiplatform-core/pull/1977)
- Fix composite input in `BasicTextField(TextFieldState)` [#1984](https://github.com/JetBrains/compose-multiplatform-core/pull/1984)
- Fixes an issue where the accessibility engine could leave a scrollable list without reading it to the end [#1986](https://github.com/JetBrains/compose-multiplatform-core/pull/1986)
- Fix Accessibility navigation through traversal groups in Container mode [#1987](https://github.com/JetBrains/compose-multiplatform-core/pull/1987)
- Fix focus for items within dialogs when full keyboard access is enabled [#1990](https://github.com/JetBrains/compose-multiplatform-core/pull/1990)
- _(prerelease fix)_ Fixed a white scene background on iOS when a dark theme is activated [#1989](https://github.com/JetBrains/compose-multiplatform-core/pull/1989)
- Fix gesture handling for third party interop views [#1993](https://github.com/JetBrains/compose-multiplatform-core/pull/1993)
- Fix overscroll touches assertion when back handler is involved [#2017](https://github.com/JetBrains/compose-multiplatform-core/pull/2017)
- Fixed incorrect selection and navigation by arrow keys from a hardware keyboard in `BasicTextField(TextFieldState)` [#2018](https://github.com/JetBrains/compose-multiplatform-core/pull/2018)
- Fix back gesture after modal popup appearance [#2019](https://github.com/JetBrains/compose-multiplatform-core/pull/2019)
- Fixed the behavior of a context menu in the text fields inside modal screens [#2028](https://github.com/JetBrains/compose-multiplatform-core/pull/2028)
- _(prerelease fix)_ Change runtime experimental annotations to proper "ui" experimental annotations for a few fields inside `ComposeUIViewControllerConfiguration` [#2034](https://github.com/JetBrains/compose-multiplatform-core/pull/2034)
- _(prerelease fix)_ Fix back gesture handling after modal view controller dismissal [#2048](https://github.com/JetBrains/compose-multiplatform-core/pull/2048)
- _(prerelease fix)_ Fix overscroll when RTL is enabled [#2054](https://github.com/JetBrains/compose-multiplatform-core/pull/2054)

### Desktop

- _(prerelease fix)_ [macOS] Fixed accented character input via long press [#1980](https://github.com/JetBrains/compose-multiplatform-core/pull/1980)

### Navigation

- _(prerelease fix)_ Fix custom navigation animation in nested graphs in non-android targets [#1982](https://github.com/JetBrains/compose-multiplatform-core/pull/1982)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0-rc01`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0-rc03](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0-rc03)
  - [UI 1.8.0-rc03](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0-rc03)
  - [Foundation 1.8.0-rc03](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0-rc03)
  - [Material 1.8.0-rc03](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0-rc03)
  - [Material3 1.3.2](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.2)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-alpha07`. Based on [Jetpack Lifecycle 2.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-beta01)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-alpha17`. Based on [Jetpack Navigation 2.9.0-beta01](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0-beta01)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0-rc01`. Based on [Jetpack Material3 Adaptive 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0)

---

# 1.8.0-beta02 (April 2025)

_Changes since 1.8.0-beta01_

## Features

### iOS

- Support text input when Full Keyboard Access is enabled [#1917](https://github.com/JetBrains/compose-multiplatform-core/pull/1917)
- Added native behavior for tap, long tap and multiple tap to `BasicTextField(TextFieldState)` [#1923](https://github.com/JetBrains/compose-multiplatform-core/pull/1923)
- Magnifier support for `BasicTextField(TextFieldState)` [#1926](https://github.com/JetBrains/compose-multiplatform-core/pull/1926)

## Fixes

### Multiple Platforms

- Fixed `IllegalArgumentException: maxWidth(value) must be >= than minWidth(0)` crashes when a `TextField` with `trailingIcon` is too narrow [#1936](https://github.com/JetBrains/compose-multiplatform-core/pull/1936)
- Fixed crash when dragging selection in `TextField(TextFieldState)` while also editing the text, such as by pressing Backspace [#1969](https://github.com/JetBrains/compose-multiplatform-core/pull/1969)

### iOS

- Fix Text Menu opening after Select All action tap [#1930](https://github.com/JetBrains/compose-multiplatform-core/pull/1930)
- Fix freeze where scrolling was cancelled but the overscroll effect was not completed [#1928](https://github.com/JetBrains/compose-multiplatform-core/pull/1928)
- Fix overscroll animation freeze when pull-to-refresh is triggered [#1932](https://github.com/JetBrains/compose-multiplatform-core/pull/1932)
- Fix issue where root compose canvas does not resize without animation [#1934](https://github.com/JetBrains/compose-multiplatform-core/pull/1934)
- Fix issue where dialog layer may not be shown [#1934](https://github.com/JetBrains/compose-multiplatform-core/pull/1934)
- Remove focus on accessibility nodes with clearing semantics [#1933](https://github.com/JetBrains/compose-multiplatform-core/pull/1933)
- Fix adding extra `MetalView` when Compose controller re-enters view hierarchy [#1938](https://github.com/JetBrains/compose-multiplatform-core/pull/1938)
- _(prerelease fix)_ Fix deletion of certain RTL or diacritics characters [#1952](https://github.com/JetBrains/compose-multiplatform-core/pull/1952)
- Fix the first layer appearance freeze [#1948](https://github.com/JetBrains/compose-multiplatform-core/pull/1948)
- Fixed text editing behavior (typing / deleting) in `BasicTextField(TextFieldState)` with applied `OutputTransformation` [#1953](https://github.com/JetBrains/compose-multiplatform-core/pull/1953)
- Fixed incorrect undo behavior for text deletion in `BasicTextField(TextFieldState)` [#1956](https://github.com/JetBrains/compose-multiplatform-core/pull/1956)
- _(prerelease fix)_ Fix Text Menu popup placement [#1971](https://github.com/JetBrains/compose-multiplatform-core/pull/1971)

### Desktop

- Fixed only the first character being temporarily shown in a `SecureTextField` [#1853](https://github.com/JetBrains/compose-multiplatform-core/pull/1853)
- Changes in `TextFieldState` are now correctly reported to the transformations when inputting composite characters (e.g. ㅀ), instead of the whole text being replaced on each new character [#1853](https://github.com/JetBrains/compose-multiplatform-core/pull/1853)
- Fixed a potential deadlock when calling `Snapshot.sendApplyNotifications` from a thread other than the event dispatching thread [#1955](https://github.com/JetBrains/compose-multiplatform-core/pull/1955)
- Fix missing window shadows on macOS in case of usage `compose.interop.blending` flag [#1908](https://github.com/JetBrains/compose-multiplatform-core/pull/1908)
- Fix `Execution failed for task ':composeApp:proguardReleaseJars'` when `material3` is included in the project [#5261](https://github.com/JetBrains/compose-multiplatform/pull/5261)

### Web

- Fix horizontal scroll when using touchpad and pressing Shift [#1909](https://github.com/JetBrains/compose-multiplatform-core/pull/1909)
- Prevent app from crashing when running in an insecure context where Web Clipboard API is unavailable [#1931](https://github.com/JetBrains/compose-multiplatform-core/pull/1931)
- Replace tab characters with spaces in `Text` and `TextField`s, to avoid them being drawn as tofu [#1943](https://github.com/JetBrains/compose-multiplatform-core/pull/1943)
- Fix touch events processing. Now `Modifier.detectTransformGestures` will allow to handle zoom and rotation gestures [#1942](https://github.com/JetBrains/compose-multiplatform-core/pull/1942)

### Navigation

- _(prerelease fix)_ Fixed navigation on JS targets [#1939](https://github.com/JetBrains/compose-multiplatform-core/pull/1939)
- _(prerelease fix)_ Encode only parameters in web navigation routes to have nicer UX [#1940](https://github.com/JetBrains/compose-multiplatform-core/pull/1940)
- _(prerelease fix)_ Add a flag to disable iOS back gesture detection [#1951](https://github.com/JetBrains/compose-multiplatform-core/pull/1951)
- _(prerelease fix)_ Fixed default pop `NavHost` animations if enter/exit animations are customized only [#1963](https://github.com/JetBrains/compose-multiplatform-core/pull/1963)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0-beta02`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0-rc02](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0-rc02)
  - [UI 1.8.0-rc02](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0-rc02)
  - [Foundation 1.8.0-rc02](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0-rc02)
  - [Material 1.8.0-rc02](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0-rc02)
  - [Material3 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-alpha06`. Based on [Jetpack Lifecycle 2.9.0-alpha12](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-alpha12)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-alpha16`. Based on [Jetpack Navigation 2.9.0-alpha08](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0-alpha08)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0-beta02`. Based on [Jetpack Material3 Adaptive 1.1.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0)

---

# 1.8.0-beta01 (March 2025)

_Changes since 1.8.0-alpha04_

## Breaking Changes

### Multiple Platforms

- Add `@Composable` annotations to the actual `WindowInsets.Companion.*` extensions to match the expected declarations [#1919](https://github.com/JetBrains/compose-multiplatform-core/pull/1919), [#1720](https://github.com/JetBrains/compose-multiplatform-core/pull/1720)

## Features

### iOS

- Add ability to recreate Composable after `ComposeUIViewController` leaves view controller hierarchy [#1877](https://github.com/JetBrains/compose-multiplatform-core/pull/1877)
- Support trackpad pointer input [#1893](https://github.com/JetBrains/compose-multiplatform-core/pull/1893)
- Save Composable state when view controller leaves view controller hierarchy [#1904](https://github.com/JetBrains/compose-multiplatform-core/pull/1904)

## Fixes

### iOS

- Removed permissions alert when pasting text into a `TextField` [#1894](https://github.com/JetBrains/compose-multiplatform-core/pull/1894)
- Fix crash when removing popup during scene size change animation [#1878](https://github.com/JetBrains/compose-multiplatform-core/pull/1878)
- Fix accessibility elements rects when `ComposeUIViewController` is shifted [#1899](https://github.com/JetBrains/compose-multiplatform-core/pull/1899)
- Fixed an issue where it wasn't possible to open a popup using pointer input devices [#1906](https://github.com/JetBrains/compose-multiplatform-core/pull/1906)
- Fix popup safe drawing padding when `usePlatformInsets = true` [#1920](https://github.com/JetBrains/compose-multiplatform-core/pull/1920)
- Fix an issue where Compose would retain the old state when its view was reappeared [#1921](https://github.com/JetBrains/compose-multiplatform-core/pull/1921)
-  _(prerelease fix)_ Fix non-interactive UI after interop view tap [#1925](https://github.com/JetBrains/compose-multiplatform-core/pull/1925)

### Desktop

- [Windows] Fixed ordering of `SwingPanel`s when using `compose.interop.blending=true` [#1901](https://github.com/JetBrains/compose-multiplatform-core/pull/1901)
- [macOS] Fix, for some cases, the background flashing when closing a window/dialog. This can still happen if an animation is running when the window or dialog is closing [#1911](https://github.com/JetBrains/compose-multiplatform-core/pull/1911)
- Fix `onRenderApiChanged` in `ComposeWindow` and `ComposeDialog` not working when the renderer changes due to fallback, rather than explicit change [#1911](https://github.com/JetBrains/compose-multiplatform-core/pull/1911)
- [Swing Interop] Fixed `compose.interop.blending=true` completely breaking Swing interop on Windows when Direct3D is unsupported. Note that interop blending is still supported on Windows only if Direct3D is available [#1913](https://github.com/JetBrains/compose-multiplatform-core/pull/1913)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0-beta01`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0-beta02)
  - [UI 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0-beta02)
  - [Foundation 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0-beta02)
  - [Material 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0-beta02)
  - [Material3 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-alpha05`. Based on [Jetpack Lifecycle 2.9.0-alpha12](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-alpha12)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-alpha15`. Based on [Jetpack Navigation 2.9.0-alpha08](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0-alpha08)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0-beta01`. Based on [Jetpack Material3 Adaptive 1.1.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0-beta01)

---

# 1.8.0-alpha04 (March 2025)

_Changes since 1.8.0-alpha03_

## Highlights

### Desktop

- Improved performance on Windows by replacing the native code compiler to Clang. See details in https://github.com/JetBrains/skiko/pull/1020#issuecomment-2649433785 [#1863](https://github.com/JetBrains/compose-multiplatform-core/pull/1863)
  - DirectX rendering FPS is **17 % higher**
  - Software rendering FPS is **73 % higher**
  - Skiko binary size is reduced from **16.7 to 12.1 Mb on x64** and from **17.4 to 10.5 Mb on arm64**
  - Note that the numbers are measured on one machine and also can differ between Compose 1.7 and Compose 1.8 (will be updated in the stable Release Notes)

## Known Issues

### iOS

- Back gesture may remain stuck in the middle, the fix will be available in the next pre-release version [#1861](https://github.com/JetBrains/compose-multiplatform-core/pull/1861)

## Breaking Changes

### Multiple Platforms

- [Google Maven](https://maven.google.com/) now contains some artifacts for all Kotlin targets including Wasm and JS. Compose Multiplatform now depends on those artifacts and user projects might need to add `google()` repo to `repositories {...}` block if it is not there yet [#1819](https://github.com/JetBrains/compose-multiplatform-core/pull/1819)
- Multiplatform lifecycle was migrated from a internal `core-bundle` module to the androidx SavedState. Libraries that use `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate`  or `org.jetbrains.androidx.savedstate:savedstate` should migrate to the latest version [#1850](https://github.com/JetBrains/compose-multiplatform-core/pull/1850)
- _(prerelease fix)_ Material3 is reverted from Jetpack Compose Material3 `1.4.0-alpha07` to `1.3.1` because 1.4.0 Stable will be released only after Compose Multiplatform 1.8.0. As a workaround to continue using the new API, replace:
  ```
  implementation(compose.material3)
  ```
  by:
  ```
  implementation("org.jetbrains.compose.material3:material3:1.8.0-alpha03")
  ```
  Jetpack Compose Material3 1.4.0 will be merged again sometime after the Compose Multiplatform 1.8.0 release [#1868](https://github.com/JetBrains/compose-multiplatform-core/pull/1868)

## Features

### iOS

- Support new haptic feedback types [#1831](https://github.com/JetBrains/compose-multiplatform-core/pull/1831)
- Support for focusable nodes when Full Keyboard Access is enabled on iOS [#1825](https://github.com/JetBrains/compose-multiplatform-core/pull/1825)
- Floating cursor support for `BasicTextField(TextFieldState)` [#1598](https://github.com/JetBrains/compose-multiplatform-core/pull/1598)
- Add support for Bold Text accessibility setting [#1846](https://github.com/JetBrains/compose-multiplatform-core/pull/1846)
- Bhojpuri language support for VoiceOver [#1838](https://github.com/JetBrains/compose-multiplatform-core/pull/1838)
- Add support for Reduce Motion accessibility setting [#1847](https://github.com/JetBrains/compose-multiplatform-core/pull/1847)
- Default `androidx.navigation` transition animation on iOS is as close as possible to the iOS back gesture [#1861](https://github.com/JetBrains/compose-multiplatform-core/pull/1861)
- Support accessibility text input [#1875](https://github.com/JetBrains/compose-multiplatform-core/pull/1875)
- Support text input for UI Tests [#1875](https://github.com/JetBrains/compose-multiplatform-core/pull/1875)
- Accessibility: added ability to traverse nodes within a scrollable container [#1837](https://github.com/JetBrains/compose-multiplatform-core/pull/1837)

## Fixes

### Multiple Platforms

- Fixed the output of `TextField(TextFieldState)` (aka `BasicTextField2`) transformations incorrectly leeching into the untransformed text itself, when input method is used (Chinese and other languages with multi-keystroke character input) [#1833](https://github.com/JetBrains/compose-multiplatform-core/pull/1833)
- Fix possible `IllegalArgumentException: Object already closed` during `GraphicsLayer.record` [#1839](https://github.com/JetBrains/compose-multiplatform-core/pull/1839)
- _(prerelease fix)_ Change an `Esc` button interception from `onPreviewKeyEvent` to `onKeyEvent` in the desktop `BackGestureDispatcher` implementation. It fixes problems when user's code handles the `Esc`-button [#1860](https://github.com/JetBrains/compose-multiplatform-core/pull/1860)
- _(prerelease fix)_ Fix a problem when an `onBack` callback is updated but listener is not [#1860](https://github.com/JetBrains/compose-multiplatform-core/pull/1860)
- _(prerelease fix)_ Fix memory leak in some cases of re-usage internal layout nodes [#1873](https://github.com/JetBrains/compose-multiplatform-core/pull/1873)
- Compose runtime module now exposes its dependency on Kotlin Coroutines API (changed from `implementation()` to `api()`) [#1883](https://github.com/JetBrains/compose-multiplatform-core/pull/1883)
- Correctly set `ScrollState.viewportSize` for (Basic)`TextField(TextFieldState)` [#1896](https://github.com/JetBrains/compose-multiplatform-core/pull/1896)

### iOS

- _(prerelease fix)_ Fixed issue where cross-directional scrolling could intercept and cancel each other [#1851](https://github.com/JetBrains/compose-multiplatform-core/pull/1851)
- Fixed a bug where touches could be handled by back gesture and composable content at the same time [#1879](https://github.com/JetBrains/compose-multiplatform-core/pull/1879)

### Desktop

- _(prerelease fix)_ Restore compatibility with Ubuntu 20.04 [#1870](https://github.com/JetBrains/compose-multiplatform-core/pull/1870)
- _(prerelease fix)_ Fix "Could not resolve version conflict" in non-Gradle projects [#1872](https://github.com/JetBrains/compose-multiplatform-core/pull/1872)

### Web

- Mobile browsers: the virtual keyboard is shown when the TextField is clicked/focues [#1865](https://github.com/JetBrains/compose-multiplatform-core/pull/1865)
- Fix text selection with mouse in TextField [#1876](https://github.com/JetBrains/compose-multiplatform-core/pull/1876)

### Navigation

- _(prerelease fix)_ Fix a desktop back navigation when Esc button clicked [#1890](https://github.com/JetBrains/compose-multiplatform-core/pull/1890)
- _(prerelease fix)_ Fix an iOS back navigation after a swipe on disallowed edge [#1890](https://github.com/JetBrains/compose-multiplatform-core/pull/1890)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0-alpha04`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0-beta02)
  - [UI 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0-beta02)
  - [Foundation 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0-beta02)
  - [Material 1.8.0-beta02](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0-beta02)
  - [Material3 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-alpha04`. Based on [Jetpack Lifecycle 2.9.0-alpha08](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-alpha08)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.9.0-alpha14`. Based on [Jetpack Navigation 2.9.0-alpha07](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.0-alpha07)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0-alpha04`. Based on [Jetpack Material3 Adaptive 1.1.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0-beta01)

---

# 1.8.0-alpha03 (February 2025)

_Changes since 1.8.0-alpha02_

## Highlights

### Multiple Platforms

- [Compose Multiplatform codebase is fully migrated to K2](https://github.com/JetBrains/compose-multiplatform-core/pull/1778). Please note that native and web klibs can be consumed only with Kotlin 2.1.0 or newer. Also, due to underlying changes in the compiler plugin, it's better to recompile libraries against the new version. Please let us know if you find any compatibility issues during this migration
- [Implement multiplatform `BackHandler` and `PredictiveBackHandler`](https://github.com/JetBrains/compose-multiplatform-core/pull/1771). And use them in material3 widgets and androidx-navigation library

## Breaking Changes

### Multiple Platforms

- [IdlingResource](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/IdlingResource) interface was moved from commonMain to android and desktop source sets. The related experimental methods of [ComposeUiTest](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/ComposeUiTest) were moved too. [They are not available for Web and iOS anymore](https://github.com/JetBrains/compose-multiplatform-core/pull/1822). Consider using [waitUntil function](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/ComposeUiTest#waitUntil(kotlin.String,kotlin.Long,kotlin.Function0)) as an alternative. Note: it's a breaking change only for Web and iOS, but not for Desktop and Android

### iOS

- [Experimental classes `CupertinoScrollDecayAnimationSpec` and `CupertinoOverscrollEffect` are removed from public API](https://github.com/JetBrains/compose-multiplatform-core/pull/1806)

## Features

### Multiple Platforms

- [Update skia to m132](https://github.com/JetBrains/compose-multiplatform-core/pull/1823)
- [Adopt a new `Clipboard` interface with suspend functions, which work correctly on all targets including Web](https://github.com/JetBrains/compose-multiplatform-core/pull/1796). The `ClipboardManager` was deprecated because it was not possible to correctly implement it for Web

### iOS

- [Support VoiceControl on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1780)
- [`AccessibilitySyncOptions` removed](https://github.com/JetBrains/compose-multiplatform-core/pull/1780). The accessibility tree is built on demand
- [Calculate the order and location of semantic elements in the same way as it's done on Android](https://github.com/JetBrains/compose-multiplatform-core/pull/1809)
- [Support `UIAccessibilityContainerTypeSemanticGroup` for traversal groups](https://github.com/JetBrains/compose-multiplatform-core/pull/1809)
- [Compose works correctly with nested `UIScrollView`s, as well as within `UIScrollView`s](https://github.com/JetBrains/compose-multiplatform-core/pull/1818)
- [Added the ability to close modal Compose view controllers](https://github.com/JetBrains/compose-multiplatform-core/pull/1818) (with non-scrollable content on them) with a swipe gesture

## Fixes


### Multiple Platforms

- [Fix light source position for shadow rendering to match the Material specification and Android behavior](https://github.com/JetBrains/compose-multiplatform-core/pull/1784)
- [Fix inconsistency between Android and Desktop in `placeWithLayer` scale application: offset is not scaled now](https://github.com/JetBrains/compose-multiplatform-core/pull/1784) (Android behavior)
- [Clear the canvas before rendering each frame in tests, to avoid drawing different frames on top of each other, resulting in incorrect images being returned by `captureToImage`](https://github.com/JetBrains/compose-multiplatform-core/pull/1820)

### iOS

- [Performance issues when iOS screen reader is active fixed](https://github.com/JetBrains/compose-multiplatform-core/pull/1780)
- [Fixed issues where the interactive pop gesture would stop working](https://github.com/JetBrains/compose-multiplatform-core/pull/1818)
- [Fixes an issue where it's not possible to close the `UIMenu` that appears over the Compose content](https://github.com/JetBrains/compose-multiplatform-core/pull/1818)
- [Fix touches tracking for multitouch gestures](https://github.com/JetBrains/compose-multiplatform-core/pull/1827)

### Desktop

- [Elements marked with `Modifier.semantics { hideFromAccessibility() }` should now be correctly hidden from a11y](https://github.com/JetBrains/compose-multiplatform-core/pull/1798)
- [Fixed the positioning of the IME candidate popup for `TextField(TextFieldState)`](https://github.com/JetBrains/compose-multiplatform-core/pull/1794) (aka `BasicTextField2`)
- [Fixed input of diacritics via long-press on macOS in `TextField(TextFieldState)`](https://github.com/JetBrains/compose-multiplatform-core/pull/1810) (aka `BasicTextField2`)
- [Radio buttons and any other elements using `Modifier.selectable` with `Role.RadioButton` will have their state reported to accessibility via `AccessibleValue.getCurrentAccessibleValue()` and as `AccessibleState.CHECKED` in `getAccessibleStateSet()`](https://github.com/JetBrains/compose-multiplatform-core/pull/1797)

### Web

- [The `BasicTextField` handles browser copy/cut/paste events correctly now](https://github.com/JetBrains/compose-multiplatform-core/pull/1795). Previously, they were ignored

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0-alpha03`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0-beta01)
  - [UI 1.8.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0-beta01)
  - [Foundation 1.8.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0-beta01)
  - [Material 1.8.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0-beta01)
  - [Material3 1.4.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha07)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-alpha03`. Based on [Jetpack Lifecycle 2.9.0-alpha03](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-alpha03)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha13`. Based on [Jetpack Navigation 2.8.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.4)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0-alpha03`. Based on [Jetpack Material3 Adaptive 1.1.0-beta01](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0-beta01)

---

# 1.8.0-alpha02 (January 2025)

_Changes since 1.8.0-alpha01_

## Highlights

### Resources

- [Add `FontVariation.Settings` support to the resources library](https://github.com/JetBrains/compose-multiplatform/pull/5183)

## Features

### iOS

- [Accessibility navigation uses safe area to calculate when focused rect is out of bounds](https://github.com/JetBrains/compose-multiplatform-core/pull/1745)
- [Initial Drag&Drop support](https://github.com/JetBrains/compose-multiplatform-core/pull/1690)
- [Align Compose components semantics with UIKit views accessibility](https://github.com/JetBrains/compose-multiplatform-core/pull/1719)

### Web

- [Adds experimental `PointerIcon.fromKeyword` function to change the browser cursor](https://github.com/JetBrains/compose-multiplatform-core/pull/1704)

## Fixes

### Multiple Platforms

- [Fix missing implementation on non-Android platforms for `ModalDrawerSheet` overload with `DrawerState` argument](https://github.com/JetBrains/compose-multiplatform-core/pull/1763)

### iOS

- [Fixes an interruption while typing characters on a Chinese keyboard](https://github.com/JetBrains/compose-multiplatform-core/pull/1692)
- [Accessibility reading of content when obscured by layers such as pop-ups and dialogs](https://github.com/JetBrains/compose-multiplatform-core/pull/1698)
- [Taps should be properly registered on interop views with `UIKitInteropInteractionMode.Cooperative` interaction mode](https://github.com/JetBrains/compose-multiplatform-core/pull/1695)
- [Interactive pop on `UINavigationController` should recognize correctly](https://github.com/JetBrains/compose-multiplatform-core/pull/1695)
- [Enables Cupertino overscroll by default for scrollable components](https://github.com/JetBrains/compose-multiplatform-core/pull/1753)
- [Experimental method`optOutOfCupertinoOverscroll()` removed](https://github.com/JetBrains/compose-multiplatform-core/pull/1753)
- [Fix iOS Accessibility element tree construction within merged nodes](https://github.com/JetBrains/compose-multiplatform-core/pull/1750)

### Desktop

- [Fixed rare crash when using a scrollbar for lazy grid with animated enter/exit items](https://github.com/JetBrains/compose-multiplatform-core/pull/1707)
- [Fix possible exception during `Dialog`/`Popup` creation in case of `compose.layers.type=COMPONENT` and pointing `windowContainer` to `ComposePanel` itself](https://github.com/JetBrains/compose-multiplatform-core/pull/1747)
- [Fix interop initial z-order placement on Windows with `compose.interop.blending` flag](https://github.com/JetBrains/compose-multiplatform-core/pull/1774) (1.7.0 regression)
- [Fixed the background of transparent windows/dialogs on Windows becoming opaque as the window is moved](https://github.com/JetBrains/compose-multiplatform-core/pull/1772)

### Resources

- [Fix string resource's regex for placeholders to correctly match multi-digit placeholders](https://github.com/JetBrains/compose-multiplatform/pull/5187)

### Navigation

- [Fix incorrect navigation up on the root screen for non-android targets](https://github.com/JetBrains/compose-multiplatform-core/pull/1736)
- [Fixed browser navigation integration when route paths contain special symbols](https://github.com/JetBrains/compose-multiplatform-core/pull/1738)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0-alpha02`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0-alpha07)
  - [UI 1.8.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0-alpha07)
  - [Foundation 1.8.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0-alpha07)
  - [Material 1.8.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0-alpha07)
  - [Material3 1.4.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha04)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-alpha02`. Based on [Jetpack Lifecycle 2.9.0-alpha03](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-alpha03)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha12`. Based on [Jetpack Navigation 2.8.4](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.4)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0-alpha02`. Based on [Jetpack Material3 Adaptive 1.1.0-alpha07](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0-alpha07)

---

# 1.7.3 (December 2024)

_Changes since 1.7.1_

## Features

### Desktop

- [Compose plugin for IntelliJ IDEA now supports K2 mode](https://github.com/JetBrains/compose-multiplatform/pull/5138)

## Fixes

### iOS

- [Taps should be properly registered on interop views with `UIKitInteropInteractionMode.Cooperative` interaction mode](https://github.com/JetBrains/compose-multiplatform-core/pull/1731)
- [Interactive pop](https://github.com/JetBrains/compose-multiplatform-core/pull/1731) (swipe to go back) on `UINavigationController` should recognize correctly

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.7.3`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.6](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.6)
  - [UI 1.7.6](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.6)
  - [Foundation 1.7.6](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.6)
  - [Material 1.7.6](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.6)
  - [Material3 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.4`. Based on [Jetpack Lifecycle 2.8.5](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.5)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha11`. Based on [Jetpack Navigation 2.8.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.0.1`. Based on [Jetpack Material3 Adaptive 1.0.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0)

---

# 1.8.0-alpha01 (November 2024)

_Changes since 1.7.1_

## Highlights

### iOS

- [LocalUIViewController moved to the `androidx.compose.ui.uikit` module](https://github.com/JetBrains/compose-multiplatform-core/pull/1608)
- [Update `AccessibilitySyncOptions` and remove `AccessibilityDebugLogger` from public API](https://github.com/JetBrains/compose-multiplatform-core/pull/1604)
- [ComposeUIViewControllerDelegate marked as deprecated](https://github.com/JetBrains/compose-multiplatform-core/pull/1651). Use parent view controller to override the methods of the UIViewController class
- [Remove experimental flag from `fun enableTraceOSLog()`](https://github.com/JetBrains/compose-multiplatform-core/pull/1652)
- [Remove obsolete Canvas Layers mode on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1680)

## Breaking Changes

### Multiple Platforms

- [Tests that relied on `waitForIdle`, `awaitIdle` or `runOnIdle`](https://github.com/JetBrains/compose-multiplatform-core/pull/1550) (whether explicit or implicit) executing `delay`-ed coroutines will no longer work correctly. To fix this advance the test time via `mainClock` manually, as needed
    For example, tests that previously did something like:
    ```
    var updateText by mutableStateOf(false)
    var text by mutableStateOf("0")
    setContent {
        LaunchedEffect(updateText) {
            if (updateText) {
                delay(1000)
                text = "1"
            }
        }
    }
    updateText = true
    waitForIdle()
    assertEquals("1", text)
    ```
    will need to add `mainClock.advanceTimeBy(1000)` after `waitForIdle()`, because `waitForIdle` no longer waits for the `delay`-ed coroutine to complete
- [Tests that advance the test clock](https://github.com/JetBrains/compose-multiplatform-core/pull/1550) (via `mainClock.advanceTimeBy`) may see different behavior with regards to the amount and timing of recomposition, layout, drawing and effects
- [`runOnIdle` will now execute `action` on the UI thread](https://github.com/JetBrains/compose-multiplatform-core/pull/1601)
- [`runOnIdle` will no longer call `waitForIdle` after executing the action](https://github.com/JetBrains/compose-multiplatform-core/pull/1601)
- [Advancing `mainClock` such that it doesn't reach the next frame, will no longer cause a recomposition](https://github.com/JetBrains/compose-multiplatform-core/pull/1618)

### Desktop

- [Deprecated/experimental `Modifier.onExternalDrag` has been removed - common `Modifier.dragAndDropTarget` API should be used instead](https://github.com/JetBrains/compose-multiplatform-core/pull/1606)

## Features

### Multiple Platforms

- [Support configurable vertical text centering via `LineHeightStyle.Alignment`](https://github.com/JetBrains/compose-multiplatform-core/pull/1569)
- [Support Variable Fonts In All Platforms](https://github.com/JetBrains/compose-multiplatform-core/pull/1623)

### iOS

- [Add localised string for VoiceOver accessibility support](https://github.com/JetBrains/compose-multiplatform-core/pull/1441)
- [Support state announcements for scrollable lists in VoiceOver](https://github.com/JetBrains/compose-multiplatform-core/pull/1644)
- [Support for accessibility gestures for left-to-right languages](https://github.com/JetBrains/compose-multiplatform-core/pull/1663)

### Desktop

- [Compose plugin for IntelliJ IDEA now supports K2 mode](https://github.com/JetBrains/compose-multiplatform/pull/5138)

### Resources

- [Add new API to preload and cache font and image resources on web targets: `preloadFont`, `preloadImageBitmap`, `preloadImageVector`](https://github.com/JetBrains/compose-multiplatform/pull/5159)

### Gradle Plugin

- [Support compose resources in `androidLibrary` target](https://github.com/JetBrains/compose-multiplatform/pull/5157)

### Navigation

- [Basic support a navigation by deep links](https://github.com/JetBrains/compose-multiplatform-core/pull/1610)
- [Commonize `navController.navigate(Uri)` method](https://github.com/JetBrains/compose-multiplatform-core/pull/1617)
- [Implemented non-android `navController.handleDeepLink(NavDeepLinkRequest)` method](https://github.com/JetBrains/compose-multiplatform-core/pull/1617)
- [New API to configure browser navigation buttons and the address line](https://github.com/JetBrains/compose-multiplatform-core/pull/1621)
- [Navigation via a browser address field](https://github.com/JetBrains/compose-multiplatform-core/pull/1640)

## Fixes

### Multiple Platforms

- [`waitForIdle`, `awaitIdle` and `runOnIdle` no longer consider Compose to be non-idle when coroutines launched in a composition scope call `delay`](https://github.com/JetBrains/compose-multiplatform-core/pull/1550). This prevents tests with an infinite loop with `delay` in a `LaunchedEffect` from hanging
- [Tests that advance the test clock](https://github.com/JetBrains/compose-multiplatform-core/pull/1550) (via `mainClock.advanceTimeBy`) will now correctly (re)compose/layout/draw/effects each virtual frame as needed
- [`runOnIdle` will now execute `action` on the UI thread, as Android behaves](https://github.com/JetBrains/compose-multiplatform-core/pull/1601)
- [`runOnIdle` will no longer call `waitForIdle` after executing the action, as Android behaves](https://github.com/JetBrains/compose-multiplatform-core/pull/1601)
- [The overhead for running an empty test has been significantly reduced](https://github.com/JetBrains/compose-multiplatform-core/pull/1615)

### iOS

- [Deprecate defaultUIKitMain()](https://github.com/JetBrains/compose-multiplatform-core/pull/1585)
- [Fixed visibility of `androidx.compose.material3.internal.formatWithSkeleton` that was accidently marked as public](https://github.com/JetBrains/compose-multiplatform-core/pull/1609)
- [Fix a bug where the accessibility tree did not reload when VoiceOver was enabled](https://github.com/JetBrains/compose-multiplatform-core/pull/1656)
- [Fix Display Cutout Padding when rotating the device](https://github.com/JetBrains/compose-multiplatform-core/pull/1645)

### Desktop

- [Fix drag-and-drop when the list of supported actions doesn't include `Move`](https://github.com/JetBrains/compose-multiplatform-core/pull/1683)
- [Fix accessibility focus when using `compose.swing.render.on.graphics=true`](https://github.com/JetBrains/compose-multiplatform-core/pull/1688)
- [Fix "Context menu on desktop shows incorrect items after the second showing"](https://github.com/JetBrains/compose-multiplatform-core/pull/1693)

### Resources

- [Read `android:autoMirrored="true"` property and pass it to ImageVector builder](https://github.com/JetBrains/compose-multiplatform/pull/5140)

### Navigation

- [Fixed `No destination with ID 0 is on the NavController's back stack` crash on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1596)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.8.0-alpha01`. Based on Jetpack Compose libraries:
  - [Runtime 1.8.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.8.0-alpha03)
  - [UI 1.8.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0-alpha03)
  - [Foundation 1.8.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.8.0-alpha03)
  - [Material 1.8.0-alpha03](https://developer.android.com/jetpack/androidx/releases/compose-material#1.8.0-alpha03)
  - [Material3 1.4.0-alpha01](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha01)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.0-alpha01`. Based on [Jetpack Lifecycle 2.9.0-alpha03](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.0-alpha03)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha11`. Based on [Jetpack Navigation 2.8.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.1.0-alpha01`. Based on [Jetpack Material3 Adaptive 1.1.0-alpha04](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.1.0-alpha04)

---

# 1.7.1 (November 2024)

_Changes since 1.7.0_

## Fixes

### Multiple Platforms

- [Fixed `Modifier.clickable` binary compatibility with 1.6 on non-JVM targets](https://github.com/JetBrains/compose-multiplatform-core/pull/1647)
- [Fixed `Modifier.toggleable` and `Modifier.selectable` binary compatibility with 1.6 on non-JVM targets](https://github.com/JetBrains/compose-multiplatform-core/pull/1649)
- [Fix issue where `DateRangePicker` doesn't show confirmation button on iOS and Desktop](https://github.com/JetBrains/compose-multiplatform-core/pull/1666)
- [Fix Skia paragraph caching performance degradation](https://github.com/JetBrains/compose-multiplatform-core/pull/1676)

### iOS

- [Fling animation works correctly for fast scrolling gestures](https://github.com/JetBrains/compose-multiplatform-core/pull/1616)
- [Fix HorizontalPager snapping on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1661)
- [Fixed double recomposition on the first screen](https://github.com/JetBrains/compose-multiplatform-core/pull/1668)
- [Fix Accessibility Items availability inside dialogs](https://github.com/JetBrains/compose-multiplatform-core/pull/1678)
- [Memory leak due to Compose view controller never GCed](https://github.com/JetBrains/compose-multiplatform-core/pull/1660)

### Desktop

- [Fix for excessive garbage generation from redrawing on Swing](https://github.com/JetBrains/compose-multiplatform-core/pull/1657)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.7.1`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.5](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.5)
  - [UI 1.7.5](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.5)
  - [Foundation 1.7.5](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.5)
  - [Material 1.7.5](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.5)
  - [Material3 1.3.1](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.1)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.4`. Based on [Jetpack Lifecycle 2.8.5](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.5)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha10`. Based on [Jetpack Navigation 2.8.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.0.1`. Based on [Jetpack Material3 Adaptive 1.0.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0)

---

# 1.7.0 (October 2024)

_Changes since 1.6.11_

## Highlights

## Resources

- [Compose Multiplatform resources are stored in the android assets now. This fixes Android Studio Preview and cases such as a rendering resource files in WebViews or Media Players](https://github.com/JetBrains/compose-multiplatform/pull/4965)

### Navigation

- [Shared Element Transitions](https://developer.android.com/develop/ui/compose/animation/shared-elements)
- [Safe Args in Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety)

### Web

- [`skiko.js` is redundant in case of K/Wasm Compose Multiplatform for web applications and it can be removed from index.html files to not load redundant files](https://github.com/JetBrains/compose-multiplatform/pull/5134). `skiko.js` will be removed from the k/wasm distribution in the future releases. `skiko.js` is still needed in case of K/JS Compose Multiplatform for web apps

## Breaking changes

### iOS

- [`UIKitView` and `UIKitViewController` in `package androidx.compose.ui.interop` are deprecated](https://github.com/JetBrains/compose-multiplatform-core/pull/1494). New API are mentioned in deprecation message. Deprecated invocations should work fine unless custom `onResize` is used, it is disallowed now and will print a warning.
- [Actual of expected `InteropView` on iOS is `UIResponder` now instead of `UIView`](https://github.com/JetBrains/compose-multiplatform-core/pull/1489). It's the first common ancestor for `UIViewController` and `UIView`, both of which can be integrated using iOS interop APIs
- [The app will crash by default, if `CADisableMinimumFrameDurationOnPhone` is not set to true in `Info.plist`](https://github.com/JetBrains/compose-multiplatform-core/pull/1451). Use newly added `ComposeUIViewControllerConfiguration.enforceStrictPlistSanityCheck` to opt-out of this behavior

### Desktop

- [`Modifier.onExternalDrag` has been deprecated in favor of the new `Modifier.dragAndDropTarget`](https://github.com/JetBrains/compose-multiplatform-core/pull/1528)

### Android

- [Minimal supported AGP raised to 8.1.0](https://github.com/JetBrains/compose-multiplatform/pull/4840)

### Resources

- [Deprecate resources in `compose.ui` in favour of the new resource library](https://github.com/JetBrains/compose-multiplatform-core/pull/1457)

## Features

### Multiple Platforms
- [The `clickable` modifier now responds to NumPadEnter and Spacebar, too, in addition to Enter](https://github.com/JetBrains/compose-multiplatform-core/pull/1464)
- [`LocalLifecycleOwner` moved from Compose UI to `lifecycle-runtime-compose` so that its Compose-based helper APIs can be used outside of Compose UI](https://github.com/JetBrains/compose-multiplatform-core/pull/1449)
- [Skia is updated to m126](https://github.com/JetBrains/compose-multiplatform-core/pull/1486)
- [Commonized `org.jetbrains.compose.material3:material3-window-size-class` module](https://github.com/JetBrains/compose-multiplatform-core/pull/1466)
- [Commonized `org.jetbrains.compose.material3.adaptive:adaptive*` modules](https://github.com/JetBrains/compose-multiplatform-core/pull/1468)
- [New multiplatform module "material-navigation" (in beta status)](https://github.com/JetBrains/compose-multiplatform-core/pull/1504)
- [`material3-adaptive-navigation-suite` is multiplatform now](https://github.com/JetBrains/compose-multiplatform-core/pull/1539)
- [Support Kotlin 1.9.25](https://github.com/JetBrains/compose-multiplatform/pull/5141)

### iOS

- [Initial iOS floating cursor support](https://github.com/JetBrains/compose-multiplatform-core/pull/1312)
- [Added `accessibilityEnabled: Boolean = true` argument to `UIKitView` and `UIKitViewController`](https://github.com/JetBrains/compose-multiplatform-core/pull/1350)
- [`preferredStatusBarStyle`, `preferredStatysBarAnimation` and `prefersStatusBarHidden` are added to `ComposeUIViewControllerDelegate` to allow status bar appearance modification](https://github.com/JetBrains/compose-multiplatform-core/pull/1378)
- [Improvements in touches processing to detect if touches were meant to be delivered to interop views, or should be processed by Compose](https://github.com/JetBrains/compose-multiplatform-core/pull/1440)
- [New `UIKitView` and `UIKitViewController` API in `package androidx.compose.ui.viewinterop`](https://github.com/JetBrains/compose-multiplatform-core/pull/1494). Support of `onReset` to reuse the interop composable emitted node and avoid excessive native views reallocations, fine-grain touches strategy control (cooperative with explicit time delay, non-cooperative where no touches are received by Compose, ignoring touches)
- [Basic support for `BasicTextField(TextFieldState, ...)` on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1540)

### Desktop

- [Add constructor with `RenderSettings` to `ComposePanel`. Added a class `RenderSettings` with  `val isVsyncEnabled: Boolean?`. When set to `true` gives a hint to renderer implementation of the particular `ComposePanel` to reduce the latency between the input and visual changes in exchange for possible screen tearing](https://github.com/JetBrains/compose-multiplatform-core/pull/1377)
- [Add public `moveEnabled` and `positionPercentage` setters in `SplitPaneState`](https://github.com/JetBrains/compose-multiplatform/pull/3974)
- [Implemented Drag-and-Drop from AOSP: `Modifier.dragAndDropSource` and `Modifier.dragAndDropTarget`](https://github.com/JetBrains/compose-multiplatform-core/pull/1433)
- [Added support for input methods (languages such as Chinese, Korean, Arabic) to BasicTextField(TextFieldState, ...)](https://github.com/JetBrains/compose-multiplatform-core/pull/1496)
- [Add dynamic Drag&Drop target indication](https://github.com/JetBrains/compose-multiplatform-core/pull/1510) (🚫 icon under cursor if currently there is no valid drop target under it)
- [The thickness of border resizers in undecorated windows and dialogs can now be controlled by passing a new `decoration` argument](https://github.com/JetBrains/compose-multiplatform-core/pull/1505)

### Resources

- [Speed resources web rendering up by the reading a cached value instantly](https://github.com/JetBrains/compose-multiplatform/pull/4893)
- [If there is no resource with suitable density, use resource with the most suitable density, otherwise use default (similar to the Android logic)](https://github.com/JetBrains/compose-multiplatform/pull/4969)
- [Add a customization for resources directories. Now it is possible to use e.g downloaded resources](https://github.com/JetBrains/compose-multiplatform/pull/5016)
- [Now the gradle plugin generates resources map to find a resource by a string ID](https://github.com/JetBrains/compose-multiplatform/pull/5068)
- [To avoid constant reading raw font bytes on each Font usage on non-android targets, there was added the font cache. Android has own font cache inside the platform implementation](https://github.com/JetBrains/compose-multiplatform/pull/5109)
- [Added utility functions to decode `Bitmap ByteArray as ImageVector` and `XML ByteArray as ImageVector` in the common code and `SVG ByteArray as Painter` in the non-android code](https://github.com/JetBrains/compose-multiplatform/pull/5098)
- [Added support of test resources in Compose Multiplatform projects](https://github.com/JetBrains/compose-multiplatform/pull/5122)
- [Added support of multi-module resources in JVM-only projects](https://github.com/JetBrains/compose-multiplatform/pull/5122)

### Gradle Plugin

- [New `compose.material3AdaptiveNavigationSuite` shortcut in the gradle plugin](https://github.com/JetBrains/compose-multiplatform/pull/5133)

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
- [Fix changing `FontRenderingSettings` is not reflected until composition restarts](https://github.com/JetBrains/compose-multiplatform-core/pull/1595)

### iOS

- [Pressing directional keys on a physical keyboard connected to iOS device doesn't cause a crash](https://github.com/JetBrains/compose-multiplatform-core/pull/1383)
- [Dismissing popup or dialogue within a very short timespan after its creation doesn't cause a crash](https://github.com/JetBrains/compose-multiplatform-core/pull/1384)
- [Fix missing invalidations during native view resize](https://github.com/JetBrains/compose-multiplatform-core/pull/1387)
- [Fixed a memory spike when continuously resizing the `ComposeUIViewController` (such as when used in modal sheet presentation context with different detents)](https://github.com/JetBrains/compose-multiplatform-core/pull/1390)
- [visibility of selection handles in single-line textfields with LTR + RTL text in iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1331)
- [Interop views are now correctly clipped when their measured clipped and unclipped bounding boxes don't match](https://github.com/JetBrains/compose-multiplatform-core/pull/1430)
- [Touches inside interop views are not exclusive to them and are processed on Compose side as well.](https://github.com/JetBrains/compose-multiplatform-core/pull/1426)
- [Fix `material3.ModalBottomSheet` safe area usage](https://github.com/JetBrains/compose-multiplatform-core/pull/1438)
- [Fix hiding interop element during quick scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1425)
- [Fixed the keyboard appearing when selecting from SelectionContainer](https://github.com/JetBrains/compose-multiplatform-core/pull/1448)
- [Fix status bar padding on iPad devices](https://github.com/JetBrains/compose-multiplatform-core/pull/1442)
- [VoiceOver doesn't allow to perform a11y actions (scrolling, activate, customActions) when disabled() semantics is present in affected element](https://github.com/JetBrains/compose-multiplatform-core/pull/1446)
- [Fix frame drops when dragging scrollable content on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1503)
- [A new approach to implementation of `platformLayers`.](https://github.com/JetBrains/compose-multiplatform-core/pull/1515) Now extra layers (such as Dialogs and Popups) drawing is merged into a single screen size canvas. No jittering and crashes should happen with those anymore.
- [`Dialog`s and `Popup`s now have their insets calculated correctly even when the frame of `ComposeUIViewController` spawning them doesn't intersect any safe areas](https://github.com/JetBrains/compose-multiplatform-core/pull/1515)
- [Fix offset issues with keyboard and `TextField`](https://github.com/JetBrains/compose-multiplatform-core/pull/1523)
- [Fix "Incorrect `imePadding` and high cpu usage when repeatedly opening and closing `Keyboard` on iOS"](https://github.com/JetBrains/compose-multiplatform-core/pull/1523)
- [Fix "Selection handlers in wrong positions in a fullscreen TextField"](https://github.com/JetBrains/compose-multiplatform-core/pull/1523)
- [Fix keyboard closing while scrolling content with Text Fields](https://github.com/JetBrains/compose-multiplatform-core/pull/1558)
- [Fix "UriHandler.openUri no longer works on iOS 18"](https://github.com/JetBrains/compose-multiplatform-core/pull/1595)

### Desktop

- [Fix possible `UninitializedPropertyAccessException` in `desktopTest`](https://github.com/JetBrains/compose-multiplatform-core/pull/1343)
- [Fixed `ComposePanel.requestFocus()`, making it correctly assign focus to the first focusable child](https://github.com/JetBrains/compose-multiplatform-core/pull/1352)
- [When using `ComposePanel` inside a Swing application on macOS, VoiceOver will now correctly go into the `ComposePanel` when traversing accessible elements](https://github.com/JetBrains/compose-multiplatform-core/pull/1362)
- [When using `ComposePanel` inside a Swing application on Windows with NVDA turned on, focus will now correctly go into the `ComposePanel` when traversing with (ctrl)-shift-tab](https://github.com/JetBrains/compose-multiplatform-core/pull/1363)
- [Correctly save `WindowState` with unspecified `size` instead of crashing](https://github.com/JetBrains/compose-multiplatform-core/pull/1394)
- [Fix `IndexOutOfBoundsException` crash on Windows when traversing a11y elements](https://github.com/JetBrains/compose-multiplatform-core/pull/1415)
- [Fix scrolling non-same direction nested scrolls with trackpad](https://github.com/JetBrains/compose-multiplatform-core/pull/1434)
- [Fix fling velocity for precise wheel scroll](https://github.com/JetBrains/compose-multiplatform-core/pull/1402)
- [[macOS] Fix crash when right-clicking an empty `SelectionContainer` or on the padding of a `Text` inside a `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform-core/pull/1439)
- [Fix bounds of `ComposePanel` in IntelliJ on macOs](https://github.com/JetBrains/compose-multiplatform-core/pull/1571)
- [Fix UI glitch when resizing a Compose window via its `WindowState`](https://github.com/JetBrains/compose-multiplatform-core/pull/1565)

### Web

- [Process `keydown` and `keyup` keys for identified keys from virtual keyboard](https://github.com/JetBrains/compose-multiplatform-core/pull/1380)
- [Allow preloading the fallback fonts. This enables the usage of emojis and other unicode characters without manually composing the Text with AnnotatedString](https://github.com/JetBrains/compose-multiplatform-core/pull/1400)
- [Make sure the web app distribution doesn't contain a duplicate `skiko.wasm`](https://github.com/JetBrains/compose-multiplatform/pull/4958)
- [Prevent a crash on mobile web when selecting some text in `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform-core/pull/1551)

### Resources

- [Delete `contextClassLoader` usage on JVM targets](https://github.com/JetBrains/compose-multiplatform/pull/4895)
- [Create an empty resource dir with "podspec" task instead "podInstall"](https://github.com/JetBrains/compose-multiplatform/pull/4900)
- [Fix resource accessors escaping. Now it is possible to use resources with names: "package", "is", "item_$xxx" etc](https://github.com/JetBrains/compose-multiplatform/pull/4901)
- [Read exactly requested count of bytes from InputStream on jvm platforms](https://github.com/JetBrains/compose-multiplatform/pull/4943)
- [Now drawables from upper DPIs will be downscalled to the expected size. (the same behavior as on Android)](https://github.com/JetBrains/compose-multiplatform/pull/5101)

### Gradle Plugin

- [Make sure tryGetSkikoRuntimeIfNeeded is executed only during the task execution](https://github.com/JetBrains/compose-multiplatform/pull/4918)
- [Delete outdated build services](https://github.com/JetBrains/compose-multiplatform/pull/4959)
- [Support project isolation](https://github.com/JetBrains/compose-multiplatform/pull/5120)
- [Fix a gradle project misconfiguration when KSP and Room are used](https://github.com/JetBrains/compose-multiplatform/pull/5129)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.7.0`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.1](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.1)
  - [UI 1.7.1](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.1)
  - [Foundation 1.7.1](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.1)
  - [Material 1.7.1](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.1)
  - [Material3 1.3.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.3`. Based on [Jetpack Lifecycle 2.8.5](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.5)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha10`. Based on [Jetpack Navigation 2.8.0](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.0.0`. Based on [Jetpack Material3 Adaptive 1.0.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0)

---

# 1.7.0-rc01 (September 2024)

_Changes since 1.7.0-beta02_

## Highlights

### Web

- [`skiko.js` is redundant in case of K/Wasm Compose Multiplatform for web applications and it can be removed from index.html files to not load redundant files](https://github.com/JetBrains/compose-multiplatform/pull/5134). `skiko.js` will be removed from the k/wasm distribution in the future releases. `skiko.js` is still needed in case of K/JS Compose Multiplatform for web apps

## Features

### Multiple Platforms

- [Support Kotlin 1.9.25](https://github.com/JetBrains/compose-multiplatform/pull/5141)

### Desktop

- _(prerelease fix)_ [The `decoration` parameter added to `Window` and `DialogWindow` and the APIs related to it are now marked as experimental](https://github.com/JetBrains/compose-multiplatform-core/pull/1561)

### Gradle Plugin

- [New `compose.material3AdaptiveNavigationSuite` shortcut in the gradle plugin](https://github.com/JetBrains/compose-multiplatform/pull/5133)

## Fixes

### Multiple Platforms

- _(prerelease fix)_ [Fix possible infinity invalidation loop triggered by `GraphicsLayer.record`](https://github.com/JetBrains/compose-multiplatform-core/pull/1555)
- [Fix changing `FontRenderingSettings` is not reflected until composition restarts](https://github.com/JetBrains/compose-multiplatform-core/pull/1595)

### iOS

- _(prerelease fix)_ [Fix "`ListDetailPaneScaffold` from material3-adaptive throws ArrayIndexOutOfBoundsException"](https://github.com/JetBrains/compose-multiplatform-core/pull/1548)
- _(prerelease fix)_ [Fix "White bars on sides on some devices"](https://github.com/JetBrains/compose-multiplatform-core/pull/1547)
- [Fix offset issues with keyboard and `TextField`](https://github.com/JetBrains/compose-multiplatform-core/pull/1523)
- [Fix "Incorrect `imePadding` and high cpu usage when repeatedly opening and closing `Keyboard` on iOS"](https://github.com/JetBrains/compose-multiplatform-core/pull/1523)
- [Fix "Selection handlers in wrong positions in a fullscreen TextField"](https://github.com/JetBrains/compose-multiplatform-core/pull/1523)
- [Fix keyboard closing while scrolling content with Text Fields](https://github.com/JetBrains/compose-multiplatform-core/pull/1558)
- _(prerelease fix)_ [Fix missing interop views with new `onReset` argument and placing inside more complex reusable layout inside `Lazy*` lists](https://github.com/JetBrains/compose-multiplatform-core/pull/1560)
- _(prerelease fix)_ [Fix selection handlers height for `BasicTextField` on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1587)
- _(prerelease fix)_ [To avoid `Symbol not found: _objc_release_x8` crash on iOS 15 simulators, skia has been re-built with downgraded Xcode](https://github.com/JetBrains/compose-multiplatform-core/pull/1595) (13.1)
- [Fix "UriHandler.openUri no longer works on iOS 18"](https://github.com/JetBrains/compose-multiplatform-core/pull/1595)

### Desktop

- [Fix bounds of `ComposePanel` in IntelliJ on macOs](https://github.com/JetBrains/compose-multiplatform-core/pull/1571)
- [Fix UI glitch when resizing a Compose window via its `WindowState`](https://github.com/JetBrains/compose-multiplatform-core/pull/1565)

### Web

- [Prevent a crash on mobile web when selecting some text in `SelectionContainer`](https://github.com/JetBrains/compose-multiplatform-core/pull/1551)

### Android

- _(prerelease fix)_ [Fix "Compose UI test error on android: No static method forceEnableAppTracing"](https://github.com/JetBrains/compose-multiplatform-core/pull/1564)
- _(prerelease fix)_ [Fix "Android target depends on prerelease versions"](https://github.com/JetBrains/compose-multiplatform-core/pull/1564)

### Navigation

- _(prerelease fix)_ [Fix `IllegalArgumentException` on putting lists into `savedStateHandle`](https://github.com/JetBrains/compose-multiplatform-core/pull/1546)

## Dependencies

- Gradle Plugin `org.jetbrains.compose`, version `1.7.0-rc01`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.0)
  - [UI 1.7.0](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.0)
  - [Foundation 1.7.0](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0)
  - [Material 1.7.0](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0)
  - [Material3 1.3.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.2`. Based on [Jetpack Lifecycle 2.8.4](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.4)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha10`. Based on [Jetpack Navigation 2.8.0-rc01](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-rc01)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.0.0-rc01`. Based on [Jetpack Material3 Adaptive 1.0.0](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0)

---

# 1.7.0-beta02 (September 2024)

_Changes since 1.7.0-beta01_

## Breaking changes
### Desktop
- [`Modifier.onExternalDrag` has been deprecated in favor of the new `Modifier.dragAndDropTarget`](https://github.com/JetBrains/compose-multiplatform-core/pull/1528)

### Resources
- [Deprecate resources in `compose.ui` in favour of the new resource library](https://github.com/JetBrains/compose-multiplatform-core/pull/1457)

## Features
### Multiple Platforms
- [`material3-adaptive-navigation-suite` is multiplatform now](https://github.com/JetBrains/compose-multiplatform-core/pull/1539)

### iOS
- [Basic support for BasicTextField(TextFieldState, ...) on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1540)

### Desktop
- [The thickness of border resizers in undecorated windows and dialogs can now be controlled by passing a new `decoration` argument](https://github.com/JetBrains/compose-multiplatform-core/pull/1505)

## Fixes
### Multiple Platforms
- _(prerelease fix)_ [Fix `GraphicsLayer` perspective matrix calculation and missing invalidations](https://github.com/JetBrains/compose-multiplatform-core/pull/1541)
- _(prerelease fix)_ [Fix Wasm/Native ArrayIndexOutOfBoundsException exception in adaptive-layout module](https://github.com/JetBrains/compose-multiplatform-core/pull/1545)

### iOS
- [A new approach to implementation of `platformLayers`.](https://github.com/JetBrains/compose-multiplatform-core/pull/1515) Now extra layers (such as Dialogs and Popups) drawing is merged into a single screen size canvas. No jittering and crashes should happen with those anymore.
- [`Dialog`s and `Popup`s now have their insets calculated correctly even when the frame of `ComposeUIViewController` spawning them doesn't intersect any safe areas](https://github.com/JetBrains/compose-multiplatform-core/pull/1515)

### Desktop
- _(prerelease fix)_ [Fix "Moving after initiating a click cancels it"](https://github.com/JetBrains/compose-multiplatform-core/pull/1534)

### Resources
- _(prerelease fix)_ [Fix Cocoapods resources integration which leaded to a lack resources in iOS apps](https://github.com/JetBrains/compose-multiplatform/pull/5128)

### Gradle Plugin
- [Fix a gradle project misconfiguration when KSP and Room are used](https://github.com/JetBrains/compose-multiplatform/pull/5129)

### Lifecycle
- Lifecycle 2.8.2 depends on Compose 1.6.11 (Lifecycle 2.8.1 accidentaly made dependent on Compose 1.7.0-beta01)

### Navigation
- _(prerelease fix)_ [Fix saving state for nested `NavHostController`](https://github.com/JetBrains/compose-multiplatform-core/pull/1508)
- _(prerelease fix)_ [Fixed missing commonization for type-safe version of `SavedStateHandle.toRoute`](https://github.com/JetBrains/compose-multiplatform-core/pull/1521)

## Dependencies
- Gradle Plugin `org.jetbrains.compose`, version `1.7.0-beta02`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.0-rc01)
  - [UI 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.0-rc01)
  - [Foundation 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0-rc01)
  - [Material 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0-rc01)
  - [Material3 1.3.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0-rc01)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.2`. Based on [Jetpack Lifecycle 2.8.4](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.4)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha10`. Based on [Jetpack Navigation 2.8.0-rc01](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-rc01)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.0.0-alpha03`. Based on [Jetpack Material3 Adaptive 1.0.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0-rc01)

---

# 1.7.0-beta01 (September 2024)

_Changes since 1.7.0-alpha03_

## Breaking changes
### iOS
- [`UIKitView` and `UIKitViewController` in `package androidx.compose.ui.interop` are deprecated](https://github.com/JetBrains/compose-multiplatform-core/pull/1494). New API are mentioned in deprecation message. Deprecated invocations should work fine unless custom `onResize` is used, it is disallowed now and will print a warning.
- [Actual of expected `InteropView` on iOS is `UIResponder` now instead of `UIView`](https://github.com/JetBrains/compose-multiplatform-core/pull/1489). It's the first common ancestor for `UIViewController` and `UIView`, both of which can be integrated using iOS interop APIs
- [The app will crash by default, if `CADisableMinimumFrameDurationOnPhone` is not set to true in `Info.plist`](https://github.com/JetBrains/compose-multiplatform-core/pull/1451). Use newly added `ComposeUIViewControllerConfiguration.enforceStrictPlistSanityCheck` to opt-out of this behavior

## Features
### Multiple Platforms
- [New multiplatform module "material-navigation" (in beta status)](https://github.com/JetBrains/compose-multiplatform-core/pull/1504)

### iOS
- [New `UIKitView` and `UIKitViewController` API in `package androidx.compose.ui.viewinterop`](https://github.com/JetBrains/compose-multiplatform-core/pull/1494). Support of `onReset` to reuse the interop composable emitted node and avoid excessive native views reallocations, fine-grain touches strategy control (cooperative with explicit time delay, non-cooperative where no touches are received by Compose, ignoring touches)

### Desktop
- [Added support for input methods (languages such as Chinese, Korean, Arabic) to BasicTextField(TextFieldState, ...)](https://github.com/JetBrains/compose-multiplatform-core/pull/1496)
- [Add dynamic Drag&Drop target indication](https://github.com/JetBrains/compose-multiplatform-core/pull/1510) (🚫 icon under cursor if currently there is no valid drop target under it)

### Resources
- [Added support of test resources in Compose Multiplatform projects](https://github.com/JetBrains/compose-multiplatform/pull/5122)
- [Added support of multi-module resources in JVM-only projects](https://github.com/JetBrains/compose-multiplatform/pull/5122)

## Fixes
### Multiple Platforms
- _(prerelease fix)_ [Fix redirect on android artifacts for "window-core" module](https://github.com/JetBrains/compose-multiplatform-core/pull/1506)

### iOS
- [Fix frame drops when dragging scrollable content on iOS](https://github.com/JetBrains/compose-multiplatform-core/pull/1503)

### Desktop
- _(prerelease fix)_ [Fixed drag-and-drop not working after a popup is displayed in the window](https://github.com/JetBrains/compose-multiplatform-core/pull/1493)

### Resources
- _(prerelease fix)_ [Fix a resource reading on iOS 12](https://github.com/JetBrains/compose-multiplatform/pull/5123)
- _(prerelease fix)_ [Fix resource reading on Java 11](https://github.com/JetBrains/compose-multiplatform/pull/5125)

### Gradle Plugin
- [Support project isolation](https://github.com/JetBrains/compose-multiplatform/pull/5120)

## Dependencies
- Gradle Plugin `org.jetbrains.compose`, version `1.7.0-beta01`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.0-rc01)
  - [UI 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.0-rc01)
  - [Foundation 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0-rc01)
  - [Material 1.7.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0-rc01)
  - [Material3 1.3.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0-rc01)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.1`. Based on [Jetpack Lifecycle 2.8.4](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.4)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha09`. Based on [Jetpack Navigation 2.8.0-beta05](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-beta05)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.0.0-alpha02`. Based on [Jetpack Material3 Adaptive 1.0.0-rc01](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0-rc01)

---

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
- Gradle Plugin `org.jetbrains.compose`, version `1.7.0-alpha03`. Based on Jetpack Compose libraries:
  - [Runtime 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.7.0-beta06)
  - [UI 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-ui#1.7.0-beta06)
  - [Foundation 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0-beta06)
  - [Material 1.7.0-beta06](https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0-beta06)
  - [Material3 1.3.0-beta05](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0-beta05)

- Lifecycle libraries `org.jetbrains.androidx.lifecycle:lifecycle-*:2.8.0`. Based on [Jetpack Lifecycle 2.8.0](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.0)
- Navigation libraries `org.jetbrains.androidx.navigation:navigation-*:2.8.0-alpha09`. Based on [Jetpack Navigation 2.8.0-beta05](https://developer.android.com/jetpack/androidx/releases/navigation#2.8.0-beta05)
- Material3 Adaptive libraries `org.jetbrains.compose.material3.adaptive:adaptive*:1.0.0-alpha01`. Based on [Jetpack Material3 Adaptive 1.0.0-beta04](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive#1.0.0-beta04)

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

- [Fix a cached font if the resource accessor was changed](https://github.com/JetBrains/compose-multiplatform/pull/4864)

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
