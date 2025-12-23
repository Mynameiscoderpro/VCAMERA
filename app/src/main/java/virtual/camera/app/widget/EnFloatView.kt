package virtual.camera.app.widget

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Fixed: Removed FloatingMagnetView dependency
 * Now extends FrameLayout - floating rocker removed
 */
class EnFloatView(mContext: Context) : FrameLayout(mContext) {

    private var mListener: LocationListener? = null

    init {
        // Inflate layout removed - no longer depends on floatingview library
        // This view is now a simple frame layout stub
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    fun setListener(listener: LocationListener) {
        this.mListener = listener
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Floating rocker functionality removed
        return super.onTouchEvent(event)
    }
}

typealias LocationListener = (angle: Float, distance: Float) -> Unit