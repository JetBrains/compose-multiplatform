### Updating Skia

1. Update `third_party/skia` submodule in [skija repo](https://github.com/JetBrains/skija).
2. Ensure Skija build is not broken:
    * Run [<skija root>/script/build.sh](https://github.com/JetBrains/skija/blob/master/script/build.sh) 
3. If used Skia branch is changed (e.g. a switch from `chrome/m85` to `chrome/m86`), update `VER` variable value in build scripts:
    * [script/build_skia_linux.sh](https://github.com/JetBrains/skija/blob/master/script/build_skia_linux.sh)
    * [script/build_skia_windows.sh](https://github.com/JetBrains/skija/blob/master/script/build_skia_windows.sh) 
    * [script/build_skia_macos.sh](https://github.com/JetBrains/skija/blob/master/script/build_skia_macos.sh)
4. Deploy Skia to Bintray by clicking "Deploy" (or "..." to customize options) in 
[Skia | Publish Release](https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Compose_Skia_PublishRelease) 
configuration.
5. Pin the resulting build to preserve artifacts on TeamCity too.
6. Update Skia in Skiko:
    * Update the following configuration parameters in 
    [gradle.properties](https://github.com/JetBrains/skiko/blob/master/skiko/gradle.properties):
        * `dependencies.skija.git.commit`, 
        * `dependencies.skia.windows`, 
        * `dependencies.skia.linux`, 
        * `dependencies.skia.windows`. 
    * Ensure build is not broken by running `./gradlew publishToMavenLocal` in `<skiko_root>/skiko`:
    * To further ensure, that Skiko builds on all platforms, consider running a new build in 
        [Build Check configuration](https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Compose_Skiko_BuildCheckManualTrigger):
        * click "...",
        * EITHER choose a branch/commit on "Changes" tab, 
        * OR upload a custom patch by checking "run as a personal build" on "General" tab.     
            
### Releasing Skiko

1. Check, that the release is ready:
    * Ask teammates if all the necessary changes are published;
    * Determine a git commit for a release;
    * Check, that [sample projects](https://github.com/JetBrains/skiko/tree/master/samples) work:
        * E.g. `cd skiko && ./gradlew publishToMavenLocal && cd samples/SkijaInjectSample && ./gradlew run`;
        * Ask teammates for help with testing on platforms you don't have access to.
2. Deploy Skiko to Space by clicking "Deploy" in [Publish Release configuration](https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Compose_Skiko_PublishRelease):
    * Set "Skiko Release Version" on "Parameters" tab to a *new release version*;
    * Choose a branch/commit on "Changes" tab;
    * (Tip) If you're in a hurry, consider checking "put the build to the queue top" on "General" tab.
3. [Check new release on GitHub](https://github.com/JetBrains/skiko/releases)

### Updating Skiko in Compose

1. Use `<androidx-dev-master>/frameworks/support/development/importMaven/import_maven_artifacts.py` script 
to download maven artifacts:
    ```
    # you may need to add `maven("https://packages.jetbrains.team/maven/p/ui/dev")` 
    # the repository section of corresponding build.gradle.kts
    export SKIKO_VERSION="0.1.6"
    import_maven_artifacts.py --name "org.jetbrains.skiko:skiko-jvm:$SKIKO_VERSION"
    import_maven_artifacts.py --name "org.jetbrains.skiko:skiko-jvm-runtime-linux:$SKIKO_VERSION"
    import_maven_artifacts.py --name "org.jetbrains.skiko:skiko-jvm-runtime-windows:$SKIKO_VERSION"
    import_maven_artifacts.py --name "org.jetbrains.skiko:skiko-jvm-runtime-macos:$SKIKO_VERSION"
    ```
2. Commit changes to `<androidx-dev-master>/prebuilts/androidx/external` repository & upload a CL. 

### Publishing Compose Desktop

Run a new build in [Compose configuration](https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Skija_JetpackComposeMpp_Dev)

### Building Docker Images

For building locally see corresponding the instructions:
 * for [Linux](docker/linux/README.md), 
 * for [Windows](docker/windows/README.md).
     
For updating internal images on Docker registry run a build in one of 
the corresponding configurations:
* for [Linux](https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Compose_Docker_Linux),
* for [Windows](https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Compose_Docker_Windows)
