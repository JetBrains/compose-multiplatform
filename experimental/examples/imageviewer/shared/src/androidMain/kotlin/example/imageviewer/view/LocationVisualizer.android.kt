package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import example.imageviewer.model.GpsPosition

@Composable
internal actual fun LocationVisualizer(modifier: Modifier, gps: GpsPosition, title: String) {
    val currentLocation = LatLng(gps.latitude, gps.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 10f)
    }
    GoogleMap(
        modifier = modifier.height(250.dp),
        cameraPositionState = cameraPositionState
    )
}
