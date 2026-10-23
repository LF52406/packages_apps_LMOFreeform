package com.libremobileos.freeform.server.ui

data class FreeformConfig @JvmOverloads constructor(
    var width: Int,
    var height: Int,
    var densityDpi: Int,
    var secure: Boolean = true,
    var ownContentOnly: Boolean = true,
    var shouldShowSystemDecorations: Boolean = false,
    var refreshRate: Float = 60.0f,
    var presentationDeadlineNanos: Long = 1666666L,
    var hangUpWidth: Int = 300,
    var hangUpHeight: Int = 400,
    var isHangUp: Boolean = false,
    // Saved centered window position for restoring the mini window
    var notInHangUpX: Int = 0,
    var notInHangUpY: Int = 0,
    // Logical freeform display size relative to the rendered content
    var freeformWidth: Int = 1080,
    var freeformHeight: Int = 1920,
    // Logical display pixels per visible content pixel
    var scale: Float = 1.0f
)
