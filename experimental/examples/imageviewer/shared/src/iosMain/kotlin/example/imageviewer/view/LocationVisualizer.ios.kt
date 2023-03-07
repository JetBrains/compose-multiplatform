package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitInteropView
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@Composable
internal actual fun LocationVisualizer(modifier: Modifier) {
    //todo get real geo coordinates
    UIKitInteropView(
        modifier = modifier,
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
            mkMapView.addAnnotation(MKPointAnnotation(cityAmsterdam, title = null, subtitle = null))
            mkMapView
        },
    )
}
