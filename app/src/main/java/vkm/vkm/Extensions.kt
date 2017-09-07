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
import java.security.MessageDigest
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

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


fun ByteArray.md5(): String {
    return MessageDigest.getInstance("MD5").digest(this).toHexString()
}

fun String.md5(charset: Charset = StandardCharsets.UTF_8): String {
    return this.toByteArray(charset).md5()
}

fun String?.beginning(length: Int): String {
    if (this == null) { return "" }
    return this.filterIndexed({ index, _ -> index < length })
}

fun InputStream.readAll(charset: Charset = StandardCharsets.UTF_8): String {
    return this.use { it.bufferedReader(charset).use { it.readText() } }
}

fun Composition.serialize(): String {
    return Composition::class.memberProperties.joinToString(separator = "||") { "${it.name}:${it.get(this)}" }
}

fun String.toComposition(): Composition {
    val composition = Composition()
    val properties = this.split("||")
    val map = mutableMapOf<String, String>()
    properties.forEach { serializedProperty ->
        val pair = serializedProperty.split(":")
        if (pair.size > 1) {
            map[pair[0]] = pair[1]
        }
    }
    composition::class.memberProperties.forEach {
        val kMutableProperty = it as KMutableProperty<*>
        when (kMutableProperty.returnType.javaType) {
            Int::class.javaPrimitiveType,
            Int::class.javaObjectType -> kMutableProperty.setter.call(composition, if (map[it.name] != null) map[it.name]!!.toInt() else 0)
            String::class.java -> kMutableProperty.setter.call(composition, map[it.name] ?: "")
        }
    }

    return composition
}

fun Composition.uid(): String {
    return "${this.ownerId}/${this.id}"
}

private fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)