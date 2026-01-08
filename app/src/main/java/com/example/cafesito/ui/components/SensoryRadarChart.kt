package com.example.cafesito.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SensoryRadarChart(
    data: Map<String, Float>,
    maxValue: Float = 10f,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val labels = data.keys.toList()
    val values = data.values.toList()
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) { 
            val center = this.center
            val radius = min(size.width, size.height) / 2.5f // Smaller radius to make space for labels inside
            val angleStep = (2 * PI / labels.size).toFloat()

            // Draw Web
            val steps = 5
            for (i in 1..steps) {
                val r = radius * (i / steps.toFloat())
                drawPath(
                    path = Path().apply {
                        for (j in labels.indices) {
                            val angle = -PI.toFloat() / 2 + j * angleStep
                            val x = center.x + r * cos(angle)
                            val y = center.y + r * sin(angle)
                            if (j == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    },
                    color = Color.LightGray.copy(alpha = 0.5f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw Spokes & Labels
            for (i in labels.indices) {
                val angle = -PI.toFloat() / 2 + i * angleStep
                val spokeEnd = Offset(
                    center.x + radius * cos(angle),
                    center.y + radius * sin(angle)
                )
                
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = center,
                    end = spokeEnd,
                    strokeWidth = 1.dp.toPx()
                )

                // Draw Label inside the chart
                val labelRadius = radius * 1.15f // Position labels just outside the main web
                val labelX = center.x + labelRadius * cos(angle)
                val labelY = center.y + labelRadius * sin(angle)
                
                val measuredText = textMeasurer.measure(labels[i], style = TextStyle(fontSize = 12.sp))
                drawText(
                    textMeasurer = textMeasurer,
                    text = labels[i],
                    topLeft = Offset(
                        labelX - measuredText.size.width / 2,
                        labelY - measuredText.size.height / 2
                    ),
                    style = TextStyle(color = labelColor, fontSize = 12.sp)
                )
            }

            // Draw Data Path
            val dataPath = Path().apply {
                 for (i in values.indices) {
                    val angle = -PI.toFloat() / 2 + i * angleStep
                    val normalizedValue = (values[i] / maxValue).coerceIn(0f, 1f)
                    val r = radius * normalizedValue
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            
            drawPath(path = dataPath, color = fillColor)
            drawPath(path = dataPath, color = lineColor, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
