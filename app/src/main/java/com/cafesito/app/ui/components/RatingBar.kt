package com.cafesito.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafesito.app.R

@Composable
fun RatingBar(
    rating: Float,
    isInteractive: Boolean,
    starCount: Int = 5,
    starSize: Dp = 32.dp,
    onRatingChanged: ((Float) -> Unit)? = null
) {
    Row {
        for (i in 1..starCount) {
            val isSelected = i <= rating
            val icon = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            val modifier = if (isInteractive && onRatingChanged != null) Modifier.clickable { onRatingChanged(i.toFloat()) } else Modifier
            Icon(
                icon,
                contentDescription = stringResource(id = R.string.common_star_of_pattern, i, starCount),
                tint = tint,
                modifier = modifier.size(starSize)
            )
        }
    }
}
