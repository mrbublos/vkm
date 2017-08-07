package vkm.vkm.utils

import android.app.Activity
import android.content.Intent
import android.view.MotionEvent

object SwipeManager {
    // for swipe
    var x1: Float = 0f
    var x2: Float = 0f
    var y1: Float = 0f
    var y2: Float = 0f
    val minDistance: Int = 150

    fun manageSwipe(event: MotionEvent?, activity: Activity, newActivityClass: Class<*>) {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                x1 = event.x
                y1 = event.y
            }
            MotionEvent.ACTION_UP -> {
                x2 = event.x
                y2 = event.y

                val distance = x1 - x2
                if (Math.abs(distance) > minDistance) {
                    activity.startActivity(Intent(activity.applicationContext, newActivityClass))
                    activity.finish()
                }
            }
        }
    }
}