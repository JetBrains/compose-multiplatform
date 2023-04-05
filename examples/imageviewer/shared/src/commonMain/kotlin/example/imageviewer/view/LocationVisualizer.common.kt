package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.model.GpsPosition

@Composable
internal expect fun LocationVisualizer(modifier: Modifier, gps: GpsPosition, title: String)