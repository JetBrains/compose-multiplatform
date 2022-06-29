import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

fun currentTime(): String {
    val time: Instant = Clock.System.now()
    val toLocalDateTime = time.toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
    toLocalDateTime.hour
    toLocalDateTime.minute
    return with(toLocalDateTime) {
        "$hour:$minute"
    }
}
