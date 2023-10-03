package fallingballs

actual fun createTime():Time = JvmTime

object JvmTime : Time {
    override fun now(): Long = kotlin.system.getTimeNanos()
}
