package com.google.r4a.examples.explorerapp.ui.components

import android.content.Context
import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.adapters.*
import java.util.*

// NOTE(lmr): having this as a view instead of just a helper function is pretty questionable. Perhaps we should change.
class TimeAgo(context: Context) : TextView(context) {
    fun setDate(date: Long) {
        setText(timeAgo(date))
    }
    // TODO(lmr): internationalize
    private fun timeAgo(date: Long): String {
        val current = Date().getTime() / 1000
        val diff = current - date
        if (diff < 0) {
            return "now"
        }

        val seconds = diff.toDouble()
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val years = days / 365

        return when {
            seconds < 45 -> "now"
            minutes < 45 -> "${Math.round(minutes)}m"
            hours < 24 -> "${Math.round(hours)}h"
            hours < 42 -> "1d"
            days < 30 -> "${Math.round(days)}d"
            days < 365 -> "${Math.round(days / 30)}mn"
            else -> "${Math.round(years)}y"
        }
    }
}

