package vkm.vkm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView

// TODO consider redo to a fragments, not activities
open class Swiper @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        var SWIPE_DISTANCE_MIN = 300
    }

    lateinit var activity: Activity
    open val swipeLeft: () -> Any? = {}
    open val swipeRight: () -> Any? = {}
    private val sw: SwipeManager by lazy { SwipeManager() }

    init {
        // it will catch events, otherwise fling over empty space wont work
        setOnTouchListener { _, _ -> return@setOnTouchListener true }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (sw.mDetector.onTouchEvent(ev)) { return true }
        return super.dispatchTouchEvent(ev)
    }


    inner class SwipeManager: GestureDetector.SimpleOnGestureListener() {

        val mDetector = GestureDetectorCompat(activity, this)

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null || e2 == null || Math.abs(e1.x - e2.x) < SWIPE_DISTANCE_MIN) { return false }
            "Registered swipe distance ${Math.abs(e1.x - e2.x)}".log()

            if (velocityX > 0) swipeLeft() else swipeRight()
            return true
        }
    }
}

class ScreenSwiper(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : Swiper(context, attrs, defStyleAttr) {
    lateinit var left: Class<out Activity>
    lateinit var right: Class<out Activity>

    override val swipeLeft = {
        activity.apply {
            startActivity(Intent(applicationContext, left))
            finish()
        }
    }

    override val swipeRight = {
        activity.apply {
            startActivity(Intent(applicationContext, right))
            finish()
        }
    }
}

class StringSwiper(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : Swiper(context, attrs, defStyleAttr) {
    var currentString = 0
    lateinit var list: List<String>
    lateinit var view: TextView
    lateinit var onSwiped: (index: Int, value: String) -> Any?

    override val swipeLeft = {
        currentString = currentString++ % list.size
        view.text = list[currentString]
        onSwiped(currentString, list[currentString])
    }

    override val swipeRight = {
        currentString = currentString-- % list.size
        view.text = list[currentString]
        onSwiped(currentString, list[currentString])
    }
}
