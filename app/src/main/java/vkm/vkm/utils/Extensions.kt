package vkm.vkm.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import com.github.kittinunf.fuel.android.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import vkm.vkm.DownloadManager
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

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


fun String.md5(charset: Charset = StandardCharsets.UTF_8): String {
    return toByteArray(charset).md5()
}

fun String?.beginning(length: Int): String {
    if (this == null) { return "" }
    return filterIndexed { index, _ -> index < length }
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
    val me = this
    context?.let {
        GlobalScope.launch(Dispatchers.Main) { Toast.makeText(context, me, length).show() }
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

suspend fun MediaPlayer.loadAsync() {
    suspendCoroutine<Unit> { continuation ->
        this.setOnPreparedListener {
            continuation.resume(Unit)
        }
        this.prepareAsync()
    }
}

fun JSONObject.gets(name: String): String {
    return try {
        this.get(name).toString()
    } catch (e: Exception) {
        ""
    }
}

fun JSONObject.geta(name: String): JSONArray {
    return try {
        this.getJSONArray(name)
    } catch (e: Exception) {
        JSONArray("[]")
    }
}

fun JSONObject.geto(name: String): JSONObject {
    return try {
        this.getJSONObject(name)
    } catch (e: Exception) {
        JSONObject("{}")
    }
}

fun JSONObject.getl(name: String): Long {
    return try {
        this.getLong(name)
    } catch (e: Exception) {
        0L
    }
}

fun <R> JSONArray.map(action: (obj: JSONObject) -> R): MutableList<R>  {
    return (0 until this.length()).map {
        action(this.get(it) as JSONObject)
    }.toMutableList()
}

fun <R> JSONArray.mapArr(action: (obj: JSONArray) -> R): MutableList<R>  {
    return (0 until this.length()).map {
        action(this.get(it) as JSONArray)
    }.toMutableList()
}

fun Json?.safeObj(): JSONObject {
    return try {
        this?.obj() ?: JSONObject()
    } catch (e: Exception) {
        JSONObject()
    }
}

fun Json?.safeArr(): JSONArray {
    return try {
        this?.array() ?: JSONArray()
    } catch (e: Exception) {
        JSONArray()
    }
}