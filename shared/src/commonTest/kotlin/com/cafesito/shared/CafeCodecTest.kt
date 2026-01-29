package com.cafesito.shared

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CafeCodecTest {
    @Test
    fun encodesAndDecodesCafeProfile() = runTest {
        val codec = CafeCodec()
        val profile = CafeProfile(name = "Cafesito", rating = 5)

        val payload = codec.encode(profile)
        val decoded = codec.decode(payload)

        assertEquals(profile, decoded)
    }
}
