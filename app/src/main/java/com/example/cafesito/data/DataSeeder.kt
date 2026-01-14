package com.example.cafesito.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coffeeDao: CoffeeDao,
    private val supabaseDataSource: SupabaseDataSource
) {
    suspend fun seedIfNeeded() {
        try {
            // Verificamos si Supabase tiene cafés
            val remoteCoffees = supabaseDataSource.getAllCoffees()
            
            if (remoteCoffees.isEmpty()) {
                Log.d("DataSeeder", "Supabase está vacío. Iniciando migración de cafés...")
                migrateCsvToSupabase()
            } else {
                Log.d("DataSeeder", "Supabase ya tiene cafés. Sincronización delegada al SyncManager.")
            }
        } catch (e: Exception) {
            Log.e("DataSeeder", "Error durante el seeding", e)
        }
    }

    private suspend fun migrateCsvToSupabase() {
        val coffeesToUpload = mutableListOf<Coffee>()
        try {
            val inputStream = context.assets.open("cafes.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Omitir cabecera
            
            val csvRegex = ";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) continue
                val tokens = line!!.split(csvRegex).map { it.trim().removeSurrounding("\"") }
                if (tokens.size < 26) continue

                coffeesToUpload.add(
                    Coffee(
                        id = tokens[0],
                        especialidad = tokens[1],
                        marca = tokens[2],
                        paisOrigen = tokens[3],
                        variedadTipo = tokens[4],
                        nombre = tokens[5],
                        descripcion = tokens[6],
                        fuentePuntuacion = tokens[7],
                        puntuacionOficial = tokens[8].toDoubleOrNull() ?: 0.0,
                        notasCata = tokens[9],
                        formato = tokens[10],
                        cafeina = tokens[11],
                        tueste = tokens[12],
                        proceso = tokens[13],
                        ratioRecomendado = tokens[14],
                        moliendaRecomendada = tokens[15],
                        aroma = tokens[16].toFloatOrNull() ?: 0f,
                        sabor = tokens[17].toFloatOrNull() ?: 0f,
                        retrogusto = tokens[18].toFloatOrNull() ?: 0f,
                        acidez = tokens[19].toFloatOrNull() ?: 0f,
                        cuerpo = tokens[20].toFloatOrNull() ?: 0f,
                        uniformidad = tokens[21].toFloatOrNull() ?: 0f,
                        dulzura = tokens[22].toFloatOrNull() ?: 0f,
                        puntuacionTotal = tokens[23].toDoubleOrNull() ?: 0.0,
                        codigoBarras = tokens[24],
                        imageUrl = tokens[25],
                        productUrl = if (tokens.size > 26) tokens[26] else ""
                    )
                )
            }
            reader.close()

            // Subir a Supabase
            if (coffeesToUpload.isNotEmpty()) {
                supabaseDataSource.upsertCoffees(coffeesToUpload)
                Log.d("DataSeeder", "Se han subido ${coffeesToUpload.size} cafés a Supabase.")
            }

        } catch (e: Exception) {
            Log.e("DataSeeder", "Error procesando el CSV para migración", e)
        }
    }
}
