package example.imageviewer.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitInteropView
import androidx.compose.ui.unit.dp
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@Composable
internal actual fun LocationVisualizer(modifier: Modifier) {
    //todo get real geo coordinates
    UIKitInteropView(
        modifier = modifier.fillMaxWidth().height(250.dp),
        factory = {
            val mkMapView = MKMapView()
            val cityAmsterdam = CLLocationCoordinate2DMake(52.3676, 4.9041)
            mkMapView.setRegion(
                MKCoordinateRegionMakeWithDistance(
                    centerCoordinate = cityAmsterdam,
                    5000.0, 5000.0
                ),
                animated = false
            )
            mkMapView.addAnnotation(MKPointAnnotation(cityAmsterdam, "I am here", null))
            mkMapView
        },
    )
}
