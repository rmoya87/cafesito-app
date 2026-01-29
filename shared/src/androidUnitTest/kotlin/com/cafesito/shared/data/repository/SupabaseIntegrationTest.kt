package com.cafesito.shared.data.repository

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import org.junit.Assume.assumeTrue
import org.junit.Test

class SupabaseIntegrationTest {
    @Test
    fun smokeTest_fetchCustomCoffees_whenConfigured() {
        val url = System.getenv("SUPABASE_URL")
        val key = System.getenv("SUPABASE_KEY")
        val runIntegration = System.getenv("RUN_SUPABASE_INTEGRATION_TESTS")
        assumeTrue(!url.isNullOrBlank() && !key.isNullOrBlank() && runIntegration == "true")

        val client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Auth)
        }

        val repository = SupabaseCafesitoRepository.fromClient(client)
        val result = kotlinx.coroutines.runBlocking {
            repository.getCustomCoffees(userId = 0)
        }

        assert(result is com.cafesito.shared.core.DataResult.Success)
    }
}
