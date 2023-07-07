# Jetpack Compose Demos

This module contains the development demos for Jetpack Compose. We aim to provide demos for all
components where possible.

## Building

When using `studiow -c` or `./gradlew studio`, you will see a `compose.integration-tests.demos` run
configuration. You can use it to build the demo app.

## Developing

`DemoActivity`, the entry point for the demo app, accepts an intent string extra called "demoname".
You can optionally supply it to deeplink into a specific demo to speed up your workflow. This name
is the name you specify when creating a new `Demo` instance. You can supply a name for a category or
a specific demo.

Using ADB, you can pass it like this:

```bash
adb ... --es demoname "Simple Magnifier"
```

You can create your own run configuration for a specific demo:
1. Open the list of run configurations and click "Edit Configurations"
2. Duplicate the `compose.integration-tests.demos` configuration and give it a name
3. Under "Launch Options" -> "Launch", select "Specified Activity"
4. Select `DemoActivity`
5. Add `--es demoname "My Demo's Title"` to the launch flags
6. Don't forget to hit apply!