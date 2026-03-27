package com.cafesito.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cafesito.app.R
import com.cafesito.app.ui.components.GlassyTopBar
import com.cafesito.app.ui.components.PremiumCard
import com.cafesito.app.ui.theme.AppLanguageManager

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LanguageSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var languagePreference by remember { mutableStateOf(AppLanguageManager.getLanguagePreference(context)) }

    val items = listOf(
        Triple(AppLanguageManager.SYSTEM, stringResource(R.string.language_system), ""),
        Triple("es", stringResource(R.string.language_spanish), ""),
        Triple("en", stringResource(R.string.language_english), ""),
        Triple("fr", stringResource(R.string.language_french), ""),
        Triple("pt", stringResource(R.string.language_portuguese), ""),
        Triple("de", stringResource(R.string.language_german), "")
    )

    Scaffold(
        topBar = {
            GlassyTopBar(
                title = stringResource(R.string.language_title).uppercase(),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.first }) { (code, title, description) ->
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            languagePreference = code
                            AppLanguageManager.setLanguagePreference(context, code)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                            )
                            if (languagePreference == code) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = title,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Sin subtítulo para "Sistema" (requisito UX).
                    }
                }
            }
        }
    }
}

