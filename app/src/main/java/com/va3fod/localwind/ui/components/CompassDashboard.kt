package com.va3fod.localwind.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalTextApi::class)
@Composable
fun CompassDashboard(
    currentAzimuth: Float,
    lockedWindDirection: Float,
    declination: Float,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val themeBackgroundColor = if (isDarkTheme) Color.Black else Color.White
    val themeContentColor = if (isDarkTheme) Color.White else Color.Black
    val tickColor = if (isDarkTheme) Color.LightGray else Color.DarkGray
    val numberColor = if (isDarkTheme) Color.Gray else Color.Gray

    Box(modifier = modifier.background(themeBackgroundColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.95f
            val innerRadius = radius * 0.88f

            // 1. Static Reference Line (Lubber Line)
            drawLine(
                color = themeContentColor,
                start = Offset(center.x, center.y - radius - 5.dp.toPx()),
                end = Offset(center.x, center.y - innerRadius),
                strokeWidth = 2.dp.toPx()
            )

            // 2. Rotating Compass Dial
            val trueHeading = (currentAzimuth + declination + 360) % 360
            
            rotate(-trueHeading, center) {
                // Dial Circles
                drawCircle(color = Color.Gray, radius = radius, center = center, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = Color.Gray, radius = innerRadius, center = center, style = Stroke(width = 1.dp.toPx()))

                // Ticks and Degree Numbers
                for (i in 0 until 360 step 2) {
                    val angle = Math.toRadians(i.toDouble() - 90).toFloat()
                    val isMajor = i % 10 == 0
                    val isCardinal = i % 90 == 0
                    val tickLen = if (isCardinal) 18.dp.toPx() else if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                    
                    drawLine(
                        color = if (i == 0) Color.Cyan else tickColor,
                        start = Offset(center.x + innerRadius * cos(angle), center.y + innerRadius * sin(angle)),
                        end = Offset(center.x + (innerRadius - tickLen) * cos(angle), center.y + (innerRadius - tickLen) * sin(angle)),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    )

                    if (i % 30 == 0 && !isCardinal) {
                        val textRadius = innerRadius - 28.dp.toPx()
                        val textLayout = textMeasurer.measure(
                            text = i.toString(),
                            style = TextStyle(color = numberColor, fontSize = 11.sp)
                        )
                        drawText(textLayout, topLeft = Offset(center.x + textRadius * cos(angle) - textLayout.size.width/2, center.y + textRadius * sin(angle) - textLayout.size.height/2))
                    }
                }

                // Cardinal Labels
                val cardinals = mapOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")
                cardinals.forEach { (deg, label) ->
                    val angle = Math.toRadians(deg.toDouble() - 90).toFloat()
                    val textLayout = textMeasurer.measure(
                        text = label,
                        style = TextStyle(color = if (deg == 0) Color.Cyan else themeContentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    drawText(textLayout, topLeft = Offset(center.x + (innerRadius - 35.dp.toPx()) * cos(angle) - textLayout.size.width/2, center.y + (innerRadius - 35.dp.toPx()) * sin(angle) - textLayout.size.height/2))
                }

                // 3. True North Indicator (Cyan Triangle at 0 on dial)
                val triPathTrue = Path().apply {
                    moveTo(center.x, center.y - radius - 12.dp.toPx())
                    lineTo(center.x - 7.dp.toPx(), center.y - radius - 2.dp.toPx())
                    lineTo(center.x + 7.dp.toPx(), center.y - radius - 2.dp.toPx())
                    close()
                }
                drawPath(triPathTrue, color = Color.Cyan)

                // 4. Magnetic North Indicator (Red Triangle)
                rotate(declination, center) {
                    val triPathMag = Path().apply {
                        moveTo(center.x, center.y - radius - 12.dp.toPx())
                        lineTo(center.x - 7.dp.toPx(), center.y - radius - 2.dp.toPx())
                        lineTo(center.x + 7.dp.toPx(), center.y - radius - 2.dp.toPx())
                        close()
                    }
                    drawPath(triPathMag, color = Color.Red)
                }

                // 5. YELLOW WIND TRIANGLE (Source -> Center)
                rotate(lockedWindDirection, center) {
                    val triPathWind = Path().apply {
                        val tipY = center.y - innerRadius + 2.dp.toPx()
                        val baseWidth = 14.dp.toPx()
                        val height = 18.dp.toPx()
                        
                        moveTo(center.x, tipY)
                        lineTo(center.x - baseWidth/2, tipY - height)
                        lineTo(center.x + baseWidth/2, tipY - height)
                        close()
                    }
                    drawPath(triPathWind, color = Color.Yellow)
                }
            }

            // Static Center Crosshair
            val crossSize = 25.dp.toPx()
            drawLine(Color.Gray, Offset(center.x - crossSize, center.y), Offset(center.x + crossSize, center.y), 1.dp.toPx())
            drawLine(Color.Gray, Offset(center.x, center.y - crossSize), Offset(center.x, center.y + crossSize), 1.dp.toPx())
        }
    }
}
