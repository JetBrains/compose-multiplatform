import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithNativeShortcuts
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun KotlinTargetContainerWithNativeShortcuts.iosWorkaroundSupportArm64Simulator(
    configure: KotlinNativeTarget.() -> Unit
) {
    val isBuildToSimulator = System.getenv("SDK_NAME")?.startsWith("iphonesimulator") ?: false
    val isArm64Target = System.getenv("NATIVE_ARCH") == "arm64"

    if (isBuildToSimulator && isArm64Target) {
        //workaround:
        iosSimulatorArm64(name = "ios", configure = configure)
    } else {
        //default behavior:
        ios(configure = configure)
    }
}
