package vkm.vkm

import android.app.Activity
import android.support.annotation.IdRes
import android.util.Log
import android.view.View
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun <T : View> Activity.bind(@IdRes idRes: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return unsafeLazy {
        Log.d(MainActivity.TAG, "Lazy Binding $idRes")
        findViewById(idRes) as T
    }
}

fun <T : View> View.bind(@IdRes idRes: Int): T {
    @Suppress("UNCHECKED_CAST")
    return findViewById(idRes) as T
}

fun String?.toJson(): JsonObject {
    if (this == null) { return JsonObject() }
    return Parser().parse(StringBuilder(this)) as JsonObject
}

fun ByteArray?.toHexString(): String {
    if (this == null || this.isEmpty()) { return "" }
    val ret = StringBuilder()
    this.forEach { ret.append(String.format("%02x", it)) }
    return ret.toString()
}

fun InputStream.readAll(charset: Charset = StandardCharsets.UTF_8): String {
    return this.bufferedReader(charset).use { it.readText() }
}

private fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)