package com.cafesito.shared.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual class PlatformHaptics {
    actual fun success() {
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
    }

    actual fun error() {
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium).impactOccurred()
    }

    actual fun critical() {
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy).impactOccurred()
    }
}

actual object PlatformNativeCapabilities {
    actual val supportsSignInWithApple: Boolean = true
    actual val supportsPasskeys: Boolean = true
    actual val supportsNativeCameraScanner: Boolean = true
}
