package vkm.vkm

import android.app.Activity
import android.support.annotation.IdRes
import android.util.Log
import android.view.View
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser

fun <T : View> Activity.bind(@IdRes idRes: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return unsafeLazy {
        Log.d(MainActivity.TAG, "Lazy Binding $idRes")
        findViewById(idRes) as T
    }
}

fun <T : View> Activity.bindNonLazy(@IdRes idRes: Int): T {
    @Suppress("UNCHECKED_CAST")
    return findViewById(idRes) as T
}

fun <T : View> View.bind(@IdRes idRes: Int): T {
    @Suppress("UNCHECKED_CAST")
    return findViewById(idRes) as T
}

fun String?.toJson(): JsonObject {
    if (this == null) { return JsonObject() }
    return Parser().parse(StringBuilder(this)) as JsonObject
}

private fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)