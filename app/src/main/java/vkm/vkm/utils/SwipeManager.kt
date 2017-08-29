package vkm.vkm.utils

import android.app.Activity
import android.content.Intent
import android.support.v4.view.GestureDetectorCompat
import android.view.GestureDetector
import android.view.MotionEvent
import vkm.vkm.HistoryActivity
import vkm.vkm.SearchActivity

class SwipeManager(private val activity: Activity): GestureDetector.SimpleOnGestureListener() {

    val mDetector = GestureDetectorCompat(activity, this)

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        var clazz: Class<*>? = null
        when (activity) {
            is SearchActivity -> clazz = HistoryActivity::class.java
            is HistoryActivity -> clazz = SearchActivity::class.java
        }

        activity.startActivity(Intent(activity.applicationContext, clazz))
        activity.finish()
        return true
    }
}