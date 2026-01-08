package com.example.cafesito.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cafesito.data.Origin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    availableOrigins: List<Origin>,
    currentMinScore: Float,
    currentOriginId: Int?, // null = All
    onDismiss: () -> Unit,
    onApply: (minScore: Float, originId: Int?) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentMinScore) }
    var selectedOrigin by remember {
        mutableStateOf(availableOrigins.find { it.id == currentOriginId })
    }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar Cafés") },
        text = {
            Column {
                // Score Filter
                Text("Puntuación Mínima: ${sliderValue.toInt()}")
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 70f..100f,
                    steps = 29
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Origin Filter
                Text("Origen")
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedOrigin?.countryName ?: "Todos los países",
                        onValueChange = {},
                        readOnly = true,
                        shape = CircleShape,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos los países") },
                            onClick = {
                                selectedOrigin = null
                                expanded = false
                            }
                        )
                        availableOrigins.forEach { origin ->
                            DropdownMenuItem(
                                text = { Text(origin.countryName) },
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
            Button(onClick = { onApply(sliderValue, selectedOrigin?.id) }) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
