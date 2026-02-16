package com.cafesito.shared.platform

expect class PlatformHaptics {
    fun success()
    fun error()
    fun critical()
}

expect object PlatformNativeCapabilities {
    val supportsSignInWithApple: Boolean
    val supportsPasskeys: Boolean
    val supportsNativeCameraScanner: Boolean
}
