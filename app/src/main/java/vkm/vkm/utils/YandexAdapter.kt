package vkm.vkm.utils

import com.beust.klaxon.*
import org.json.JSONArray
import org.json.JSONObject
import vkm.vkm.utils.HttpMethod.GET
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object YMusicParsers {

    fun parseCompositionList(result: JsonObject?): MutableList<Composition> {
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
                }
            } catch (e: Exception) {
                "Error parsing YM response".logE(e)
            }
        }
        return compositionsFound
    }

    fun parseNewReleases(result: JSONObject): List<String> {
        return result.geta("newReleases").join(",").split(",")
    }

    fun parseAlbums(result: JSONArray): MutableList<Album> {
        return result.map { album ->
            val artist = album.geta("artists").map { it.gets("name") }.joinToString(",")
            Album(id = album.gets("id"),
                    artist = artist,
                    name = album.gets("title"),
                    url = "http://${album.gets("coverUri").substringBefore("%%")}50x50")
        }
    }

}

object YMusicApi {

    suspend fun search(text: String, offset: Int): JSONObject {
        val urlEncText = URLEncoder.encode(text.replace(" ", "%20"), StandardCharsets.UTF_8.name())
        val url = "https://music.yandex.ru/handlers/music-search.jsx?text=$urlEncText&type=tracks&page=${offset / 100}"
        return HttpUtils.call4Json(GET, url, true).obj()
    }

    suspend fun getNewReleases(): JSONObject {
        val url = "https://music.yandex.ru/handlers/main.jsx?what=new-releases"
        return HttpUtils.call4Json(GET, url, true).obj()
    }

    suspend fun getAlbums(ids: List<String>): JSONArray {
        val url = "https://music.yandex.ru/handlers/albums.jsx?albumIds=${ids.joinToString(",")}"
        return HttpUtils.call4Json(GET, url, false).array() // TODO check if need proxy
    }

    suspend fun preprocessUrl(composition: Composition) {
        if (composition.url.startsWith("http")) { return }

        val url = "https://storage.mds.yandex.net/download-info/${composition.url}/2?format=json"
        val result = HttpUtils.call4Json(GET, url, false).obj()
        if (result.gets("path").isBlank()) { return }

        val path = result.gets("path").substring(1)
        val s = result.gets("s")
        val ts = result.gets("ts")
        val server = result.gets("host")
        val trackId = composition.url.split(".").last()
        val md5 = "XGRlBW9FXlekgbPrRHuSiA$path$s".md5()
        composition.url = "https://$server/get-mp3/$md5/$ts/$path?track-id=$trackId&play=false"
        "Resolved to link: ${composition.url}".log()
    }
}
