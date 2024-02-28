_Changes since 1.5.12_

## Highlights
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
- [Popups/Dialogs can now be displayed outside a ViewController over native components](https://github.com/JetBrains/compose-multiplatform-core/pull/1031)
- [Allow selecting `Text` in `SelectionContainer` by double and triple tap](https://github.com/JetBrains/compose-multiplatform-core/pull/984)

#### Web
- [Compose for Web (Wasm) artifacts are available in Maven Central](https://github.com/JetBrains/compose-multiplatform-core/pull/914) <sub>_**Warning**: Kotlin 1.9.21 has [an issue](https://github.com/JetBrains/compose-multiplatform/issues/4230) with web target. Use Kotlin 1.9.22._</sub>

#### Desktop
- [Proper clipping of `SwingPanel` interop](https://github.com/JetBrains/compose-multiplatform-core/pull/915) <sub>_(under an experimental flag, see the link)_</sub>
- [Popups/Dialogs can now be displayed outside the main window or panel and over native components](https://github.com/JetBrains/compose-multiplatform-core/pull/992) <sub>_(under an experimental flag, see the link)_</sub>

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

#### Desktop
- [Remove deprecated APIs in `TooltipArea` and `PointerEvent`](https://github.com/JetBrains/compose-multiplatform-core/pull/1029)

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
- [Fix "BasicTextField NPE-crash on JBR 17.0.9 on Linux when clicked"](https://github.com/JetBrains/compose-multiplatform-core/pull/973)
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