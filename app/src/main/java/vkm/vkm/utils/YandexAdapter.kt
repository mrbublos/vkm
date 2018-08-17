package vkm.vkm.utils

import com.beust.klaxon.*
import com.github.kittinunf.fuel.httpGet
import vkm.vkm.ProxyActivity
import vkm.vkm.SearchFragment
import vkm.vkm.State
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.experimental.suspendCoroutine

class YMusicParsers(private val fragment: SearchFragment) {

    val parseCompositionList = { result: JsonObject? ->
        val compositionsFound = mutableListOf<Composition>()
        if (result != null && result.string("text") != null) {
            try {
                result.obj("tracks")?.let { tracks ->
                    tracks.array<JsonObject>("items")?.takeIf { it.isNotEmpty() }?.let {
                        val compositions = it.map { track ->
                            Composition(id = track.long("id")?.toString() ?: "",
                                    artist = track.array<JsonObject>("artists")?.joinToString(",") { artist ->
                                        artist.string("name") ?: ""
                                    }!!,
                                    name = track.string("title") ?: "",
                                    url = track.string("storageDir") ?: "",
                                    length = track.long("durationMs")?.toString() ?: "")
                        }
                        compositionsFound.addAll(compositions)
                    }
                    State.totalCompositions = tracks.int("total") ?: 0
                }
            } catch (e: Exception) {
                "Error parsing YM response".logE(e)
            }
        }
        fragment.setCompositionsList(compositionsFound, false)
    }
}

object YMusicApi {
    private val proxy = "89.179.33.21:80"

    suspend fun search(text: String, offset: Int): JsonObject {
        val urlEncText = URLEncoder.encode(text.replace(" ", "%20"), StandardCharsets.UTF_8.name())
        val url = "https://music.yandex.ru/handlers/music-search.jsx?text=$urlEncText&type=tracks&page=${offset / 100}"
        return callHttp(url, true)
    }

    suspend fun preprocessUrl(composition: Composition) {
        if (composition.url.startsWith("http")) { return }

        val url = "https://storage.mds.yandex.net/download-info/${composition.url}/2?format=json"
        val result = callHttp(url, false)
        if (result.string("path") == null) { return }

        val path = result.string("path")!!.substring(1)
        val s = result.string("s")!!
        val ts = result.string("ts")!!
        val server = result.string("host")!!
        val trackId = composition.url.split(".").last()
        val md5 = "XGRlBW9FXlekgbPrRHuSiA$path$s".md5()
        composition.url = "https://$server/get-mp3/$md5/$ts/$path?track-id=$trackId&play=false"
        "Resolved to link: ${composition.url}".log()
    }

    private suspend fun callHttp(url: String, withProxy: Boolean): JsonObject {
        ProxyActivity.setProxy(if (withProxy) proxy else "")

        "YM Calling: $url".log()
        val caller = url.httpGet()
        return suspendCoroutine { continuation ->
            caller.responseString { _, response, result ->
                "YM received result $result".log()
                try {
                    if (response.statusCode == 200) {
                        continuation.resume(result.component1().toJson())
                        return@responseString
                    } else {
                        result.component2().toString().logE()
                    }
                } catch (e: Exception) {
                    "Error connecting YM".logE(e)
                }
                continuation.resume(JsonObject())
            }
        }
    }
}
