package com.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URL

/**
 * MapView to display Earth tile maps. API provided by OpenStreetMap.
 *
 * @param modifier to specify size strategy for this composable
 *
 * @param latitude initial Latitude of map center.
 * Available values between [-90.0 (South) .. 90.0 (North)]
 *
 * @param longitude initial Longitude of map center
 * Available values between [-180.0 (Left) .. 180.0 (Right)]
 *
 * @param startScale initial scale
 * (value around 1.0   = entire Earth view),
 * (value around 30.0  = Countries),
 * (value around 150.0 = Cities),
 * (value around 40000.0 = Street's)
 */
@Composable
fun MapViewWithButtons(
    modifier: Modifier,
    userAgent: String,
    latitude: Double,
    longitude: Double,
    startScale: Double
) {
    val scaleCoefficient = 2f
    val maxScale = 20_000f
    val minScale = 4f

    var targetScale: Float by remember { mutableStateOf(startScale.toFloat()) }
    val currentScale: Float by animateFloatAsState(targetScale)
    val mapState = remember { mutableStateOf(MapState(latitude, longitude, startScale)) }
    mapState.value = mapState.value.copy(scale = currentScale.toDouble())

    Box(modifier) {
        MapView(
            Modifier.fillMaxSize(),
            userAgent,
            state = mapState
        )
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            ZoomBtn(Icons.Filled.ZoomIn, "zoom in") {
                if (targetScale < maxScale) {
                    targetScale *= scaleCoefficient
                }
            }
            ZoomBtn(Icons.Filled.ZoomOut, "zoom out") {
                if (targetScale > minScale) {
                    targetScale /= scaleCoefficient
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter)) {
            LinkText("OpenStreetMap license", Config.OPENSTREET_MAP_LICENSE)
            LinkText("Usage policy", Config.OPENSTREET_MAP_POLICY)
        }
    }
}

@Composable
private fun ZoomBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .clickable {
                onClick()
            }
    ) {
        Icon(icon, contentDescription, Modifier.fillMaxSize().padding(2.dp), Color.Blue)
    }
}

@Composable
private fun LinkText(text: String, link: String) {
    Text(
        text = text,
        color = Color.Blue,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable {
            navigateToUrl(link)
        }
            .padding(4.dp)
            .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(5.dp))
            .padding(10.dp)
            .clip(RoundedCornerShape(5.dp))
    )
}

private fun navigateToUrl(url: String) {
    Desktop.getDesktop().browse(URL(url).toURI())
}
