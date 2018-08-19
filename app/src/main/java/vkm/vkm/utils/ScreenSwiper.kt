package vkm.vkm.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.constraint.ConstraintLayout
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.TextView
import kotlinx.android.synthetic.main.text_swiper_view.view.*
import vkm.vkm.R
import java.lang.ref.WeakReference

// TODO consider redo to a fragments, not activities
open class Swiper @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    companion object {
        var SWIPE_DISTANCE_MIN = 300
    }

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

        val mDetector = GestureDetectorCompat(context, this)

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null || e2 == null || Math.abs(e1.x - e2.x) < SWIPE_DISTANCE_MIN) { return false }
            "Registered swipe distance ${Math.abs(e1.x - e2.x)}".log()

            if (velocityX > 0) swipeLeft() else swipeRight()
            return true
        }
    }
}

class ScreenSwiper @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : Swiper(context, attrs, defStyleAttr) {
    lateinit var left: Class<out Activity>
    lateinit var right: Class<out Activity>
    lateinit var activity: WeakReference<Activity>

    override val swipeLeft = {
        activity.get()?.apply {
            startActivity(Intent(applicationContext, left))
            finish()
        }
    }

    override val swipeRight = {
        activity.get()?.apply {
            startActivity(Intent(applicationContext, right))
            finish()
        }
    }
}

class StringSwiper @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : Swiper(context, attrs, defStyleAttr) {
    lateinit var view: TextView
    var onSwiped: (index: Int, value: String, prev: Int) -> Unit = { _, _, _ -> }
    private var currentElementIndex = 0

    var value: MutableList<String> = mutableListOf()
    set(value) {
        field.clear()
        field.addAll(value)
        currentElementIndex = 0
        changeText()
        invalidate()
        requestLayout()
    }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.text_swiper_view, this)
        next.setOnClickListener { swipeRight() }
        previous.setOnClickListener { swipeLeft() }
        setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_DOWN) { parent.requestDisallowInterceptTouchEvent(true) }
            return@setOnTouchListener true
        }
    }

    fun setCurrentString(string: String) {
        currentElementIndex = value.indexOf(string)
        changeText()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        changeText()
    }

    override val swipeRight = {
        currentElementIndex = next()
        changeText()
        onSwiped(currentElementIndex, value[currentElementIndex], previous())
    }

    override val swipeLeft = {
        currentElementIndex = previous()
        changeText()
        onSwiped(currentElementIndex, value[currentElementIndex], next())
    }

    private fun changeText() {
        if (value.isEmpty()) { return }

        current.text = value.getOrNull(currentElementIndex) ?: ""
        next.text = value.getOrNull(next()) ?: ""
        previous.text = value.getOrNull(previous()) ?: ""
    }

    private fun next(): Int = (currentElementIndex + 1) % value.size
    private fun previous(): Int = (currentElementIndex - 1 + value.size) % value.size
}
