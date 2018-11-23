package vkm.vkm.utils

import org.json.JSONArray
import org.json.JSONObject
import vkm.vkm.utils.HttpMethod.GET
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

object YMusicParsers {

    fun parseCompositionList(result: JSONObject): MutableList<Composition> {
        return parseTracks(result.geto("tracks").geta("items"))
    }

    fun parseNewReleases(result: JSONObject): List<String> {
        return result.geta("newReleases").join(",").split(",")
    }

    fun parseAlbums(result: JSONArray): MutableList<Album> {
        return result.map { album ->
            Album(id = album.gets("id"),
                    artist = getItemArtist(album),
                    name = album.gets("title"),
                    url = "http://${album.gets("coverUri").substringBefore("%%")}50x50")
        }
    }

    fun parseAlbum(result: JSONObject): MutableList<Composition> {
        return result.geta("volumes").mapArr { parseTracks(it) }.flatten() as MutableList<Composition>
    }

    fun parseChart(result: JSONObject): MutableList<Composition> {
        return parseTracks(result.geto("chart").geta("tracks"))
    }

    fun parseArtists(result: JSONObject): MutableList<Artist> {
        return result.geto("artists").geta("items").map { artist ->
            val cover = artist.geto("cover").gets("uri").substringBefore("%%")
            Artist(id = artist.gets("id"),
                   name = artist.gets("name"),
                   url = if (cover.isEmpty()) "" else "http://${cover}50x50")
        }
    }

    fun parseArtistTracks(result: JSONObject): MutableList<Composition> {
        return parseTracks(result.geta("tracks"))
    }

    private fun parseTracks(tracks: JSONArray): MutableList<Composition> {
        return tracks.map { track ->
            Composition(id = track.gets("id"),
                        name = track.gets("title"),
                        artist = getItemArtist(track),
                        length = (track.getl("durationMs") / 1000).toString(),
                        vkmId = System.nanoTime(),
                        url = track.gets("storageDir"))
        }
    }

    private fun getItemArtist(item: JSONObject) = item.geta("artists").map { it.gets("name") }.joinToString(",")
}

object YMusicApi {

    // type: (artist|album|track|all)
    suspend fun search(text: String, offset: Int, type: String): JSONObject {
        val pageSize = when (type) {
            "artist" -> 48
            "track" -> 100
            "album" -> 48
            else -> 48
        }

        val page = offset / pageSize
        if (offset > 0 && page == 0) { return JSONObject() } // only one page available
        val urlEncText = URLEncoder.encode(text.replace(" ", "%20"), StandardCharsets.UTF_8.name())
        val url = "https://music.yandex.ru/handlers/music-search.jsx?text=$urlEncText&type=$type&page=$page"
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

    suspend fun getArtistTracks(id: String): JSONObject {
        val url = "https://music.yandex.ru/handlers/artist.jsx?artist=$id&what=tracks&sort=&dir=&lang=ru"
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
