package org.jetbrains.codeviewer

import MainView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import org.jetbrains.codeviewer.platform._HomeFolder
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        copyAssets()
        _HomeFolder = filesDir

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MainView()
        }
    }

    private fun copyAssets() {
        for (filename in assets.list("data")!!) {
            assets.open("data/$filename").use { assetStream ->
                val file = File(filesDir, filename)
                FileOutputStream(file).use { fileStream ->
                    assetStream.copyTo(fileStream)
                }
            }
        }
    }
}