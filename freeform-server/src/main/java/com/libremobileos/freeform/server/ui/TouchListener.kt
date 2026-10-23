package com.libremobileos.freeform.server.ui

import android.annotation.SuppressLint
import android.os.Build
import android.util.Slog
import android.view.Display
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import kotlin.math.abs
import com.libremobileos.freeform.server.LMOFreeformServiceHolder
import com.libremobileos.freeform.server.SystemServiceHolder
import kotlin.math.roundToInt

class MoveTouchListener(
    private val window: FreeformWindow,
) : View.OnTouchListener {
    private var lastX = 0f
    private var lastY = 0f
    private var velocityTracker: VelocityTracker? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val layout = window.freeformLayout ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                lastX = event.rawX
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastX
                val dy = event.rawY - lastY
                if (!dx.isFinite() || !dy.isFinite()) return true
                velocityTracker?.addMovement(event)
                window.windowParams.apply {
                    x = (x + dx).roundToInt()
                    y = (y + dy).roundToInt()
                }
                window.clampWindowPosition()
                window.windowManager.updateViewLayout(layout, window.windowParams)
                lastX = event.rawX
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val vx = velocityTracker?.xVelocity ?: 0f
                val density = window.context.resources.displayMetrics.density
                val size = window.freeformConfig
                val edge = (window.defaultDisplayWidth - size.width) / 2
                val nearEdge = abs(window.windowParams.x) >= edge - 36 * density
                val flickToSide = nearEdge && abs(vx) >= 850f * density &&
                    vx * window.windowParams.x > 0
                velocityTracker?.recycle()
                velocityTracker = null
                if (flickToSide) window.handleHangUp()
                else window.makeSureFreeformInScreen()
                v.performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                window.makeSureFreeformInScreen()
                return true
            }
        }
        return false
    }
}

class LeftViewClickListener(private val window: FreeformWindow) : View.OnClickListener {
    override fun onClick(v: View) {
        window.close()
    }

}

/**
 * maximize freeform screen
 */
class MaximizeClickListener(private val window: FreeformWindow): View.OnClickListener {
    companion object {
        private const val TAG = "LMOFreeform/TouchListener"
    }
    override fun onClick(v: View) {
        if (null != window.freeformTaskStackListener) {
            if (window.freeformTaskStackListener!!.taskId == -1) {
                Slog.e(TAG, "taskId is -1, can`t move")
                return
            }
            runCatching { SystemServiceHolder.activityTaskManager.moveRootTaskToDisplay(window.freeformTaskStackListener!!.taskId, Display.DEFAULT_DISPLAY) }
        }
    }
}

/**
 * Pin freeform
 */
class PinClickListener(private val window: FreeformWindow): View.OnClickListener {
    override fun onClick(v: View) {
        window.handler.post {
            // hangup
            window.handleHangUp()
        }
    }
}

class RightViewClickListener(private val displayId: Int) : View.OnClickListener {
    override fun onClick(v: View) {
        LMOFreeformServiceHolder.back(displayId)
    }
}

class ScaleTouchListener(
    private val window: FreeformWindow,
    private val isRight: Boolean = true,
) : View.OnTouchListener {
    private var startRawX = 0f
    private var startRawY = 0f
    private var startX = 0
    private var startY = 0
    private var startSize = FreeformGeometry.Size(1, 1)
    private var pendingSize = startSize

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val root = window.freeformRootView ?: return false
        if (window.freeformConfig.isHangUp) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startRawX = event.rawX
                startRawY = event.rawY
                startX = window.windowParams.x
                startY = window.windowParams.y
                startSize = FreeformGeometry.Size(
                    window.freeformConfig.width, window.freeformConfig.height
                )
                pendingSize = startSize
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - startRawX
                val dy = event.rawY - startRawY
                if (!dx.isFinite() || !dy.isFinite()) return true
                val size = FreeformGeometry.resize(
                    startSize, dx, dy, isRight,
                    window.defaultDisplayWidth, window.defaultDisplayHeight,
                    window.context.resources.displayMetrics.density.coerceAtLeast(0.1f)
                )
                pendingSize = size
                root.layoutParams = root.layoutParams.apply {
                    width = size.width
                    height = size.height
                }
                // Anchor the opposite edge instead of growing around the center.
                val shift = (size.width - startSize.width) / 2
                window.windowParams.x = startX + if (isRight) shift else -shift
                window.windowParams.y = startY + (size.height - startSize.height) / 2
                window.clampWindowPosition(size.width, size.height)
                window.freeformLayout?.let {
                    window.windowManager.updateViewLayout(it, window.windowParams)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                window.finishResize(pendingSize)
                v.performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                root.layoutParams = root.layoutParams.apply {
                    width = startSize.width
                    height = startSize.height
                }
                window.windowParams.x = startX
                window.windowParams.y = startY
                window.freeformLayout?.let {
                    window.windowManager.updateViewLayout(it, window.windowParams)
                }
                return true
            }
        }
        return false
    }
}

class HangUpGestureListener(private val window: FreeformWindow) : SimpleOnGestureListener() {
    private var startX = 0
    private var startY = 0
    override fun onDown(e: MotionEvent): Boolean {
        startX = window.windowParams.x
        startY = window.windowParams.y
        return super.onDown(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        window.handler.post { window.handleHangUp() }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!isValidMotionEvent(e1) || !isValidMotionEvent(e2)) {
            return true
        }

        val e1RawX = e1?.rawX ?: 0f
        val e1RawY = e1?.rawY ?: 0f

        if (!isValidCoordinate(e1RawX) || !isValidCoordinate(e1RawY) 
                || !isValidCoordinate(e2.rawX) || !isValidCoordinate(e2.rawY)) {
            return true
        }
        
        val newX = (startX + e2.rawX - e1RawX).roundToInt()
        val newY = (startY + e2.rawY - e1RawY).roundToInt()

        try {
            window.handler.post {
                val layout = window.freeformLayout ?: return@post
                window.windowParams.apply {
                    x = newX
                    y = newY
                }
                window.clampWindowPosition()
                window.windowManager.updateViewLayout(layout, window.windowParams)
            }
        } catch (e: Exception) {}
        return true
    }

    fun isValidMotionEvent(event: MotionEvent?): Boolean {
        return event != null &&
                !event.rawX.isNaN() &&
                !event.rawY.isNaN() &&
                event.rawX.isFinite() &&
                event.rawY.isFinite()
    }
    
    fun isValidCoordinate(coordinate: Float): Boolean {
        return !coordinate.isNaN() && coordinate.isFinite()
    }
}
