package com.cafesito.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cafesito.app.R
import com.cafesito.app.ui.theme.OrangeYellow

@Composable
fun SemicircleRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val newRating = (change.position.x / size.width * 5).coerceIn(0f, 5f)
                    onRatingChanged(newRating)
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val isFilled = i <= rating
            val isHalf = !isFilled && (i - 0.5f) <= rating
            
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = stringResource(id = R.string.common_rating_of_five_pattern, i),
                modifier = Modifier
                    .size(48.dp)
                    .clickableWithoutRipple { onRatingChanged(i.toFloat()) },
                tint = if (isFilled || isHalf) {
                    OrangeYellow 
                } else {
                    if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)
