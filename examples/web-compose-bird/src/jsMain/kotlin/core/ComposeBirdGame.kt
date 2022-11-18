package core

import kotlin.js.Date

actual fun getTime(): Double = Date().getTime()
