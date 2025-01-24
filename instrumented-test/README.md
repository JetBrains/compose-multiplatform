# Compose Multiplatform Instrumented Test

## Overview

This project is a Compose Multiplatform module that implements instrumented UI tests Kotlin tests that runs as native XCTest with host app on iOS Simulator.

## Requirements

- Kotlin >= 2.1.0
- Compose Multiplatform 1.8.0-alpha02
- iOS 12+

## Testing

To execute XCTest cases on an iOS Simulator, use:

```shell
cd launcher 
xcodebuild test -scheme Launcher -destination "platform=iOS Simulator,name=iPhone 16 Pro"
```
