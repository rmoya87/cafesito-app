package com.cafesito.app.data

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository
) {
    // ✅ OPTIMIZACIÓN: Control de intervalo mínimo entre sincronizaciones
    private var _lastSyncTime: Long = 0
    private val MIN_SYNC_INTERVAL = 10 * 60 * 1000L // 10 minutos
    
    // ✅ OPTIMIZACIÓN: Evitar sincronizaciones simultáneas
    private val syncLock = Mutex()

    suspend fun syncAll(force: Boolean = false) {
        if (syncLock.isLocked) {
            Log.d("SyncManager", "Sincronización ya en curso, ignorando llamada.")
            return
        }

        syncLock.withLock {
            val now = System.currentTimeMillis()
            
            // Evitar sincronizaciones muy frecuentes (a menos que sea forzado)
            if (!force && (now - _lastSyncTime) < MIN_SYNC_INTERVAL) {
                Log.d("SyncManager", "Sincronización saltada (última hace ${(now - _lastSyncTime) / 1000}s)")
                return
            }
            
            try {
                Log.d("SyncManager", "Iniciando sincronización global paralela...")
                
                // ✅ OPTIMIZACIÓN: Paralelizar TODAS las sincronizaciones
                coroutineScope {
                    awaitAll(
                        async { coffeeRepository.syncCoffees() },
                        async { userRepository.syncUsers() },
                        async { userRepository.syncFollows() },
                        async { socialRepository.syncSocialData() }
                    )
                }
                
                _lastSyncTime = now
                Log.d("SyncManager", "Sincronización completada en paralelo.")
            } catch (e: Exception) {
                Log.e("SyncManager", "Error durante la sincronización", e)
            }
        }
    }
}
