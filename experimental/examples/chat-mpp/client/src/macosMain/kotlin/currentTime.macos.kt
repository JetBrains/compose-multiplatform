import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun timestampMs(): Long {
    return NSDate().timeIntervalSince1970().toLong()
}
