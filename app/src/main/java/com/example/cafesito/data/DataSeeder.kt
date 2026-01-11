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
                if (userDao.getUserCount() == 0) {
                    val userEntities = allUsers.map { domainUser ->
                        UserEntity(
                            id = domainUser.id,
                            username = domainUser.username,
                            fullName = domainUser.fullName,
                            avatarUrl = domainUser.avatarUrl,
                            email = domainUser.email,
                            bio = domainUser.bio
                        )
                    }
                    userDao.insertUsers(userEntities)
                    userDao.insertFollow(FollowEntity(1, 2))
                    userDao.insertFollow(FollowEntity(1, 3))
                    userDao.insertFollow(FollowEntity(1, 4))
                }

                val existingCoffees = repository.allCoffees.firstOrNull()
                if (existingCoffees.isNullOrEmpty()) {
                    val inputStream = context.assets.open("cafes.csv")
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    reader.readLine() 

                    var line = reader.readLine()
                    while (line != null) {
                        val tokens = line.split(";")
                        if (tokens.size >= 27) {
                            try {
                                // NORMALIZACIÓN DE BRASIL (Brazil, Brazi, Brasil -> Brasil)
                                val rawCountry = tokens[3].trim()
                                val normalizedCountry = if (
                                    rawCountry.contains("brazi", ignoreCase = true) || 
                                    rawCountry.equals("brasil", ignoreCase = true)
                                ) "Brasil" else rawCountry

                                repository.insertCoffee(
                                    Coffee(
                                        id = tokens[0],
                                        especialidad = tokens[1],
                                        marca = tokens[2],
                                        paisOrigen = normalizedCountry,
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
                                        productUrl = tokens[26]
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("DataSeeder", "Error en línea CSV")
                            }
                        }
                        line = reader.readLine()
                    }
                    reader.close()
                }
            } catch (e: Exception) {
                Log.e("DataSeeder", "Error crítico", e)
            }
        }
    }
}
