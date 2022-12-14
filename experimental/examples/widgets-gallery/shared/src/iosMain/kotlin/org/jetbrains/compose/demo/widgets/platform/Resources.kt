package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.BitmapPainter
import org.jetbrains.compose.resources.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun painterResource(res: String): Painter {
    if (res.endsWith(".xml")) {
        return rememberVectorPainter(icons.get(res)!!)
    }

    return BitmapPainter(resource(res).rememberImageBitmap().orEmpty())
}

// automatically translated from .xml resources using https://github.com/LennartEgb/vec2compose
// TODO: implement .xml vector images loading on iOS
val icons = mapOf(
"drawable/ic_instagram.xml" to ImageVector.Builder(
    name = "ic_instagram",
    defaultWidth = 20.dp,
    defaultHeight = 20.dp,
    viewportWidth = 512f,
    viewportHeight = 512f
).path(
    fill = SolidColor(Color.Black),
    fillAlpha = 1f,
    stroke = null,
    strokeAlpha = 1f,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = PathFillType.NonZero
) {
    moveTo(352.0f, 0.0f)
    horizontalLineTo(160.0f)
    curveTo(71.648f, 0.0f, 0.0f, 71.648f, 0.0f, 160.0f)
    verticalLineToRelative(192.0f)
    curveToRelative(0.0f, 88.352f, 71.648f, 160.0f, 160.0f, 160.0f)
    horizontalLineToRelative(192.0f)
    curveToRelative(88.352f, 0.0f, 160.0f, -71.648f, 160.0f, -160.0f)
    verticalLineTo(160.0f)
    curveTo(512.0f, 71.648f, 440.352f, 0.0f, 352.0f, 0.0f)
    close()
    moveTo(464.0f, 352.0f)
    curveToRelative(0.0f, 61.76f, -50.24f, 112.0f, -112.0f, 112.0f)
    horizontalLineTo(160.0f)
    curveToRelative(-61.76f, 0.0f, -112.0f, -50.24f, -112.0f, -112.0f)
    verticalLineTo(160.0f)
    curveTo(48.0f, 98.24f, 98.24f, 48.0f, 160.0f, 48.0f)
    horizontalLineToRelative(192.0f)
    curveToRelative(61.76f, 0.0f, 112.0f, 50.24f, 112.0f, 112.0f)
    verticalLineTo(352.0f)
    close()
}.path(
    fill = SolidColor(Color.Black),
    fillAlpha = 1f,
    stroke = null,
    strokeAlpha = 1f,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = PathFillType.NonZero
) {
    moveTo(256.0f, 128.0f)
    curveToRelative(-70.688f, 0.0f, -128.0f, 57.312f, -128.0f, 128.0f)
    reflectiveCurveToRelative(57.312f, 128.0f, 128.0f, 128.0f)
    reflectiveCurveToRelative(128.0f, -57.312f, 128.0f, -128.0f)
    reflectiveCurveTo(326.688f, 128.0f, 256.0f, 128.0f)
    close()
    moveTo(256.0f, 336.0f)
    curveToRelative(-44.096f, 0.0f, -80.0f, -35.904f, -80.0f, -80.0f)
    curveToRelative(0.0f, -44.128f, 35.904f, -80.0f, 80.0f, -80.0f)
    reflectiveCurveToRelative(80.0f, 35.872f, 80.0f, 80.0f)
    curveTo(336.0f, 300.096f, 300.096f, 336.0f, 256.0f, 336.0f)
    close()
}.path(
    fill = SolidColor(Color.Black),
    fillAlpha = 1f,
    stroke = null,
    strokeAlpha = 1f,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = PathFillType.NonZero
) {
    moveTo(393.6f, 118.4f)
    moveToRelative(-17.056f, 0.0f)
    arcToRelative(17.056f, 17.056f, 0.0f, true, true, 34.112f, 0.0f)
    arcToRelative(17.056f, 17.056f, 0.0f, true, true, -34.112f, 0.0f)
}.build(),

"drawable/ic_send.xml" to ImageVector.Builder(
    name = "ic_send",
    defaultWidth = 20.dp,
    defaultHeight = 20.dp,
    viewportWidth = 512.001f,
    viewportHeight = 512.001f
).path(
    fill = SolidColor(Color.Black),
    fillAlpha = 1f,
    stroke = null,
    strokeAlpha = 1f,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = PathFillType.NonZero
) {
    moveTo(507.608f, 4.395f)
    curveToRelative(-4.243f, -4.244f, -10.609f, -5.549f, -16.177f, -3.321f)
    lineTo(9.43f, 193.872f)
    curveToRelative(-5.515f, 2.206f, -9.208f, 7.458f, -9.42f, 13.395f)
    curveToRelative(-0.211f, 5.936f, 3.101f, 11.437f, 8.445f, 14.029f)
    lineToRelative(190.068f, 92.181f)
    lineToRelative(92.182f, 190.068f)
    curveToRelative(2.514f, 5.184f, 7.764f, 8.455f, 13.493f, 8.455f)
    curveToRelative(0.178f, 0.0f, 0.357f, -0.003f, 0.536f, -0.01f)
    curveToRelative(5.935f, -0.211f, 11.189f, -3.904f, 13.394f, -9.419f)
    lineToRelative(192.8f, -481.998f)
    curveTo(513.156f, 15.001f, 511.851f, 8.638f, 507.608f, 4.395f)
    close()
    moveTo(52.094f, 209.118f)
    lineTo(434.72f, 56.069f)
    lineTo(206.691f, 284.096f)
    lineTo(52.094f, 209.118f)
    close()
    moveTo(302.883f, 459.907f)
    lineToRelative(-74.979f, -154.599f)
    lineToRelative(228.03f, -228.027f)
    lineTo(302.883f, 459.907f)
    close()
}.build(),

"drawable/ic_twitter.xml" to ImageVector.Builder(
    name = "ic_twitter",
    defaultWidth = 20.dp,
    defaultHeight = 20.dp,
    viewportWidth = 512f,
    viewportHeight = 512f
).path(
    fill = SolidColor(Color.Black),
    fillAlpha = 1f,
    stroke = null,
    strokeAlpha = 1f,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = PathFillType.NonZero
) {
    moveTo(459.37f, 151.716f)
    curveToRelative(0.325f, 4.548f, 0.325f, 9.097f, 0.325f, 13.645f)
    curveToRelative(0.0f, 138.72f, -105.583f, 298.558f, -298.558f, 298.558f)
    curveToRelative(-59.452f, 0.0f, -114.68f, -17.219f, -161.137f, -47.106f)
    curveToRelative(8.447f, 0.974f, 16.568f, 1.299f, 25.34f, 1.299f)
    curveToRelative(49.055f, 0.0f, 94.213f, -16.568f, 130.274f, -44.832f)
    curveToRelative(-46.132f, -0.975f, -84.792f, -31.188f, -98.112f, -72.772f)
    curveToRelative(6.498f, 0.974f, 12.995f, 1.624f, 19.818f, 1.624f)
    curveToRelative(9.421f, 0.0f, 18.843f, -1.3f, 27.614f, -3.573f)
    curveToRelative(-48.081f, -9.747f, -84.143f, -51.98f, -84.143f, -102.985f)
    verticalLineToRelative(-1.299f)
    curveToRelative(13.969f, 7.797f, 30.214f, 12.67f, 47.431f, 13.319f)
    curveToRelative(-28.264f, -18.843f, -46.781f, -51.005f, -46.781f, -87.391f)
    curveToRelative(0.0f, -19.492f, 5.197f, -37.36f, 14.294f, -52.954f)
    curveToRelative(51.655f, 63.675f, 129.3f, 105.258f, 216.365f, 109.807f)
    curveToRelative(-1.624f, -7.797f, -2.599f, -15.918f, -2.599f, -24.04f)
    curveToRelative(0.0f, -57.828f, 46.782f, -104.934f, 104.934f, -104.934f)
    curveToRelative(30.213f, 0.0f, 57.502f, 12.67f, 76.67f, 33.137f)
    curveToRelative(23.715f, -4.548f, 46.456f, -13.32f, 66.599f, -25.34f)
    curveToRelative(-7.798f, 24.366f, -24.366f, 44.833f, -46.132f, 57.827f)
    curveToRelative(21.117f, -2.273f, 41.584f, -8.122f, 60.426f, -16.243f)
    curveToRelative(-14.292f, 20.791f, -32.161f, 39.308f, -52.628f, 54.253f)
    close()
}.build())