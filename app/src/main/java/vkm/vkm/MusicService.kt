package vkm.vkm

import android.util.Log

import com.github.kittinunf.fuel.httpGet
class MusicService {

    var token: String

    init {
        Log.i("", "MusicService Started")
        token = authorize()
    }

    val apiUrl = "https://api.vk.com"
    val appId = "3682744"

    fun getUserDetails(userId: String) {
        val url = "$apiUrl/method/users.get"
        val params = listOf<Pair<String, Any>>(Pair("user_id", userId))
        url.httpGet(params).responseString { _, resp, result ->
            println("test")
        }
    }

    fun getUserPlaylist(name: String): List<Composition>? {
        val url = "$apiUrl/method/users.search"
        val params = listOf<Pair<String, Any>>(Pair("q", name))
        url.httpGet(params).responseString { _, resp, result ->
            println("test")
        }

        return null
    }

    fun getGroupPlaylist(groupId: String): List<Composition>? {
        return null
    }

    fun authorize(): String {
        val url = "https://oauth.vk.com/token"
        val params = listOf<Pair<String, Any>>(Pair("grant_type", "password"),
                Pair("scope", "nohttps"), Pair("client_id", appId), Pair("client_secret", ""), Pair("username", ""), Pair("password", ""))

        url.httpGet(params).responseString { _, resp, result ->
            println("test")
        }

        return ""
    }
}

data class Composition(var name: String, var url: String, var artist: String)