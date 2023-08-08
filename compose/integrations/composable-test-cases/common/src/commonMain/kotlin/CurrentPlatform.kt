import kotlin.jvm.JvmInline

@JvmInline
value class CurrentPlatform internal constructor(internal val value: Int) {

    fun name(): String {
        return when (this) {
            Desktop -> "Desktop"
            Web -> "Web"
            Native -> "Native"
            else -> "Unknown CurrentPlatform"
        }
    }
    companion object {
        val Desktop = CurrentPlatform(0)
        val Web = CurrentPlatform(10)
        val Native = CurrentPlatform(20)
    }
}

expect val currentPlatform: CurrentPlatform
