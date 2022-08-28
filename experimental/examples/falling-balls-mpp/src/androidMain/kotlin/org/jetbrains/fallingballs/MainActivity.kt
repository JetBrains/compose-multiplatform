package org.jetbrains.fallingballs

import FallingBalls
import Game
import Time
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember

object AndroidTime : Time {
    override fun now(): Long = System.nanoTime()
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val game = remember { Game(AndroidTime) }
            FallingBalls(game)
        }
    }
}