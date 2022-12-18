//package example.imageviewer.view
//
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.gestures.detectTransformGestures
//import androidx.compose.material.Surface
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.MutableState
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.input.pointer.pointerInput
//import example.imageviewer.style.Transparent
//
//@Composable
//fun Scalable(
//    modifier: Modifier = Modifier,
//    children: @Composable() () -> Unit
//) {
//    Surface(
//        color = Transparent,
//        modifier = modifier.pointerInput(Unit) {
//            detectTapGestures(onDoubleTap = { onScale.reset() })
//            detectTransformGestures { _, _, zoom, _ ->
//                onScale.onScale(zoom)
//            }
//        },
//    ) {
//        children()
//    }
//}
