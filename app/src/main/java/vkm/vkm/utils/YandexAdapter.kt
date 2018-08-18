package vkm.vkm.utils

import com.beust.klaxon.*
import vkm.vkm.SearchFragment
import vkm.vkm.State
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    suspend fun search(text: String, offset: Int): JsonObject {
        val urlEncText = URLEncoder.encode(text.replace(" ", "%20"), StandardCharsets.UTF_8.name())
        val url = "https://music.yandex.ru/handlers/music-search.jsx?text=$urlEncText&type=tracks&page=${offset / 100}"
        return HttpUtils.call4Json(url, true)
    }

    suspend fun preprocessUrl(composition: Composition) {
        if (composition.url.startsWith("http")) { return }

        val url = "https://storage.mds.yandex.net/download-info/${composition.url}/2?format=json"
        val result = HttpUtils.call4Json(url, false)
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
}
