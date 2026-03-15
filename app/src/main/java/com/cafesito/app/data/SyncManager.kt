package com.cafesito.app.data

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val diaryRepository: DiaryRepository
) {
    // Control de intervalo mínimo entre sincronizaciones
    private var _lastSyncTime: Long = 0
    private var _lastCoffeesSyncTime: Long = 0
    private var _lastUsersSyncTime: Long = 0
    private val MIN_SYNC_INTERVAL_MS = 10 * 60 * 1000L // 10 minutos
    private val RETRY_DELAY_MS = 2_000L

    private val syncLock = Mutex()

    /**
     * Sincroniza cafés desde Supabase con intervalo mínimo y reintento en error.
     * Fuente de verdad: Supabase; en fallo se reintenta una vez.
     */
    suspend fun syncCoffeesIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - _lastCoffeesSyncTime) < MIN_SYNC_INTERVAL_MS) {
            Log.d("SyncManager", "syncCoffeesIfNeeded: saltado (intervalo)")
            return
        }
        runWithRetry(
            block = {
                coffeeRepository.syncCoffees()
                _lastCoffeesSyncTime = System.currentTimeMillis()
            },
            onSuccess = { Log.d("SyncManager", "syncCoffees completado.") },
            onFailure = { e -> Log.e("SyncManager", "syncCoffees falló tras reintento", e) }
        )
    }

    /**
     * Sincroniza usuarios desde Supabase con intervalo mínimo y reintento en error.
     * Fuente de verdad: Supabase; en fallo se reintenta una vez.
     */
    suspend fun syncUsersIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - _lastUsersSyncTime) < MIN_SYNC_INTERVAL_MS) {
            Log.d("SyncManager", "syncUsersIfNeeded: saltado (intervalo)")
            return
        }
        runWithRetry(
            block = {
                userRepository.syncUsers()
                _lastUsersSyncTime = System.currentTimeMillis()
            },
            onSuccess = { Log.d("SyncManager", "syncUsers completado.") },
            onFailure = { e -> Log.e("SyncManager", "syncUsers falló tras reintento", e) }
        )
    }

    /**
     * Sincroniza favoritos desde remoto con reintento. Fuente de verdad: Supabase.
     */
    suspend fun syncFavoritesIfNeeded(force: Boolean = false) {
        runWithRetry(
            block = { coffeeRepository.syncFavoritesFromRemote() },
            onSuccess = { },
            onFailure = { e -> Log.e("SyncManager", "syncFavorites falló tras reintento", e) }
        )
    }

    private suspend fun runWithRetry(
        block: suspend () -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        try {
            block()
            onSuccess()
        } catch (e: Exception) {
            Log.w("SyncManager", "Primer intento falló, reintentando en ${RETRY_DELAY_MS}ms: ${e.message}")
            delay(RETRY_DELAY_MS)
            try {
                block()
                onSuccess()
            } catch (e2: Exception) {
                onFailure(e2)
            }
        }
    }

    /**
     * Sync imprescindible para la pantalla inicial (Timeline/Home): usuarios, seguimientos, cafés y favoritos.
     * Se ejecuta tras login para que la primera pantalla tenga datos sin esperar al sync completo.
     */
    suspend fun syncEssentialForLaunch() {
        if (syncLock.isLocked) {
            Log.d("SyncManager", "Sync en curso, saltando essential.")
            return
        }
        syncLock.withLock {
            try {
                Log.d("SyncManager", "Sync esencial para pantalla inicial...")
                coroutineScope {
                    awaitAll(
                        async { userRepository.syncUsers() },
                        async { userRepository.syncFollows() },
                        async { coffeeRepository.syncCoffees() },
                        async { coffeeRepository.syncFavoritesFromRemote() }
                    )
                }
                _lastCoffeesSyncTime = System.currentTimeMillis()
                _lastUsersSyncTime = _lastCoffeesSyncTime
                Log.d("SyncManager", "Sync esencial completado.")
            } catch (e: Exception) {
                Log.e("SyncManager", "Error en sync esencial, reintentando...", e)
                delay(RETRY_DELAY_MS)
                try {
                    coroutineScope {
                        awaitAll(
                            async { userRepository.syncUsers() },
                            async { userRepository.syncFollows() },
                            async { coffeeRepository.syncCoffees() },
                            async { coffeeRepository.syncFavoritesFromRemote() }
                        )
                    }
                    _lastCoffeesSyncTime = System.currentTimeMillis()
                    _lastUsersSyncTime = _lastCoffeesSyncTime
                } catch (e2: Exception) {
                    Log.e("SyncManager", "Sync esencial falló tras reintento", e2)
                }
            }
        }
    }

    /**
     * Sync diferido (social, despensa, diario pendiente). Llamar tras login con delay o al cambiar a pestaña que lo necesite.
     */
    suspend fun syncDeferred() {
        if (syncLock.isLocked) return
        syncLock.withLock {
            try {
                Log.d("SyncManager", "Sync diferido (social, despensa, diario)...")
                coroutineScope {
                    awaitAll(
                        async { socialRepository.syncSocialData() },
                        async { diaryRepository.syncPantryItems() },
                        async { diaryRepository.syncPendingDiaryEntries() }
                    )
                }
                Log.d("SyncManager", "Sync diferido completado.")
            } catch (e: Exception) {
                Log.e("SyncManager", "Sync diferido falló", e)
            }
        }
    }

    suspend fun syncAll(force: Boolean = false) {
        if (syncLock.isLocked) {
            Log.d("SyncManager", "Sincronización ya en curso, ignorando llamada.")
            return
        }

        syncLock.withLock {
            val now = System.currentTimeMillis()
            if (!force && (now - _lastSyncTime) < MIN_SYNC_INTERVAL_MS) {
                Log.d("SyncManager", "Sincronización saltada (última hace ${(now - _lastSyncTime) / 1000}s)")
                return@withLock
            }
            try {
                Log.d("SyncManager", "Iniciando sincronización global paralela...")
                coroutineScope {
                    awaitAll(
                        async { coffeeRepository.syncCoffees() },
                        async { coffeeRepository.syncFavoritesFromRemote() },
                        async { userRepository.syncUsers() },
                        async { userRepository.syncFollows() },
                        async { socialRepository.syncSocialData() },
                        async { diaryRepository.syncPantryItems() },
                        async { diaryRepository.syncPendingDiaryEntries() }
                    )
                }
                _lastSyncTime = System.currentTimeMillis()
                _lastCoffeesSyncTime = _lastSyncTime
                _lastUsersSyncTime = _lastSyncTime
                Log.d("SyncManager", "Sincronización completada en paralelo.")
            } catch (e: Exception) {
                Log.e("SyncManager", "Error durante la sincronización, reintentando...", e)
                delay(RETRY_DELAY_MS)
                try {
                    coroutineScope {
                        awaitAll(
                            async { coffeeRepository.syncCoffees() },
                            async { coffeeRepository.syncFavoritesFromRemote() },
                            async { userRepository.syncUsers() },
                            async { userRepository.syncFollows() },
                            async { socialRepository.syncSocialData() },
                            async { diaryRepository.syncPantryItems() },
                            async { diaryRepository.syncPendingDiaryEntries() }
                        )
                    }
                    _lastSyncTime = System.currentTimeMillis()
                    _lastCoffeesSyncTime = _lastSyncTime
                    _lastUsersSyncTime = _lastSyncTime
                    Log.d("SyncManager", "Sincronización completada tras reintento.")
                } catch (e2: Exception) {
                    Log.e("SyncManager", "Error durante la sincronización tras reintento", e2)
                }
            }
        }
    }
}
