package com.example.cafesito.data

import kotlinx.coroutines.flow.firstOrNull
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    private val repository: CoffeeRepository
) {
    fun seedIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentOrigins = repository.origins.firstOrNull()
                if (!currentOrigins.isNullOrEmpty()) {
                    Log.d("DataSeeder", "Database already seeded, skipping.")
                    return@launch
                }

                // Origins
                val ethId = repository.insertOrigin(Origin(countryName = "Ethiopia", continent = "Africa"))
                val colId = repository.insertOrigin(Origin(countryName = "Colombia", continent = "South America"))
                val braId = repository.insertOrigin(Origin(countryName = "Brazil", continent = "South America"))

                // Sources
                val scaId = repository.insertScoreSource(ScoreSource(name = "SCA", url = "https://sca.coffee"))

                // Coffees
                // 1. Ethiopia Yirgacheffe
                val coffee1Id = repository.insertCoffee(
                    Coffee(
                        name = "Yirgacheffe G1",
                        brandRoaster = "Garden Coffee",
                        originId = ethId.toInt(),
                        process = "Washed",
                        roastLevel = "Light",
                        description = "Floral notes of jasmine and bergamot.",
                        imageUrl = "https://images.unsplash.com/photo-1497935586351-b67a49e012bf?auto=format&fit=crop&w=400&q=80",
                        officialScore = 88.5,
                        scoreSourceId = scaId.toInt()
                    )
                )
                repository.insertSensoryProfile(
                    SensoryProfile(coffeeId = coffee1Id.toInt(), aroma = 9.0f, flavor = 8.8f, body = 6.0f, acidity = 8.5f, aftertaste = 8.0f, balance = 8.5f, sweetness = 8.2f, clarity = 9.0f, freshness = 8.0f)
                )

                // 2. Colombia Huila
                val coffee2Id = repository.insertCoffee(
                    Coffee(
                        name = "Huila Supremo",
                        brandRoaster = "Andes Roasters",
                        originId = colId.toInt(),
                        process = "Washed",
                        roastLevel = "Medium",
                        description = "Balanced with caramel and fruit notes.",
                        imageUrl = "https://images.unsplash.com/photo-1559525839-4f34240be8b5?auto=format&fit=crop&w=400&q=80",
                        officialScore = 85.0,
                        scoreSourceId = scaId.toInt()
                    )
                )
                repository.insertSensoryProfile(
                    SensoryProfile(coffeeId = coffee2Id.toInt(), aroma = 8.0f, flavor = 8.2f, body = 7.5f, acidity = 7.0f, aftertaste = 7.8f, balance = 8.5f, sweetness = 8.5f, clarity = 7.5f, freshness = 9.0f)
                )

                 // 3. Brazil Santos
                val coffee3Id = repository.insertCoffee(
                    Coffee(
                        name = "Santos Bourbon",
                        brandRoaster = "Yellow Tuccan",
                        originId = braId.toInt(),
                        process = "Natural",
                        roastLevel = "Medium-Dark",
                        description = "Nutty, chocolatey, low acidity.",
                        imageUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=400&q=80",
                        officialScore = 82.5,
                        scoreSourceId = scaId.toInt()
                    )
                )
                repository.insertSensoryProfile(
                    SensoryProfile(coffeeId = coffee3Id.toInt(), aroma = 7.5f, flavor = 7.8f, body = 9.0f, acidity = 5.5f, aftertaste = 8.0f, balance = 8.0f, sweetness = 8.8f, clarity = 6.5f, freshness = 7.5f)
                )
                
                Log.d("DataSeeder", "Seeding completed.")
            } catch (e: Exception) {
                Log.e("DataSeeder", "Error seeding data", e)
            }
        }
    }
}
