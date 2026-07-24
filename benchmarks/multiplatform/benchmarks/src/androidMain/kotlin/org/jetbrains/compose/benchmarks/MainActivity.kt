package org.jetbrains.compose.benchmarks

import BenchmarkRunner
import Config
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import getBenchmarks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val args = intent.extras?.getString("args")
            ?.trim('\'', '"')
            ?.split(" ")
            ?.filter { it.isNotEmpty() }
            ?.toTypedArray()
            ?: emptyArray()

        Config.setGlobalFromArgs(args)

        setContent {
            val display = windowManager.defaultDisplay
            val frameRate = display.refreshRate.toInt().takeIf { it > 0 } ?: 60
            BenchmarkRunner(getBenchmarks(), frameRate, {
                println("Completed!")
                finishAndRemoveTask()
            })
        }
    }
}
