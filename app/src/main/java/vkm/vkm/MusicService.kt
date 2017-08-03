package vkm.vkm

import android.util.Log

import com.github.kittinunf.fuel.httpGet

/**
 * todo decompile off app and get client_id and key
 */
open class MusicService {

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

    open fun getUserPlaylist(name: String): List<Composition>? {
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

    fun getMock(): MusicServiceMock {
        return MusicServiceMock()
    }
}

class MusicServiceMock : MusicService() {
    override fun getUserPlaylist(name: String): List<Composition>? {
        return listOf(Composition("name", "url", "artist"),
                Composition("name1", "url", "artist1"),
                Composition("name2", "url", "artist2"),
                Composition("name3", "url", "artist3"),
                Composition("name4", "url", "artist4"),
                Composition("name5", "url", "artist5"),
                Composition("name6", "url", "artist6"),
                Composition("name7", "url", "artist7"),
                Composition("name8", "url", "artist8"),
                Composition("name9", "url", "artist9"))
    }
}
