package com.example.cafesito.data

import android.content.Context
import android.util.Log
import com.example.cafesito.domain.allUsers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CoffeeRepository,
    private val userDao: UserDao
) {
    fun seedIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Seed Usuarios y Seguimientos
                if (userDao.getUserCount() == 0) {
                    userDao.insertUsers(allUsers.map { it.toEntity() })
                    userDao.insertFollow(FollowEntity(1, 2))
                    userDao.insertFollow(FollowEntity(1, 3))
                    userDao.insertFollow(FollowEntity(1, 4))
                    userDao.insertFollow(FollowEntity(1, 5))
                    Log.d("DataSeeder", "Usuarios inicializados.")
                }

                // 2. Verificar si ya hay cafés para evitar re-importar todo
                val existingCoffees = repository.allCoffees.firstOrNull()
                if (!existingCoffees.isNullOrEmpty()) {
                    Log.d("DataSeeder", "Base de datos de cafés ya poblada.")
                    return@launch
                }

                val scaSourceId = repository.insertScoreSource(ScoreSource(name = "SCA", url = "https://sca.coffee")).toInt()
                val originMap = mutableMapOf<String, Int>()
                val seenCoffees = mutableSetOf<String>()

                val csvFiles = listOf(
                    "merged_data_cleaned.csv",
                    "robusta_data_cleaned.csv",
                    "arabica_data_cleaned.csv",
                    "df_arabica_clean.csv"
                )

                csvFiles.forEach { fileName ->
                    try {
                        importCsvFile(fileName, scaSourceId, originMap, seenCoffees)
                    } catch (e: Exception) {
                        Log.e("DataSeeder", "Error importando $fileName", e)
                    }
                }

                Log.d("DataSeeder", "Importación masiva completada con éxito.")
            } catch (e: Exception) {
                Log.e("DataSeeder", "Error crítico en seeder", e)
            }
        }
    }

    private suspend fun importCsvFile(
        fileName: String,
        scaSourceId: Int,
        originMap: MutableMap<String, Int>,
        seenCoffees: MutableSet<String>
    ) {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val headerLine = reader.readLine() ?: return
        val headers = parseCsvLine(headerLine)

        // Mapeo dinámico de índices basado en nombres de columnas
        val idxCountry = headers.indexOfFirst { it.contains("Country", true) }
        val idxFarm = headers.indexOfFirst { it.contains("Farm.Name", true) || it.contains("Farm Name", true) }
        val idxOwner = headers.indexOfFirst { it.contains("Owner", true) || it.contains("Company", true) }
        val idxProcess = headers.indexOfFirst { it.contains("Processing", true) }
        val idxAroma = headers.indexOfFirst { it.contains("Aroma", true) }
        val idxFlavor = headers.indexOfFirst { it.contains("Flavor", true) }
        val idxAftertaste = headers.indexOfFirst { it.contains("Aftertaste", true) }
        val idxAcidity = headers.indexOfFirst { it.contains("Acid", true) }
        val idxBody = headers.indexOfFirst { it.contains("Body", true) || it.contains("Mouthfeel", true) }
        val idxBalance = headers.indexOfFirst { it.contains("Balance", true) }
        val idxUniformity = headers.indexOfFirst { it.contains("Uniformity", true) || it.contains("Uniform", true) }
        val idxCleanCup = headers.indexOfFirst { it.contains("Clean", true) }
        val idxSweetness = headers.indexOfFirst { it.contains("Sweet", true) }
        val idxScore = headers.indexOfFirst { it.contains("Total.Cup.Points", true) || it.contains("Total Cup Points", true) }
        val idxVariety = headers.indexOfFirst { it.contains("Variety", true) }
        val idxRegion = headers.indexOfFirst { it.contains("Region", true) }

        var line = reader.readLine()
        while (line != null) {
            val tokens = parseCsvLine(line!!)
            if (tokens.size >= headers.size) {
                val farmName = tokens.getOrNull(idxFarm)?.ifBlank { null } ?: tokens.getOrNull(idxOwner)?.ifBlank { "Specialty Coffee" } ?: "Unknown Farm"
                val owner = tokens.getOrNull(idxOwner) ?: "Independent"
                val uniqueKey = "$farmName|$owner".lowercase()

                // Evitar duplicados exactos entre archivos
                if (!seenCoffees.contains(uniqueKey)) {
                    seenCoffees.add(uniqueKey)

                    val country = tokens.getOrNull(idxCountry)?.ifBlank { "Global" } ?: "Global"
                    val originId = originMap.getOrPut(country) {
                        repository.insertOrigin(Origin(countryName = country, continent = "Global")).toInt()
                    }

                    val coffeeId = repository.insertCoffee(
                        Coffee(
                            name = farmName,
                            brandRoaster = owner,
                            originId = originId,
                            process = tokens.getOrNull(idxProcess) ?: "Washed",
                            roastLevel = "Medium",
                            description = "Variety: ${tokens.getOrNull(idxVariety) ?: "Unknown"}, Region: ${tokens.getOrNull(idxRegion) ?: "Unknown"}",
                            imageUrl = "https://picsum.photos/seed/${uniqueKey.hashCode()}/400/300",
                            officialScore = tokens.getOrNull(idxScore)?.toDoubleOrNull() ?: 0.0,
                            scoreSourceId = scaSourceId
                        )
                    ).toInt()

                    repository.insertSensoryProfile(
                        SensoryProfile(
                            coffeeId = coffeeId,
                            aroma = tokens.getOrNull(idxAroma)?.toFloatOrNull() ?: 0f,
                            flavor = tokens.getOrNull(idxFlavor)?.toFloatOrNull() ?: 0f,
                            aftertaste = tokens.getOrNull(idxAftertaste)?.toFloatOrNull() ?: 0f,
                            acidity = tokens.getOrNull(idxAcidity)?.toFloatOrNull() ?: 0f,
                            body = tokens.getOrNull(idxBody)?.toFloatOrNull() ?: 0f,
                            balance = tokens.getOrNull(idxBalance)?.toFloatOrNull() ?: 0f,
                            freshness = tokens.getOrNull(idxUniformity)?.toFloatOrNull() ?: 0f,
                            clarity = tokens.getOrNull(idxCleanCup)?.toFloatOrNull() ?: 0f,
                            sweetness = tokens.getOrNull(idxSweetness)?.toFloatOrNull() ?: 0f
                        )
                    )
                }
            }
            line = reader.readLine()
        }
        reader.close()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        for (ch in line.toCharArray()) {
            if (inQuotes) {
                if (ch == '\"') inQuotes = false else curVal.append(ch)
            } else {
                if (ch == '\"') inQuotes = true
                else if (ch == ',') {
                    result.add(curVal.toString().trim())
                    curVal = StringBuilder()
                } else curVal.append(ch)
            }
        }
        result.add(curVal.toString().trim())
        return result
    }
}
