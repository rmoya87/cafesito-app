package com.cafesito.shared.platform

actual class PlatformHaptics {
    actual fun success() = Unit
    actual fun error() = Unit
    actual fun critical() = Unit
}

actual object PlatformNativeCapabilities {
    actual val supportsSignInWithApple: Boolean = false
    actual val supportsPasskeys: Boolean = true
    actual val supportsNativeCameraScanner: Boolean = true
}
