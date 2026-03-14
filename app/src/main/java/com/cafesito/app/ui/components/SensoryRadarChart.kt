package com.cafesito.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cafesito.app.ui.theme.LocalCaramelAccent
import com.cafesito.app.ui.theme.RadarGridDark
import androidx.compose.ui.geometry.Rect
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

/** Orden fijo de ejes igual que webapp (Aroma, Sabor, Cuerpo, Acidez, Dulzura) para que la gráfica sea completa y consistente. */
private val RADAR_LABELS = listOf("Aroma", "Sabor", "Cuerpo", "Acidez", "Dulzura")

@Composable
fun SensoryRadarChart(
    data: Map<String, Float>,
    maxValue: Float = 10f,
    modifier: Modifier = Modifier,
    lineColor: Color = LocalCaramelAccent.current,
    fillColor: Color = LocalCaramelAccent.current.copy(alpha = 0.3f),
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    gridColor: Color = if (isSystemInDarkTheme()) RadarGridDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
) {
    val labels = RADAR_LABELS
    val values = RADAR_LABELS.map { key -> (data[key] ?: 0f).coerceIn(0f, maxValue) }
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

            // 3. Draw Data Path (Polígono de datos) — fórmulas igual que web: startAngle=-π/2, factor=value/10
            if (values.isNotEmpty()) {
                val startAngle = -PI.toFloat() / 2
                val dataPath = Path().apply {
                    for (i in values.indices) {
                        val angle = startAngle + i * angleStep
                        val factor = (values[i] / maxValue).coerceIn(0f, 1f)
                        val r = radius * factor
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

                // Puntos en cada vértice (igual que web: circle r=3 → ~5dp para que se vean)
                val pointRadiusPx = 5.dp.toPx()
                val pointStrokeWidthPx = 1.dp.toPx()
                for (i in values.indices) {
                    val angle = startAngle + i * angleStep
                    val factor = (values[i] / maxValue).coerceIn(0f, 1f)
                    val r = radius * factor
                    val px = center.x + r * cos(angle)
                    val py = center.y + r * sin(angle)
                    val pointCenter = Offset(px, py)
                    // Relleno sólido (como web profile-adn-radar-point)
                    drawCircle(color = lineColor, radius = pointRadiusPx, center = pointCenter)
                    // Borde para que destaquen en cualquier fondo
                    val oval = Path().apply {
                        addOval(Rect(pointCenter.x - pointRadiusPx, pointCenter.y - pointRadiusPx, pointCenter.x + pointRadiusPx, pointCenter.y + pointRadiusPx))
                    }
                    drawPath(oval, color = labelColor, style = Stroke(width = pointStrokeWidthPx))
                }
            }
        }
    }
}
