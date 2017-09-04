package vkm.vkm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout

// TODO consider redo to a fragments, not activities
class SwipeCatcher @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val SWIPE_DISTANCE_MIN = 200

    var left: Class<out Activity>? = null
    var right: Class<out Activity>? = null
    var activity: Activity? = null
    private val sw: SwipeManager by lazy { SwipeManager() }

    init {
        // it will catch events, otherwise fling over empty space wont work
        setOnTouchListener { _, _ -> return@setOnTouchListener true }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (sw.mDetector.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }


    inner class SwipeManager: GestureDetector.SimpleOnGestureListener() {

        val mDetector = GestureDetectorCompat(activity, this)

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null || e2 == null || Math.abs(e1.x - e2.x) < SWIPE_DISTANCE_MIN) { return false }

            val clazz: Class<*>? = if (velocityX > 0) left else right
            activity!!.startActivity(Intent(activity!!.applicationContext, clazz))
            activity!!.finish()
            return true
        }
    }
}
