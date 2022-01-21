## Example Compose Multiplatform based plugin for IntelliJ Idea.

A plugin, demonstrating an Intellij plugin, showing a dialog window written with Compose.

The only difference from [examples/intellij-plugin](../intellij-plugin) is that
this version does not bundle Compose runtime, which makes the plugin smaller
and allows sharing Compose runtime between multiple plugins
(Compose class files and native libraries are not loaded by each plugin).

### Usage

1. Start test IDE:
   * Choose the `runIde` task in the Gradle panel in Intellij;
   * Or run the following command in terminal: `./gradlew runIde`;
2. Create a new project or open any existing;
3. Select `Show Compose Demo...` from the `Tools` menu.

![screen1](../intellij-plugin/screenshots/toolsshow.png)
![screen2](../intellij-plugin/screenshots/screenshot.png)
