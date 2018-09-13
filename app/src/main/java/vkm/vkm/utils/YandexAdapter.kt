package vkm.vkm.utils

import org.json.JSONArray
import org.json.JSONObject
import vkm.vkm.utils.HttpMethod.GET
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object YMusicParsers {

    fun parseCompositionList(result: JSONObject?): MutableList<Composition> {
        val compositionsFound = mutableListOf<Composition>()
        if (result != null && result.gets("text").isNotEmpty()) {
            try {
                result.geto("tracks").let { tracks ->
                    tracks.geta("items").takeIf { it.length() > 0 }?.let {
                        val compositions = it.map { track ->
                            Composition(id = track.getl("id").toString(),
                                    artist = track.geta("artists").map { artist ->
                                        artist.gets("name")
                                    }.joinToString(","),
                                    name = track.gets("title"),
                                    url = track.gets("storageDir"),
                                    length = track.getl("durationMs").toString())
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

    fun parseAlbum(result: JSONObject): MutableList<Composition> {
        val artist = result.geta("artists").map { it.gets("name") }.joinToString(",")
        return result.geta("volumes").mapArr { volume ->
            volume.map { composition ->
                Composition(id = composition.gets("id"),
                        name = composition.gets("title"),
                        artist = artist,
                        length = (composition.getl("durationMs") / 1000).toString(),
                        vkmId = System.nanoTime(),
                        url = composition.gets("storageDir"))
            }
        }.flatten() as MutableList<Composition>
    }

    fun parseChart(result: JSONObject): MutableList<Composition> {
        return result.geto("chart").geta("tracks").map { composition ->
            val artist = composition.geta("artists").map { it.gets("name") }.joinToString(",")
            Composition(id = composition.gets("id"),
                    name = composition.gets("title"),
                    artist = artist,
                    length = (composition.getl("durationMs") / 1000).toString(),
                    vkmId = System.nanoTime(),
                    url = composition.gets("storageDir"))
        }
    }
}

object YMusicApi {

    suspend fun search(text: String, offset: Int): JSONObject {
        val urlEncText = URLEncoder.encode(text.replace(" ", "%20"), StandardCharsets.UTF_8.name())
        val url = "https://music.yandex.ru/handlers/music-search.jsx?text=$urlEncText&type=tracks&page=${offset / 100}"
        return HttpUtils.call4Json(GET, url, true).safeObj()
    }

    suspend fun getNewReleases(): JSONObject {
        val url = "https://music.yandex.ru/handlers/main.jsx?what=new-releases"
        return HttpUtils.call4Json(GET, url, true).safeObj()
    }

    suspend fun getAlbums(ids: List<String>): JSONArray {
        val url = "https://music.yandex.ru/handlers/albums.jsx?albumIds=${ids.joinToString(",")}"
        return HttpUtils.call4Json(GET, url, true).safeArr()
    }

    suspend fun getAlbum(id: String): JSONObject {
        val url = "https://music.yandex.ru/handlers/album.jsx?album=$id"
        return HttpUtils.call4Json(GET, url, true).safeObj()
    }

    suspend fun getChart(): JSONObject {
        val url = "https://music.yandex.ru/handlers/main.jsx?what=chart"
        return HttpUtils.call4Json(GET, url, true).safeObj()
    }

    suspend fun preprocessUrl(composition: Composition) {
        if (composition.url.startsWith("http")) { return }

        val url = "https://storage.mds.yandex.net/download-info/${composition.url}/2?format=json"
        val result = HttpUtils.call4Json(GET, url, false).safeObj()
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
