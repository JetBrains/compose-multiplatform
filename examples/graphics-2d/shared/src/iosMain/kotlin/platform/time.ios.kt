package platform

actual fun nanoTime(): Long = kotlin.system.getTimeNanos()