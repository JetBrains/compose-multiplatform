package com.ibenabdallah.indicatorscmp

import App
import Indicators
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import config.IIndicatorsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppAndroidPreview() {
    App()
}

@Preview(showBackground = true)
@Composable
fun IndicatorsPreview() {
    IIndicatorsTheme {
        Indicators(
            count = 7,
            size = 10,
            spacer = 5,
            selectedColor = Color.Red,
            unselectedColor = Color.Blue,
            modifier = Modifier
                .background(Color.White)
                .padding(vertical = 5.dp),
            selectedIndex = 2
        )
    }
}