import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


fun timeToString(seconds: Long): String {
    val instant: Instant = Instant.fromEpochSeconds(seconds)
    val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val m = localTime.minute
    val h = localTime.hour

    val mm = if (m < 10) {
        "0$m"
    } else {
        m.toString()
    }
    val hh = if (h < 10) {
        "0$h"
    } else {
        h.toString()
    }
    return "$hh:$mm"
}

