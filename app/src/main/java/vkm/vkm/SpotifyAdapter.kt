package vkm.vkm

import android.util.Log
import com.beust.klaxon.*
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost

class SpotifyParsers(private val activity: SearchActivity) {
    val parseUserList = { result: JsonObject? -> }

    val parsePlaylist = { result: JsonObject? -> }

    val parseCompositionList = { result: JsonObject? ->
        if (result == null) {
            activity.setCompositionsList(listOf())
        } else {
            val items = (result["tracks"] as JsonObject)["items"] as JsonArray<*>
            val compositions = items.map {
                val composition = it as JsonObject
                val artists = (composition["artists"] as JsonArray<*>).joinToString { (it as JsonObject).string("name") ?: "" }
                val compositionObject = Composition(id = composition.string("id") ?: "",
                        name = composition.string("name") ?: "",
                        artist = artists,
                        length = "${(composition.long("duration_ms") ?: 0) / 1000}",
                        url = composition.string("uri") ?: "")
                compositionObject
            }
            activity.setCompositionsList(compositions)
            StateManager.totalCompositions = (result["tracks"] as JsonObject).int("total") ?: 0
        }
    }
}

object SpotifyApi {
    private val apiUrl = "https://api.spotify.com"

    fun performLogin(): String {
        val _user = SecurityService.user
        var resultString = "Error logging in"

        _user?.let {
            val url = "https://accounts.spotify.com/api/token"
            val params = listOf("grant_type" to "client_credentials")
            val headers = mapOf("Authorization" to "Basic ${"${SecurityService.spotifyAppId}:${SecurityService.spotifyAppSecret}".base64()}")
            val request = url.httpPost(params)
            request.headers.putAll(headers)
            val result = request.responseString()
            val resp = result.component2()
            val res = result.component3()

            if (resp.statusCode == 200) {
                SecurityService.spotifyAccessToken = res.component1()?.toJson()?.string("access_token")
                resultString = "ok"
            } else {
                Log.e("vkm", res.component2().toString())
            }
        }

        return resultString
    }

    fun callApi(method: String, parameters: List<Pair<String, String>> = listOf(), callback: (data: JsonObject?) -> Unit) {
        val headers = mapOf("Authorization" to "Bearer ${SecurityService.spotifyAccessToken}")
        val url = apiUrl + method
        val request = url.httpGet(parameters)
        request.headers.putAll(headers)
        val result = request.responseString()
        val resp = result.component2()
        val res = result.component3()

        if (resp.statusCode == 200) {
            callback.invoke((result.component1() as String).toJson())
        } else {
            Log.e("vkm", res.component2().toString())
        }
    }

}
