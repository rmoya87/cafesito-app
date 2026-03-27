package com.cafesito.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cafesito.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    availableOrigins: List<String>,
    currentMinScore: Float,
    currentOrigin: String?, // null = All
    onDismiss: () -> Unit,
    onApply: (minScore: Float, origin: String?) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentMinScore) }
    var selectedOrigin by remember { mutableStateOf(currentOrigin) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.search_filter_dialog_title)) },
        text = {
            Column {
                Text(stringResource(id = R.string.search_filter_dialog_min_score, sliderValue.toInt()))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..100f,
                    steps = 100,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.outline,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline,
                        activeTickColor = MaterialTheme.colorScheme.outline,
                        inactiveTickColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(id = R.string.search_filter_origin))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedOrigin ?: stringResource(id = R.string.search_filter_all_countries),
                        onValueChange = {},
                        readOnly = true,
                        shape = CircleShape,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.search_filter_all_countries)) },
                            onClick = {
                                selectedOrigin = null
                                expanded = false
                            }
                        )
                        availableOrigins.forEach { origin ->
                            DropdownMenuItem(
                                text = { Text(origin) },
                                onClick = {
                                    selectedOrigin = origin
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(sliderValue, selectedOrigin) }) {
                Text(stringResource(id = R.string.common_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.search_cancel))
            }
        }
    )
}
