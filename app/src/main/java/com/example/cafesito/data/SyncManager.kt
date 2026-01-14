package com.example.cafesito.data

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository
) {
    suspend fun syncAll() {
        try {
            Log.d("SyncManager", "Iniciando sincronización global...")
            
            // 1. Sincronizar Catálogo de Cafés
            coffeeRepository.syncCoffees()
            
            // 2. Sincronizar Usuarios
            userRepository.syncUsers()
            
            // 3. Sincronizar Seguimientos
            userRepository.syncFollows()
            
            // 4. Sincronizar Actividad Social (Posts y Likes)
            socialRepository.syncSocialData()
            
            Log.d("SyncManager", "Sincronización completada con éxito.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Error durante la sincronización", e)
        }
    }
}
