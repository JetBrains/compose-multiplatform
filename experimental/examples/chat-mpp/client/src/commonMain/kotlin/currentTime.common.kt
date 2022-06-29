/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

fun timeToString(timestampMs:Long):String {
    val seconds = timestampMs
    val minutes = seconds / 1000 / 60
    val hours = minutes / 24
    val m = minutes % 60
    val h = hours % 24
    return "$h:$m"
}

expect fun timestampMs(): Long
