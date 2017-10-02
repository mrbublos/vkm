package vkm.vkm

import android.app.Activity
import android.content.Context
import android.support.annotation.IdRes
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

fun <T : View> Activity.bind(@IdRes idRes: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return unsafeLazy {
        Log.d(MainActivity.TAG, "Lazy Binding $idRes")
        findViewById(idRes) as T
    }
}

fun <T : View?> View.bind(@IdRes idRes: Int): T {
    @Suppress("UNCHECKED_CAST")
    return findViewById(idRes) as T
}

fun ByteArray?.toHexString(): String {
    if (this == null || isEmpty()) { return "" }
    val ret = StringBuilder()
    forEach { ret.append(String.format("%02x", it)) }
    return ret.toString()
}

fun ByteArray?.md5(): String {
    if (this == null) { return ""}
    return MessageDigest.getInstance("MD5").digest(this).toHexString()
}


fun InputStream.readAll(charset: Charset = StandardCharsets.UTF_8): String {
    return use { it.bufferedReader(charset).use { it.readText() } }
}

fun String?.toJson(): JsonObject {
    if (this == null) { return JsonObject() }
    return Parser().parse(StringBuilder(this)) as JsonObject
}

fun String.md5(charset: Charset = StandardCharsets.UTF_8): String {
    return toByteArray(charset).md5()
}

fun String?.beginning(length: Int): String {
    if (this == null) { return "" }
    return filterIndexed({ index, _ -> index < length })
}

fun String?.base64(charset: Charset = StandardCharsets.UTF_8): String {
    if (this == null) { return "" }
    return String(Base64.encode(this.toByteArray(charset), Base64.DEFAULT), StandardCharsets.UTF_8)
}

fun String?.log() {
    this?.takeIf { isNotEmpty() }?.let {
        Log.v("vkmLog", this)
    }
}

fun String?.logE(e: Throwable? = null) {
    this?.takeIf { isNotEmpty() }?.let {
        Log.e("vkmLog", this, e)
    }
}

fun String.toComposition(): Composition {
    val composition = Composition()
    val properties = split("|VKM|")
    val map = mutableMapOf<String, String>()
    properties.forEach { serializedProperty ->
        val pair = serializedProperty.split("=VKM=")
        if (pair.size > 1) {
            map[pair[0]] = pair[1]
        }
    }
    composition::class.memberProperties.forEach {
        val kMutableProperty = it as KMutableProperty<*>
        map[it.name]?.let { propertyValue ->
            when (kMutableProperty.returnType.javaType) {
                Int::class.javaPrimitiveType,
                Int::class.javaObjectType -> kMutableProperty.setter.call(composition, propertyValue.toInt())
                Long::class.javaPrimitiveType,
                Long::class.javaObjectType -> kMutableProperty.setter.call(composition, propertyValue.toLong())
                String::class.java -> kMutableProperty.setter.call(composition, propertyValue)
            }
       }
    }

    return composition
}

fun String?.toast(context: Context?, length: Int = Toast.LENGTH_SHORT): String? {
    if (this == null) { return this }
    context?.let {
        Toast.makeText(context, this, length).show()
    }
    return this
}

fun String?.normalize() : String? {
    if (this == null) { return null }
    return this.trim().toLowerCase().replace(" ", "")
}

fun Composition.serialize(): String {
    return Composition::class.memberProperties.joinToString(separator = "|VKM|") { "${it.name}=VKM=${it.get(this)}" }
}

fun Composition.matches(string: String): Boolean {
    return name.contains(string) || artist.contains(string)
}

fun Composition.fileName(): String {
    val artistNormalized = artist.trim().beginning(32).replace(' ', '_').replace('/', '_')
    val nameNormalized = name.trim().beginning(32).replace(' ', '_').replace('/', '_')
    return "$artistNormalized-$nameNormalized.mp3"
}

fun Composition.localFile(): File? {
    val file = DownloadManager.getDownloadDir().resolve(fileName())
    return if (file.exists()) file else null
}

fun Composition?.equalsTo(other: Composition?): Boolean {
    return this?.name.normalize() == other?.name.normalize() && this?.artist.normalize() == other?.artist.normalize()
}

suspend fun Request.execute(): Triple<Request, Response, Result<String, FuelError>> {
    return this.responseString()
}

private fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)