
fun isIphoneSimulatorBuild(): Boolean =
    System.getenv("NATIVE_ARCH") == "arm64" && System.getenv("SDK_NAME")?.startsWith("iphonesimulator") == true

fun isIphoneOsBuild(): Boolean =
    System.getenv("SDK_NAME")?.startsWith("iphoneos") == true
