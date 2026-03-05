package com.cafesito.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cafesito.app.ui.theme.LocalCaramelAccent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SensoryRadarChart(
    data: Map<String, Float>,
    maxValue: Float = 10f,
    modifier: Modifier = Modifier,
    lineColor: Color = LocalCaramelAccent.current,
    fillColor: Color = LocalCaramelAccent.current.copy(alpha = 0.3f),
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    gridColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
) {
    val labels = data.keys.toList()
    val values = data.values.toList()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 10.sp) // Reducido un poco para que entre mejor

    Box(modifier = modifier
        .aspectRatio(1f)
        .padding(32.dp) // Aumentado padding para que no se corten etiquetas
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = min(size.width, size.height) / 2f * 0.75f // Reducido radio del gráfico para dejar espacio a etiquetas
            val angleStep = (2 * PI / labels.size).toFloat()

            // 1. Draw Web (Red de fondo)
            val steps = 5
            for (i in 1..steps) {
                val r = radius * (i / steps.toFloat())
                val webPath = Path().apply {
                    for (j in labels.indices) {
                        val angle = -PI.toFloat() / 2 + j * angleStep
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (j == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = webPath,
                    color = gridColor,
                    style = Stroke(width = 0.5.dp.toPx())
                )
            }

            // 2. Draw Spokes and Labels (Ejes y Nombres)
            for (i in labels.indices) {
                val angle = -PI.toFloat() / 2 + i * angleStep
                val cosA = cos(angle)
                val sinA = sin(angle)

                // Eje vertical
                drawLine(
                    color = gridColor,
                    start = center,
                    end = Offset(center.x + radius * cosA, center.y + radius * sinA),
                    strokeWidth = 1.dp.toPx()
                )

                // Posicionamiento inteligente de etiquetas
                val labelRadius = radius + 12.dp.toPx()
                val labelX = center.x + labelRadius * cosA
                val labelY = center.y + labelRadius * sinA
                
                val textLayout = textMeasurer.measure(labels[i], style = labelStyle)
                
                drawText(
                    textMeasurer = textMeasurer,
                    text = labels[i],
                    topLeft = Offset(
                        labelX - textLayout.size.width / 2,
                        labelY - textLayout.size.height / 2
                    ),
                    style = labelStyle
                )
            }

            // 3. Draw Data Path (Polígono de datos)
            if (values.isNotEmpty()) {
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
                drawPath(
                    path = dataPath, 
                    color = lineColor, 
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Puntos de datos (Vértices)
                for (i in values.indices) {
                    val angle = -PI.toFloat() / 2 + i * angleStep
                    val r = radius * (values[i] / maxValue).coerceIn(0f, 1f)
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
                    )
                }
            }
        }
    }
}
