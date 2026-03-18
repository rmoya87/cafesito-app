package com.cafesito.app.quicksettings

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import com.cafesito.app.brewlab.BrewLabTimerService

/**
 * Quick Settings Tile "Cafesito". Muestra "Cafesito" o "Ver elaboración" según si hay
 * timer de Brew Lab en curso (FGS activo). Al pulsar abre la app en Brew Lab
 * (brewlab?openConsumo=false); si hay elaboración en curso, BrewLabScreen restaura el estado
 * desde SharedPreferences.
 */
class CafesitoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BrewLabTimerService.EXTRA_OPEN_BREWLAB, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // En Android 14+ es obligatorio usar PendingIntent con startActivityAndCollapse
            startActivityAndCollapse(pendingIntent)
        } catch (_: UnsupportedOperationException) {
            // Fallback defensivo si algún OEM rompe el contrato
            startActivity(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isBrewing = BrewLabTimerService.isRunning(this)
        tile.label = if (isBrewing) getString(R.string.tile_label_brewing) else getString(R.string.app_name)
        tile.contentDescription =
            if (isBrewing) getString(R.string.tile_content_description_brewing)
            else getString(R.string.tile_content_description_default)
        tile.state = if (isBrewing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        // Logo transparente para la tile (drawable/ic_cafesito_tile.png)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_cafesito_tile)
        tile.updateTile()
    }
}
