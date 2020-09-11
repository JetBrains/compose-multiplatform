CEF integration for Desktop Jetpack Compose.

Setup:
1. Clone the java-cef repository (``git clone https://bitbucket.org/chromiumembedded/java-cef.git``) into ``/third_party`` directory.
2. Apply patch ``/third_party/java-cef-jb-compose-patch/jb_compose_support.patch`` to ``/third_party/java-cef``
3. Download [skiko-jvm-0.1.6.jar](https://github.com/JetBrains/skiko/releases) library and copy it to ``/third_party/java-cef/third_party/jogamp/jar`` directory.
4. Follow instructions to compile java-cef ([BranchesAndBuilding](https://bitbucket.org/chromiumembedded/java-cef/wiki/BranchesAndBuilding.md)) **until you reach step 3 of the instruction**.
5. Make **jcef.jar** - execute command in terminal (from ``/third_party/java-cef/tools`` directory):
    Windows: ``make_jar.bat win64``
    Linux: ``make_jar.sh linux64``
6. Copy **jcef.jar** file from ``/third_party/java-cef/out/win64`` (or ``/third_party/java-cef/out/linux64`` for Linux) to ``/libs`` directory.

Run example:
To run application execute in terminal: ``./gradlew run``

PS. Mac OS X is currently not supported.
