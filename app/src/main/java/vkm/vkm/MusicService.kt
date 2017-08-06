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


    // TODO make return type non-nullable
    open fun getUserPlaylist(name: String): List<Composition> {
        val url = "$apiUrl/method/users.search"
        val params = listOf<Pair<String, Any>>(Pair("q", name))
        url.httpGet(params).responseString { _, resp, result ->
            println("test")
        }

        return listOf()
    }

    open fun getDownloaded(): List<Composition> {
        return listOf()
    }

    open fun getInProgress(): List<Composition> {
        return listOf()
    }

    fun getGroupPlaylist(groupId: String): List<Composition> {
        return listOf()
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
    override fun getUserPlaylist(name: String): List<Composition> {
        return getMockList("playlist ")
    }

    override fun getDownloaded(): List<Composition> {
        return getMockList("downloaded ")
    }

    override fun getInProgress(): List<Composition> {
        return getMockList("inProgress ")
    }

    fun getMockList(id: String = ""): List<Composition> {
        return listOf(Composition(id + "name", "url", "artist", 0, "", "1:00"),
                Composition(id + "name1", "url", "artist1", 0, "", "1:00"),
                Composition(id + "name2", "url", "artist2", 0, "", "1:00"),
                Composition(id + "name3", "url", "artist3", 0, "", "1:00"),
                Composition(id + "name4", "url", "artist4", 0, "", "1:00"),
                Composition(id + "name5", "url", "artist5", 0, "", "1:00"),
                Composition(id + "name6", "url", "artist6", 0, "", "1:00"),
                Composition(id + "name7", "url", "artist7", 0, "", "1:00"),
                Composition(id + "name8", "url", "artist8", 0, "", "1:00"),
                Composition(id + "name9", "url", "artist9", 0, "", "1:00"))
    }
}
